// Flora tts-proxy Edge Function (S3, repo copy — deploy pending).
// POST { text } + Authorization: Bearer <user JWT> -> audio bytes.
// JWT gate brain-proxy step 2 jaisa: anon client se user nikalo,
// na mile to 401. Provider: Deepgram TTS (server-side key ONLY).
// Key unset ho to 503 "tts keys not configured" — jhootha audio nahi.
// v2 (admin panel build 2026-10-29): key resolution = env FIRST
//   (DEEPGRAM_API_KEY dashboard secret jeetega), else provider_keys
//   DB row ("deepgram", panel se set). Baaki logic wahi hai.
// v3 (LFMS licence gate, 2026-10-29): brain-proxy v5 wala hi gate
//   (Pro valid ya Free < 600 talk sec/day, nahi to 429 free_limit).
//   Body me user_id nahi hota — JWT user hi user hai. Usage
//   public.usage se padha jaata hai (usage-count se bharta hai);
//   gate sirf padhta hai, increment nahi karta. DB error par
//   fail-open. Device binding yahan check nahi hoti.
// Text cap: 1000 chars (cost control). User ka text kabhi log nahi.
// DEPLOY + KEYS PENDING: dashboard editor se deploy + DEEPGRAM_API_KEY
// set karna baaki hai; app wiring (system TTS se proxy par) uske baad
// hoga — aaj app system TTS par hai, isliye ye change kuch todta nahi.
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// S2-1 (Cycle 122 FIX): CORS block — browser preflight ke liye.
// OPTIONS par 200 + wahi headers har response par; JWT gate
// waisa hi rahega. Allow-Origin "*" maujooda convention hai
// (S3-4 allowlist HELD — host list parent se aane par badlega).
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  // Key resolution (v2): env FIRST, else provider_keys DB row.
  async function resolveKey(provider: string, envName: string): Promise<string> {
    const envKey = Deno.env.get(envName) || "";
    if (envKey) return envKey;
    try {
      const { data } = await svc.from("provider_keys").select("key_value").eq("provider", provider).maybeSingle();
      return data?.key_value || "";
    } catch (_e) { return ""; }
  }

  const { text } = await req.json().catch(() => ({}));
  if (typeof text !== "string" || !text.trim()) {
    return json({ error: "text required" }, 400);
  }
  if (text.length > 1000) {
    return json({ error: "text too long (max 1000 chars)" }, 400);
  }

  // v3 licence gate (LFMS, brain-proxy v5 jaisa): JWT user ki
  // licence/usage state check, Deepgram call se pehle (key burn
  // nahi). Pro valid (active + unexpired) -> allow. Free -> aaj
  // ka usage (public.usage, UTC day) < 600 talk sec ho to allow,
  // nahi to 429 free_limit. Gate sirf padhta hai, increment nahi
  // karta. DB error par fail-open. Device binding yahan check
  // nahi hoti (licence-verify par enforce hoti hai).
  async function licenceAllowed() {
    try {
      const { data: rows } = await svc.from("licences")
        .select("plan_id,status,expires_at").eq("user_id", userData.user.id).eq("status", "active");
      const nowMs = Date.now();
      const hasPro = (rows || []).some((r) =>
        r.plan_id === "pro" && (!r.expires_at || new Date(r.expires_at).getTime() >= nowMs));
      if (hasPro) return true;
      const day = new Date().toISOString().slice(0, 10);
      const { data: u } = await svc.from("usage").select("talk_seconds")
        .eq("user_id", userData.user.id).eq("day", day).maybeSingle();
      return (u?.talk_seconds || 0) < 600;
    } catch (_e) { return true; }
  }
  if (!(await licenceAllowed())) {
    return json({ error: "free_limit", message: "Free limit poora ho gaya hai (600 sec/day) — Pro lene par unlimited milega." }, 429);
  }

  const dgKey = await resolveKey("deepgram", "DEEPGRAM_API_KEY");
  if (!dgKey) return json({ error: "tts keys not configured" }, 503);
  try {
    const r = await fetch(
      "https://api.deepgram.com/v1/speak?model=aura-asteria-en",
      {
        method: "POST",
        headers: {
          Authorization: `Token ${dgKey}`,
          "Content-Type": "application/json",
        },
        // S1-16 (Cycle 122 FIX): provider fetch par 30s timeout —
        // pehle koi timeout nahi tha. 30s engineering default hai.
        signal: AbortSignal.timeout(30000),
        body: JSON.stringify({ text }),
      },
    );
    if (!r.ok) return json({ error: "tts provider failed" }, 502);
    const audio = await r.arrayBuffer();
    return new Response(audio, {
      status: 200,
      headers: { "Content-Type": "audio/mpeg" },
    });
  } catch (_e) {
    return json({ error: "tts provider failed" }, 502);
  }
});
