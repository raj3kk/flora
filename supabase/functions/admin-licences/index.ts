// Flora admin-licences Edge Function — LFMS (licence keys manage).
// POST { action, ... } + Authorization: Bearer <user JWT> (admin only).
// Admin check admin-approve pattern par: JWT se user nikalo
// (na mile -> 401); service client se users row check:
// is_admin=true na ho -> 403. Koi bhi caller licence nahi chhu sakta.
//
// KEY STORAGE (F8 resolved: PLAINTEXT-with-controls — user order
// "jab mann tab copy" hash+last4 se possible nahi hota, kyunki hash
// se full key dobara nahi milti). Isliye licence_key text column
// (unique index) me plaintext rehti hai. Protection: (1) RLS ON,
// koi policy nahi — sirf service-role (functions ke andar) chhu
// sakta hai; (2) list me kabhi FULL key nahi aati — sirf masked
// + last 4; (3) full key sirf create response me EK BAAR + reveal
// par (audit ke saath) milti hai; (4) key kabhi log/audit-json me
// nahi aati. Caveat (imaandaar): DB leak hone par key dikhegi —
// yehi caveat panel DB keys par bhi hai; service-role-only +
// masked UI hi protection hai.
//
// Actions:
//   create { user_id, plan, duration_days } -> unique key generate
//     (collision par retry) + user ki pehli active licences ko
//     'superseded' karo (superseded_by = nayi row, reason audit me)
//     + key CREATE RESPONSE me EK BAAR poori dikhao (uske baad
//     sirf masked). Ek submit = ek licence; ek user ki ek waqt
//     par sirf ek active licence.
//   list { search?, status? } -> masked keys + last 4 + bound
//     device short + status + expiry (full key KABHI nahi).
//   revoke { licence_id, reason } -> status revoked +
//     revoked_reason/by/at + admin_audit insert.
//   rebind { licence_id } -> bound_device_id NULL karo + audit.
//     (15-day wait ka admin bypass: agli verify turant nayi
//     device se bind hogi, wait nahi lagega. Sirf active row par.)
//   reveal { licence_id } -> full key (admin-only) + audit
//     ('licence_revealed', key audit-json me KABHI nahi).
// User apni key ke liye: licence-key function (apne JWT se) —
// ye function sirf admin ke liye hai.
// Deploy se pehle supabase/sql/licence_system.sql chalao.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
const PLANS = ["free", "pro"];

