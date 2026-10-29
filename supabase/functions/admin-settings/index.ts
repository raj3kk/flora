// admin-settings — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga; pehle admin_panel_tables.sql
// chalana zaroori hai).
// Kaam: admin-only settings padhna/likhna. Allowlist: SIRF
// "upi_vpa" — iske alawa koi setting is function se nahi
// badli ja sakti (client key bhej kar bhi nahi — key server
// par fixed hai).
// POST { action: "get" | "set", value? } + user JWT.
// UPI secret nahi hai — ye owner ka public payment address hai,
// isliye admin GET par value dikh sakti hai (aur public-settings
// se pay page par bhi dikhti hai). SET par basic VPA shape check
// hota hai; khaali ya beshape value reject.
// 401: JWT nahi; 403: is_admin nahi; 400: galat action/value.

// S4-2-free (Cycle 4 FIX): 500-path par DB error text (error.message)
// caller ko wapas NAHI milta — sirf generic message ("load failed" /
// "store failed") + detail console.error me (Edge logs me). 400s
// validation messages + success paths waise hi hain.

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

const VPA_RE = /^[A-Za-z0-9._-]{2,}@[A-Za-z0-9.-]{2,}$/;

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const svc = createClient(url, serviceKey);
  const adminId = userData.user.id;
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);
  // S1-32 audit helper (admin-approve pattern; audit fail se
  // action nahi rukega). UPI public payment address hai, isliye
  // before/after value audit me theek hai.
  async function audit(actionName: string, beforeJson: unknown, afterJson: unknown) {
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: actionName,
        target_user_id: null, before_json: beforeJson, after_json: afterJson,
      });
    } catch (_e) { /* audit fail se action nahi rukega */ }
  }
  const body = await req.json().catch(() => ({}));
  if (body.action === "get") {
    const { data, error } = await svc.from("admin_settings").select("value").eq("key", "upi_vpa").maybeSingle();
    if (error) { console.error(error); return json({ error: "load failed" }, 500); }
    return json({ ok: true, upi_vpa: data?.value || null });
  }
  if (body.action === "set") {
    const value = typeof body.value === "string" ? body.value.trim() : "";
    if (!value || !VPA_RE.test(value)) {
      return json({ error: "valid upi_vpa required (jaise naam@bank)" }, 400);
    }
    const { data: beforeRow } = await svc.from("admin_settings").select("value").eq("key", "upi_vpa").maybeSingle();
    const { error } = await svc.from("admin_settings").upsert({
      key: "upi_vpa", value, updated_at: new Date().toISOString(),
    });
    if (error) { console.error(error); return json({ error: "store failed" }, 500); }
    await audit("upi_set", { upi_vpa: beforeRow?.value || null }, { upi_vpa: value });
    return json({ ok: true });
  }
  return json({ error: "action must be get/set" }, 400);
});
