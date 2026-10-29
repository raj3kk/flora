// admin-keys — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga; pehle admin_panel_tables.sql
// chalana zaroori hai, warna table-missing error aayega).
// Kaam: panel se provider keys set/status/delete karna.
// POST { action: "status" | "set" | "delete", provider, key_value? }
//   + Authorization: Bearer <user JWT>.
// Allowlist: gemini, groq, deepgram — iske bahar koi provider
// nahi. Table: provider_keys (service-role-only; RLS ON, koi
// policy nahi — client se koi read/write nahi).
// Key VALUE response me KABHI nahi aayegi, log me KABHI nahi
// aayegi. Status sirf booleans deta hai (present/missing).
// Dashboard secrets (env) hamesha pehle jeetenge — DB key sirf
// fallback hai (brain-proxy v3 + tts-proxy v2 me resolution).
// 401: JWT nahi; 403: is_admin nahi; 400: galat action/provider.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const PROVIDERS = ["gemini", "groq", "deepgram"];

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
  const { action, provider } = body;
  if (action === "status") {
    const { data, error } = await svc.from("provider_keys").select("provider");
    if (error) return json({ error: error.message }, 500);
    const present = new Set((data || []).map((r: any) => r.provider));
    const providers: Record<string, boolean> = {};
    for (const p of PROVIDERS) providers[p] = present.has(p);
    return json({ ok: true, providers });
  }
  if (!PROVIDERS.includes(provider)) {
    return json({ error: "provider must be gemini/groq/deepgram" }, 400);
  }
  if (action === "set") {
    const keyValue = typeof body.key_value === "string" ? body.key_value.trim() : "";
    if (!keyValue) return json({ error: "key_value required" }, 400);
    const { error } = await svc.from("provider_keys").upsert({
      provider, key_value: keyValue, updated_at: new Date().toISOString(),
    });
    if (error) return json({ error: error.message }, 500);
    return json({ ok: true });
  }
  if (action === "delete") {
    const { error } = await svc.from("provider_keys").delete().eq("provider", provider);
    if (error) return json({ error: error.message }, 500);
    return json({ ok: true });
  }
  return json({ error: "action must be status/set/delete" }, 400);
});