function genKeyPart() {
  const buf = new Uint32Array(4);
  crypto.getRandomValues(buf);
  let s = "";
  for (let i = 0; i < 4; i++) s += KEY_ALPHABET[buf[i] % KEY_ALPHABET.length];
  return s;
}
function genKey() {
  return "FLORA-" + genKeyPart() + "-" + genKeyPart() + "-" + genKeyPart();
}
function maskKey(k) {
  if (!k) return null;
  return "FLORA-••••-••••-" + String(k).slice(-4);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const adminId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);

  const body = await req.json().catch(() => ({}));
  const { action } = body;

  async function audit(actionName, targetUserId, afterJson) {
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: actionName,
        target_user_id: targetUserId, after_json: afterJson,
      });
    } catch (_e) { /* audit fail se action nahi rukega */ }
  }

  // Supersede helper: user ki active rows (exceptId chhod kar) ko
  // 'superseded' karo, superseded_by = newId. Returns count.
  async function supersedePriorActives(userId, newId) {
    const { data: priors } = await svc.from("licences")
      .select("id").eq("user_id", userId).eq("status", "active");
    let count = 0;
    for (const r of (priors || [])) {
      if (r.id === newId) continue;
      const { error } = await svc.from("licences")
        .update({ status: "superseded", superseded_by: newId }).eq("id", r.id);
      if (error) return { count, error: error.message };
      count++;
    }
    return { count, error: null };
  }

  if (action === "create") {
    const { user_id, plan, duration_days } = body;
    if (!user_id) return json({ error: "user_id required" }, 400);
    if (!PLANS.includes(plan)) return json({ error: "plan must be free/pro" }, 400);
    const days = Number(duration_days);
    if (!Number.isInteger(days) || days < 1 || days > 3650) {
      return json({ error: "duration_days must be an integer 1..3650" }, 400);
    }
    const { data: uRow } = await svc.from("users").select("id").eq("id", user_id).maybeSingle();
    if (!uRow) return json({ error: "unknown user" }, 400);
    const now = new Date();
    const expires = new Date(now.getTime() + days * 24 * 60 * 60 * 1000).toISOString();
    // Unique key generate (collision par retry, max 5).
    let newRow = null, lastErr = null;
    for (let attempt = 0; attempt < 5; attempt++) {
      const key = genKey();
      const { data, error } = await svc.from("licences").insert({
        user_id, plan_id: plan, status: "active", device_id: null,
        licence_key: key, activated_at: now.toISOString(), expires_at: expires,
      }).select("id").single();
      if (!error) { newRow = { ...data, licence_key: key }; break; }
      if (error.code === "23505") { lastErr = error; continue; }
      return json({ error: error.message }, 500);
    }
    if (!newRow) return json({ error: "key generation failed: " + (lastErr?.message || "collision") }, 500);
    // Nayi row pehle bani, tabhi purani supersede (order zaroori:
    // supersede pehle karte aur insert fail hota to user ke paas
    // kuch nahi bachta). Supersede fail ho to bhi nayi licence
    // maujood hai — 500 me licence_id de do taaki admin retry
    // ya manual revoke kar sake; payment jaisa pending nahi hai.
    const sup = await supersedePriorActives(user_id, newRow.id);
    if (sup.error) {
      await audit("licence_created_supersede_failed", user_id,
        { licence_id: newRow.id, plan, expires_at: expires, supersede_error: sup.error });
      return json({ ok: true, licence_id: newRow.id, licence_key: newRow.licence_key,
        masked_key: maskKey(newRow.licence_key), plan, expires_at: expires,
        superseded_count: sup.count, warning: "supersede failed: " + sup.error }, 200);
    }
    await audit("licence_created", user_id,
      { licence_id: newRow.id, plan, expires_at: expires, superseded_count: sup.count });
    return json({ ok: true, licence_id: newRow.id, licence_key: newRow.licence_key,
      masked_key: maskKey(newRow.licence_key), plan, expires_at: expires,
      superseded_count: sup.count });
  }

  if (action === "list") {
    const { search, status } = body;
    if (status !== undefined && status !== "active" && status !== "revoked" &&
        status !== "expired" && status !== "superseded") {
      return json({ error: "status must be active/revoked/expired/superseded" }, 400);
    }
    let q = svc.from("licences").select("*").order("activated_at", { ascending: false }).limit(200);
    if (status) q = q.eq("status", status);
    const { data: rows, error } = await q;
    if (error) return json({ error: error.message }, 500);
    // Emails best-effort (admin-payments pattern).
    const emails = {};
    try {
      for (let page = 1; page <= 10; page++) {
        const { data: lu, error: luErr } = await svc.auth.admin.listUsers({ page, perPage: 100 });
        if (luErr || !lu?.users?.length) break;
        for (const u of lu.users) { if (u?.id && u?.email) emails[u.id] = u.email; }
        if (lu.users.length < 100) break;
      }
    } catch (_e) { /* bina email ke bhi list do */ }
    let out = (rows || []).map((r) => ({
      id: r.id, user_id: r.user_id, user_email: emails[r.user_id] || null,
      plan_id: r.plan_id, status: r.status, expires_at: r.expires_at,
      activated_at: r.activated_at, device_id: r.device_id,
      device_short: r.device_id ? String(r.device_id).slice(0, 8) + "…" : null,
      has_key: !!r.licence_key, masked_key: maskKey(r.licence_key),
      key_last4: r.licence_key ? String(r.licence_key).slice(-4) : null,
      revoked_reason: r.revoked_reason || null, revoked_at: r.revoked_at || null,
      revoked_by: r.revoked_by || null, superseded_by: r.superseded_by || null,
    }));
    // Search client-filter (email / last4 / device short / user id).
    if (typeof search === "string" && search.trim()) {
      const s = search.trim().toLowerCase();
      out = out.filter((r) =>
        (r.user_email || "").toLowerCase().includes(s) ||
        (r.key_last4 || "").toLowerCase().includes(s) ||
        (r.device_id || "").toLowerCase().includes(s) ||
        (r.user_id || "").toLowerCase().includes(s));
    }
    return json({ ok: true, licences: out });
  }

  if (action === "revoke") {
    const { licence_id, reason } = body;
    if (!licence_id || !reason) return json({ error: "licence_id + reason required" }, 400);
    const { data: row } = await svc.from("licences").select("id,user_id,status").eq("id", licence_id).maybeSingle();
    if (!row) return json({ error: "licence not found" }, 404);
    if (row.status === "revoked") return json({ error: "already revoked" }, 400);
    const now = new Date().toISOString();
    const { error } = await svc.from("licences").update({
      status: "revoked", revoked_reason: reason, revoked_by: adminId, revoked_at: now,
    }).eq("id", licence_id);
    if (error) return json({ error: error.message }, 500);
    await audit("licence_revoked", row.user_id, { licence_id, reason });
    return json({ ok: true });
  }

  if (action === "rebind") {
    const { licence_id } = body;
    if (!licence_id) return json({ error: "licence_id required" }, 400);
    const { data: row } = await svc.from("licences").select("id,user_id,status").eq("id", licence_id).maybeSingle();
    if (!row) return json({ error: "licence not found" }, 404);
    if (row.status !== "active") return json({ error: "only active licence can be rebound" }, 400);
    const { error } = await svc.from("licences").update({ device_id: null }).eq("id", licence_id);
    if (error) return json({ error: error.message }, 500);
    await audit("licence_rebound", row.user_id, { licence_id });
    return json({ ok: true });
  }

  if (action === "reveal") {
    const { licence_id } = body;
    if (!licence_id) return json({ error: "licence_id required" }, 400);
    const { data: row } = await svc.from("licences").select("id,user_id,licence_key").eq("id", licence_id).maybeSingle();
    if (!row) return json({ error: "licence not found" }, 404);
    if (!row.licence_key) return json({ error: "keyless licence (payment row)" }, 400);
    await audit("licence_revealed", row.user_id, { licence_id });
    return json({ ok: true, licence_key: row.licence_key });
  }

  return json({ error: "action create/list/revoke/rebind/reveal required" }, 400);
});
