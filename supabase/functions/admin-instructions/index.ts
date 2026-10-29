// Flora admin-instructions Edge Function — PHASE 2 (global brain config).
// POST { action, instructions?, knowledge? }
//   + Authorization: Bearer <user JWT> (Supabase Auth se).
// Admin check admin-approve pattern par: JWT se user nikalo
// (na mile -> 401); users.is_admin=true na ho -> 403.
// Actions:
//   get: global_instructions row (id=1) wapas — instructions +
//     knowledge + updated_at. Admin ko hi milta hai; instructions
//     secret nahi hain, isliye GET par text dena theek hai.
//   set: di gayi fields merge karke upsert (id=1). Har field
//     max 2000 chars (dono milakar 4000 — brain-proxy v4 ka
//     combined cap isi se sane rehta hai). Response me sirf
//     confirm + lengths (set ke baad poora text wapas nahi bheja
//     jata — panel GET se padh lega jab edit karna ho).
// Deploy se pehle admin_panel_phase2.sql chalao.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// S2-1 (Cycle 2 FIX, PARTIAL — sirf free functions): CORS preflight.
// Browser calls (panel/website, Authorization header wali) par
// OPTIONS ko 200 + allow headers do; wahi headers har response
// par bhi. Ye sirf browser preflight kholta hai — JWT/admin
// gates waise hi rahenge (CORS auth nahi hatata). LFMS functions
// (licence-verify, brain-proxy, tts-proxy, admin-licences,
// admin-plan-features, push-token-sync, usage-count, send-push)
// par CORS abhi NAHI laga hai, isliye poora S2-1 item QUEUED
// barkaraar hai — ye partial fix akela panel unblock nahi karega.
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
  // action nahi rukega). Instructions text audit me nahi — sirf
  // lengths + badle hue fields (audit log ko halka rakhne ke liye).
  async function audit(actionName: string, beforeJson: unknown, afterJson: unknown) {
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: actionName,
        target_user_id: null, before_json: beforeJson, after_json: afterJson,
      });
    } catch (_e) { /* audit fail se action nahi rukega */ }
  }
  const { action, instructions, knowledge } = await req.json().catch(() => ({}));
  if (action === "get") {
    const { data, error } = await svc.from("global_instructions").select("instructions,knowledge,updated_at").eq("id", 1).maybeSingle();
    if (error) return json({ error: error.message }, 500);
    return json({
      instructions: data?.instructions || "",
      knowledge: data?.knowledge || "",
      updated_at: data?.updated_at || null,
    });
  }
  if (action === "set") {
    if (instructions !== undefined && (typeof instructions !== "string" || instructions.length > 2000)) {
      return json({ error: "instructions max 2000 chars" }, 400);
    }
    if (knowledge !== undefined && (typeof knowledge !== "string" || knowledge.length > 2000)) {
      return json({ error: "knowledge max 2000 chars" }, 400);
    }
    const { data: cur } = await svc.from("global_instructions").select("instructions,knowledge").eq("id", 1).maybeSingle();
    const next = {
      id: 1,
      instructions: instructions !== undefined ? instructions : (cur?.instructions || ""),
      knowledge: knowledge !== undefined ? knowledge : (cur?.knowledge || ""),
      updated_at: new Date().toISOString(),
    };
    const { error } = await svc.from("global_instructions").upsert(next, { onConflict: "id" });
    if (error) return json({ error: error.message }, 500);
    await audit("instructions_set",
      { instructions_len: (cur?.instructions || "").length, knowledge_len: (cur?.knowledge || "").length },
      { instructions_len: next.instructions.length, knowledge_len: next.knowledge.length,
        changed_fields: [instructions !== undefined ? "instructions" : null, knowledge !== undefined ? "knowledge" : null].filter(Boolean) });
    return json({ ok: true, instructions_len: next.instructions.length, knowledge_len: next.knowledge.length });
  }
  return json({ error: "action get/set required" }, 400);
});
