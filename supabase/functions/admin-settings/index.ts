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

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const VPA_RE = /^[A-Za-z0-9._-]{2,}@[A-Za-z0-9.-]{2,}$/;

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", userData.user.id).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);
  const body = await req.json().catch(() => ({}));
  if (body.action === "get") {
    const { data, error } = await svc.from("admin_settings").select("value").eq("key", "upi_vpa").maybeSingle();
    if (error) return json({ error: error.message }, 500);
    return json({ ok: true, upi_vpa: data?.value || null });
  }
  if (body.action === "set") {
    const value = typeof body.value === "string" ? body.value.trim() : "";
    if (!value || !VPA_RE.test(value)) {
      return json({ error: "valid upi_vpa required (jaise naam@bank)" }, 400);
    }
    const { error } = await svc.from("admin_settings").upsert({
      key: "upi_vpa", value, updated_at: new Date().toISOString(),
    });
    if (error) return json({ error: error.message }, 500);
    return json({ ok: true });
  }
  return json({ error: "action must be get/set" }, 400);
});
