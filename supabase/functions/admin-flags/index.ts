// Flora admin-flags Edge Function — PHASE 2 (feature flags manage).
// POST { action, key?, enabled?, scope?, user_id? }
//   + Authorization: Bearer <user JWT> (Supabase Auth se).
// Admin check admin-approve pattern par: JWT se user nikalo
// (na mile -> 401); service client se users row check:
// is_admin=true na ho -> 403. Koi bhi caller flag nahi badal sakta.
// Actions:
//   list: saari flag rows wapas (key, enabled, scope).
//   set: key allowlist me honi chahiye; scope 'global' ya user_id
//     (user_id di ho to scope wahi; warna scope field ya 'global').
//     scope user_id ho to users row maujood honi chahiye (warna 400).
//     enabled boolean hona chahiye. Upsert (key, scope).
//   delete: key + scope se row hatao.
// Flags secret nahi hain (global config) — isliye list me values
// dikhana theek hai. Deploy se pehle admin_panel_phase2.sql chalao.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FLAG_KEYS = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill",
];

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
  if (userErr || !userData?.user) {
    return json({ error: "unauthorized" }, 401);
  }
  const svc = createClient(url, serviceKey);
  const adminId = userData.user.id;
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) {
    return json({ error: "admin only" }, 403);
  }
  // S1-32 audit helper (admin-approve pattern; audit fail se
  // action nahi rukega).
  async function audit(actionName: string, targetUserId: string | null, beforeJson: unknown, afterJson: unknown) {
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: actionName,
        target_user_id: targetUserId, before_json: beforeJson, after_json: afterJson,
      });
    } catch (_e) { /* audit fail se action nahi rukega */ }
  }
  const { action, key, enabled, scope, user_id } = await req.json().catch(() => ({}));
  if (action === "list") {
    const { data, error } = await svc.from("feature_flags").select("key,enabled,scope").order("key");
    if (error) return json({ error: error.message }, 500);
    return json({ flags: data || [] });
  }
  if (action === "set") {
    if (!FLAG_KEYS.includes(key)) {
      return json({ error: "key allowlist me nahi hai: " + String(key) }, 400);
    }
    if (typeof enabled !== "boolean") {
      return json({ error: "enabled boolean hona chahiye" }, 400);
    }
    const effScope = (typeof user_id === "string" && user_id) ? user_id
      : (typeof scope === "string" && scope ? scope : "global");
    if (effScope !== "global") {
      const { data: uRow } = await svc.from("users").select("id").eq("id", effScope).maybeSingle();
      if (!uRow) return json({ error: "unknown user scope" }, 400);
    }
    const { data: beforeRow } = await svc.from("feature_flags").select("key,enabled,scope").eq("key", key).eq("scope", effScope).maybeSingle();
    const { data: upserted, error } = await svc.from("feature_flags").upsert(
      { key, enabled, scope: effScope, updated_at: new Date().toISOString() },
      { onConflict: "key,scope" },
    ).select("key");
    if (error) return json({ error: error.message }, 500);
    await audit("flag_set", effScope !== "global" ? effScope : null,
      beforeRow || null, { key, enabled, scope: effScope });
    // S1-38: additive affected count (upsert hamesha 1 row deta hai).
    return json({ ok: true, affected: (upserted || []).length });
  }
  if (action === "delete") {
    if (!FLAG_KEYS.includes(key)) {
      return json({ error: "key allowlist me nahi hai: " + String(key) }, 400);
    }
    const effScope = (typeof user_id === "string" && user_id) ? user_id
      : (typeof scope === "string" && scope ? scope : "global");
    const { data: beforeRow } = await svc.from("feature_flags").select("key,enabled,scope").eq("key", key).eq("scope", effScope).maybeSingle();
    const { data: deleted, error } = await svc.from("feature_flags").delete().eq("key", key).eq("scope", effScope).select("key");
    if (error) return json({ error: error.message }, 500);
    await audit("flag_deleted", effScope !== "global" ? effScope : null,
      beforeRow || null, { key, scope: effScope });
    // S1-38: galat scope/key par silent no-op ki jagah affected:0.
    return json({ ok: true, affected: (deleted || []).length });
  }
  return json({ error: "action list/set/delete required" }, 400);
});
