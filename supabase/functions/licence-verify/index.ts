// Flora licence-verify Edge Function v2 — LFMS (server-authoritative).
// POST { user_id, device_id?, licence_key? } + Authorization: Bearer <user JWT>
//   -> { plan, status, valid, features[], expires_at, bound_device_short, ... }.
// device_id OPTIONAL hai (v2.1, website account card ke liye):
//   - device_id NA mile to STATUS-ONLY view milta hai (plan/status/
//     expiry/features, sirf JWT user ka apna status) — BINA binding
//     decision, BINA claim, BINA transfer ke (koi write nahi).
//     Website par device_id hota hi nahi, isliye account.html ka
//     licence card isi par chalega.
//   - device_id mile to poora v2 logic (binding/claim/transfer)
//     waisa hi chalega (neeche).
// v1 se wahi (toda NAHI gaya):
//   - 1-phone / 15-day change rule (standing rule): pro licence ek
//     waqt par sirf EK device par. Naye device se check aaye aur
//     user ki pro licence kisi doosre device par active ho:
//       * activated_at se 15 din poore NA hue -> plan "free" +
//         status "free" + device_locked true +
//         device_change_available_at (purani device par licence
//         chalti rehti hai, kuch move nahi hota).
//       * 15 din poore -> TRANSFER: row naye device par move
//         (device_id + activated_at update), pro wapas. Purana
//         device phir free.
//   - NULL-claim (website-path ii): device_id NULL wali pro row
//     kisi phone par kabhi chali hi nahi, isliye is par 15-din ka
//     wait NAHI lagta — pehli verify par isi device par claim.
//   - Free plan par koi device lock nahi (free har device par free).
// v2 me naya:
//   - JWT gate (v1 me koi auth check NAHI tha — ye gap tha):
//     anon client se JWT ka user nikalo (na mile -> 401); body ka
//     user_id JWT user se match nahi kare -> 403. device_id body
//     se hi aata hai (app bhejta hai).
//   - licence_key ADDITIVE hai: keyless rows (payment-approved)
//     bina key ke waise hi chalti rahengi. licence_key mile to key
//     path chalta hai: key row dhoondho -> user match (na mile ya
//     user alag ho to ek jaisa {valid:false, status:'invalid_key'}
//     — key-existence oracle nahi banega) -> revoked/expired/
//     superseded shapes -> NULL claim / device match / 15-day rule
//     (pro row par wahi rule; free row par kabhi lock/transfer nahi).
//   - valid ka matlab (Wave B client ke liye contract): valid=false
//     SIRF tab jab kuch galat/blocked hai (revoked, expired,
//     superseded, device_locked, invalid_key). Free tier (koi pro
//     nahi) valid:true + plan 'free' hai — ye error nahi hai.
//     Pro sirf valid=true + status='active' + plan='pro' par.
//   - features[] plan_features table se (na mile / table na ho to
//     defaults: free=15 flags, pro=15 flags — fail-open, canonical
//     Free rule ke hisaab se (saare tools + 600s cap alag se).
//   - bound_device_short: bound device ka pehla hissa (poora
//     device_id kabhi response me nahi aata).
//   - licence_key kabhi kisi response me nahi aata (list/create/
//     reveal alag admin functions me hain, wahan bhi masked).
// Env (dashboard, never in repo): SUPABASE_URL,
// SUPABASE_SERVICE_ROLE_KEY, SUPABASE_ANON_KEY.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FIFTEEN_DAYS_MS = 15 * 24 * 60 * 60 * 1000;
// CANONICAL FREE RULE (Roadmap Phase 5, standing product rule):
// Free = saare tools + 600 talk sec/day cap (cap alag se lagta hai).
// Isliye fallback bhi poori 15-flag list hai — pehle free fallback
// sirf ["talk"] tha aur pro fallback me "builder" missing tha, jo
// DB seed (15 flags) aur website ("saare tools") se takrata tha.
// Live plan_features row milne par wahi list milti hai; fallback
// sirf row/table na mile par lagta hai.
const FREE_FEATURES = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill", "builder",
];
const PRO_FEATURES = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill", "builder",
];

