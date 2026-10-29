// Flora licence-key Edge Function — LFMS (USER apni key, apne JWT se).
// POST { action, licence_id? } + Authorization: Bearer <user JWT>.
// Admin check NAHI hai — ye user ka apna data hai. Caller-match
// hi suraksha hai: JWT user ki hi licences dikhengi/wapas hongi,
// kisi aur user ki kabhi nahi (not-found vs not-owned ka jawab ek
// jaisa 404 hai — oracle nahi banega).
//   list: JWT user ki licences (masked + last 4 + status + expiry
//     + bound short). Full key KABHI list me nahi aati.
//   reveal { licence_id }: sirf apni licence ki full key (Wave B
//     LicenceScreen: masked default + click par reveal + copy).
//     Reveal audit me darj hota hai (key audit-json me KABHI nahi).
//     Keyless row (payment row, bina key) par 400.
// F8 (plaintext-with-controls) ka user-side hissa: user jab mann
// tab apni key dekh/copy kar sakta hai — admin reveal
// (admin-licences) se alag raasta, wahi masked-default niyam.
// Deploy se pehle supabase/sql/licence_system.sql chalao.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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
  const jwtUserId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const body = await req.json().catch(() => ({}));
  const { action } = body;

  if (action === "list") {
    const { data: rows, error } = await svc.from("licences").select("*")
      .eq("user_id", jwtUserId).order("activated_at", { ascending: false }).limit(50);
    if (error) return json({ error: error.message }, 500);
    const out = (rows || []).map((r) => ({
      id: r.id, plan_id: r.plan_id, status: r.status,
      expires_at: r.expires_at, activated_at: r.activated_at,
      device_short: r.device_id ? String(r.device_id).slice(0, 8) + "…" : null,
      has_key: !!r.licence_key, masked_key: maskKey(r.licence_key),
      key_last4: r.licence_key ? String(r.licence_key).slice(-4) : null,
      revoked_reason: r.revoked_reason || null,
    }));
    return json({ ok: true, licences: out });
  }

  if (action === "reveal") {
    const { licence_id } = body;
    if (!licence_id) return json({ error: "licence_id required" }, 400);
    const { data: row } = await svc.from("licences").select("id,user_id,licence_key")
      .eq("id", licence_id).maybeSingle();
    // Na mile ya kisi aur ki ho — ek jaisa 404 (oracle nahi).
    if (!row || row.user_id !== jwtUserId) return json({ error: "licence not found" }, 404);
    if (!row.licence_key) return json({ error: "keyless licence (payment row)" }, 400);
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: jwtUserId, action: "licence_key_self_reveal",
        target_user_id: jwtUserId, after_json: { licence_id },
      });
    } catch (_e) { /* audit fail se reveal nahi rukega */ }
    return json({ ok: true, licence_key: row.licence_key });
  }

  return json({ error: "action list/reveal required" }, 400);
});
