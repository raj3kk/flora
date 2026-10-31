// Flora user-data-delete Edge Function — P3-04 server half (CEO batch 2026-10-31).
// REPO CODE ONLY — DEPLOY NAHI HUA HAI. Deploy parent-browser step se hoga
// (Supabase dashboard -> Edge Functions -> New function -> paste -> Deploy).
// POST { user_id, confirm: true } + Authorization: Bearer <user JWT>
//   -> { ok: true, deleted: { push_tokens, usage, settings, home_boxes, heartbeats }, retained: [...] }
// JWT gate maujooda functions jaisa: anon client se user nikalo, na mile to 401.
// Body user_id JWT user se match nahi kare to 403 (koi doosre user ka data nahi).
// confirm !== true ho to 400 confirm_required — kuch delete NAHI hota.
// Delete hone wali per-user operational rows (user_id column code/SQL se verify):
//   push_tokens, usage, settings, home_boxes, heartbeats.
// RETAINED (audit/financial record): licences, payment rows, admin_audit.
// Anonymization sirf tab jab legally safe ho — silently anonymize nahi kiya gaya.
// FLAG (CEO resolve karega, silently reinterpret nahi kiya gaya): decisions doc ki
// P3-04 list me provider_keys bhi hai, par provider_keys table GLOBAL hai
// (primary key provider, koi user_id column nahi — admin_panel_tables.sql).
// Ek user ke delete par poori provider_keys table delete karna saare users ka
// panel-key route tod dega, isliye yahan provider_keys delete NAHI hoti hai.
// CEO faisla chahiye: list se provider_keys hatao, ya alag admin-only global
// purge ka rasta banao. Tab tak provider_keys retained me darj hai.
// Audit note: delete ke baad admin_audit me ek row (action user_data_delete_self,
// target = user, after_json me sirf table names + retained note — koi personal
// data/value audit-json me nahi). Audit insert fail ho to bhi delete wapas nahi
// hota (delete irreversible hai); response me audit_noted: false bataya jayega.
// Users row khud delete NAHI hoti hai (licence/payment history usi row se judi
// hai; row delete karna audit trail tod dega) — ye bhi CEO note ke taur par darj hai.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const body = await req.json().catch(() => ({}));
  if (!body.user_id || body.user_id !== userData.user.id) {
    return json({ error: "user_id does not match token" }, 403);
  }
  if (body.confirm !== true) {
    return json({ error: "confirm_required", message: "Delete ke liye confirm: true bhejo." }, 400);
  }
  const userId = userData.user.id;
  const deleted: Record<string, number> = {};
  const tables = ["push_tokens", "usage", "settings", "home_boxes", "heartbeats"];
  for (const table of tables) {
    // delete() + select count nahi deta har driver par; pehle count, phir delete.
    const { count } = await svc.from(table).select("*", { count: "exact", head: true }).eq("user_id", userId);
    const { error } = await svc.from(table).delete().eq("user_id", userId);
    if (error) { console.error(error); return json({ error: "delete failed", table }, 500); }
    deleted[table] = count || 0;
  }
  let auditNoted = false;
  try {
    const { error: auditErr } = await svc.from("admin_audit").insert({
      admin_user_id: userId, action: "user_data_delete_self",
      target_user_id: userId,
      after_json: { deleted_tables: tables, retained: ["licences", "payments", "admin_audit", "provider_keys (global, CEO decision pending)"] },
    });
    auditNoted = !auditErr;
    if (auditErr) console.error(auditErr);
  } catch (_e) { /* audit fail se delete wapas nahi hota; flag response me hai */ }
  return json({ ok: true, deleted, retained: ["licences", "payments", "admin_audit"], audit_noted: auditNoted });
});