// S2-1 (Cycle 122 FIX): CORS block — website (account.html) se
// licence-verify browser call par preflight OPTIONS 405 deta tha
// (live probe 2026-10-30: OPTIONS -> 405, koi CORS header nahi).
// OPTIONS par 200 + wahi headers har response par; JWT gate
// waisa hi rahega — CORS sirf preflight kholta hai, auth nahi
// hatata. Allow-Origin "*" maujooda convention hai (S3-4
// allowlist abhi HELD hai — host list parent se aane par hi
// badlega, yahan guess nahi kiya gaya).
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });

  // JWT gate (v2): caller ka Supabase Auth JWT chahiye.
  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const jwtUserId = userData.user.id;
  const supabase = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  const body = await req.json().catch(() => ({}));
  const { user_id, device_id, licence_key } = body;
  if (!user_id) return json({ error: "user_id required" }, 400);
  if (user_id !== jwtUserId) return json({ error: "user_id does not match token" }, 403);

  const now = new Date();
  const short = (d) => (d ? String(d).slice(0, 8) + "…" : null);
  const isExpired = (r) => r.expires_at && new Date(r.expires_at) < now;

  // features[]: plan_features se, na mile to defaults (fail-open).
  async function featuresFor(plan) {
    try {
      const { data } = await supabase.from("plan_features").select("features").eq("plan", plan).maybeSingle();
      if (Array.isArray(data?.features)) return data.features;
    } catch (_e) { /* defaults below */ }
    return plan === "pro" ? PRO_FEATURES : FREE_FEATURES;
  }

  // --- STATUS-ONLY view (device_id na mile to): sirf padhta hai,
  // koi claim/transfer/update NAHI karta. Website account card
  // isi par chalega (website par device_id hota hi nahi). ---
  async function statusOnly(row) {
    if (row.status === "revoked") {
      return { valid: false, plan: row.plan_id, status: "revoked", reason: row.revoked_reason || null };
    }
    if (row.status === "superseded") {
      return { valid: false, plan: row.plan_id, status: "superseded" };
    }
    if (row.status === "expired" || isExpired(row)) {
      return { valid: false, plan: "free", status: "expired" };
    }
    return { valid: true, plan: row.plan_id, status: row.plan_id === "pro" ? "active" : "free",
      features: await featuresFor(row.plan_id), expires_at: row.expires_at || null,
      bound_device_short: short(row.device_id) };
  }
  if (!device_id) {
    if (typeof licence_key === "string" && licence_key.trim()) {
      const key = licence_key.trim();
      const { data: keyRow, error: keyError } = await supabase.from("licences")
        .select("*").eq("licence_key", key).maybeSingle();
      if (keyError) { console.error(keyError); return json({ error: "store failed" }, 500); }
      if (!keyRow || keyRow.user_id !== user_id) {
        return json({ valid: false, status: "invalid_key" });
      }
      return json(await statusOnly(keyRow));
    }
    const { data: rows, error: rowsError } = await supabase.from("licences")
      .select("*").eq("user_id", user_id).order("activated_at", { ascending: false });
    if (rowsError) { console.error(rowsError); return json({ error: "store failed" }, 500); }
    const list = rows || [];
    const pro = list.find((r) => r.plan_id === "pro" && r.status === "active" && !isExpired(r));
    if (pro) return json(await statusOnly(pro));
    const act = list.find((r) => r.status === "active" && !isExpired(r));
    if (act) return json(await statusOnly(act));
    if (list.length) return json(await statusOnly(list[0]));
    return json({ valid: true, plan: "free", status: "free", features: await featuresFor("free") });
  }

  // Ek licence row ka device faisla (claim / match / 15-day rule).
  // Returns response object (valid + plan + status + extras).
  async function decideRow(row) {
    if (row.status === "revoked") {
      return { valid: false, plan: row.plan_id, status: "revoked", reason: row.revoked_reason || null };
    }
    if (row.status === "superseded") {
      return { valid: false, plan: row.plan_id, status: "superseded" };
    }
    if (row.status === "expired" || isExpired(row)) {
      return { valid: false, plan: "free", status: "expired" };
    }
    // Yahan row active + unexpired hai.
    if (row.plan_id !== "pro") {
      // Free row: kabhi lock/transfer nahi. NULL ho to claim kar lo
      // (tidy rehta hai), nahi to device ki parwah kiye bina free.
      if (row.device_id === null) {
        const { error: claimError } = await supabase.from("licences")
          .update({ device_id, activated_at: now.toISOString() }).eq("id", row.id);
        if (claimError) { console.error(claimError); return json({ error: "store failed" }, 500); }
        return { valid: true, plan: "free", status: "free", device_claimed: true, features: await featuresFor("free"), expires_at: row.expires_at || null, bound_device_short: short(device_id) };
      }
      return { valid: true, plan: "free", status: "free", features: await featuresFor("free"), expires_at: row.expires_at || null, bound_device_short: short(row.device_id) };
    }
    // Pro row.
    if (row.device_id === null) {
      const { error: claimError } = await supabase.from("licences")
        .update({ device_id, activated_at: now.toISOString() }).eq("id", row.id);
      if (claimError) { console.error(claimError); return json({ error: "store failed" }, 500); }
      return { valid: true, plan: "pro", status: "active", device_claimed: true, features: await featuresFor("pro"), expires_at: row.expires_at || null, bound_device_short: short(device_id) };
    }
    if (row.device_id === device_id) {
      return { valid: true, plan: "pro", status: "active", features: await featuresFor("pro"), expires_at: row.expires_at || null, bound_device_short: short(device_id) };
    }
    const activatedAt = new Date(row.activated_at);
    const availableAt = new Date(activatedAt.getTime() + FIFTEEN_DAYS_MS);
    if (now < availableAt) {
      return { valid: false, plan: "free", status: "free", device_locked: true, device_change_available_at: availableAt.toISOString(), features: await featuresFor("free"), bound_device_short: short(row.device_id) };
    }
    const { error: moveError } = await supabase.from("licences")
      .update({ device_id, activated_at: now.toISOString() }).eq("id", row.id);
    if (moveError) { console.error(moveError); return json({ error: "store failed" }, 500); }
    return { valid: true, plan: "pro", status: "active", device_changed: true, features: await featuresFor("pro"), expires_at: row.expires_at || null, bound_device_short: short(device_id) };
  }

  // --- Key path (licence_key mili ho to) ---
  if (typeof licence_key === "string" && licence_key.trim()) {
    const key = licence_key.trim();
    const { data: keyRow, error: keyError } = await supabase.from("licences")
      .select("*").eq("licence_key", key).maybeSingle();
    if (keyError) { console.error(keyError); return json({ error: "store failed" }, 500); }
    // Na mile ya kisi aur user ki ho — ek jaisa jawab (oracle nahi).
    if (!keyRow || keyRow.user_id !== user_id) {
      return json({ valid: false, status: "invalid_key" });
    }
    const res = await decideRow(keyRow);
    // decideRow 500 Response bhi de sakta hai (claim/move error).
    if (res instanceof Response) return res;
    return json(res);
  }

  // --- v1 path (user_id + device_id), status handling + features ke saath ---
  // S1-18 (Cycle 122 FIX): duplicate bound rows par maybeSingle 500
  // deta tha (har verify fail). Ab limit(2) + latest activated_at
  // wali row — naye duplicates licences_user_device_uidx (deploy
  // session me live) rokta hai, purane hon to service rukegi nahi.
  const { data: boundRows, error } = await supabase.from("licences")
    .select("*").eq("user_id", user_id).eq("device_id", device_id).order("activated_at", { ascending: false }).limit(2);
  if (error) { console.error(error); return json({ error: "store failed" }, 500); }
  const data = (boundRows || [])[0] || null;
  if (data) {
    const res = await decideRow(data);
    if (res instanceof Response) return res;
    return json(res);
  }

  // Is device par koi licence row nahi: kya user ki pro licence kisi
  // doosre device par active hai? (latest activated_at wali pehle.)
  // decideRow wahi claim/lock/transfer faisla deta hai.
  const { data: others, error: othersError } = await supabase.from("licences")
    .select("*").eq("user_id", user_id).order("activated_at", { ascending: false });
  if (othersError) { console.error(othersError); return json({ error: "store failed" }, 500); }
  const pro = (others || []).find((r) =>
    r.plan_id === "pro" && r.status === "active" &&
    (!r.expires_at || new Date(r.expires_at) >= now));
  if (!pro) {
    return json({ valid: true, plan: "free", status: "free", features: await featuresFor("free") });
  }
  const res = await decideRow(pro);
  if (res instanceof Response) return res;
  return json(res);
});
