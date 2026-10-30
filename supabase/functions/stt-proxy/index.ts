// Flora stt-proxy Edge Function — Phase 1 (server-shift) Item C.
// REPO CODE ONLY — DEPLOY NAHI HUA HAI. Deploy parent-browser
// deploy session #2 me Section F ke saath coordinated hoga;
// live deploy claim is file se nahi karna.
// POST { audio_base64, language? } + Authorization: Bearer <user JWT>
//   -> { transcript, language }.
// JWT gate brain-proxy/tts-proxy jaisa: anon client se user nikalo,
// na mile to 401. Body me user_id nahi hota — JWT user hi user hai.
// Provider: Deepgram listen (server-side key ONLY, env FIRST else
// provider_keys DB row "deepgram" — tts-proxy v2 wala hi pattern;
// STT aur TTS ek hi DEEPGRAM_API_KEY env share karte hain).
// Key unset ho to 503 "stt keys not configured" — jhootha
// transcript kabhi nahi. Provider fail par 502 (honest error).
// Batch (REST) v1 hai — streaming WebSocket (barge-in) isme
// NAHI hai; wo future kaam hai (design doc dekho).
// Licence gate: tts-proxy v3 wala hi read-only gate (Pro valid
// ya Free < 600 talk sec/day, nahi to 429 free_limit). Gate
// sirf PADHTA hai, increment nahi karta; DB error par fail-open.
// IMAANDAAR NOTE: aaj usage-count sirf boli hui (TTS) seconds
// ginta hai — STT input seconds us counter me nahi judte, isliye
// STT par ye gate approximate hai (fail-open hi rahega jab tak
// usage-count STT ko cover na kare). Device binding yahan check
// nahi hoti (licence-verify par enforce hoti hai).
// Audio: base64 WAV (16k mono PCM, app ke DeepgramClient wale
// wavBytes jaisa) — kabhi save nahi hota, sirf Deepgram ko
// bheja jaata hai, transcript wapas aata hai. Cap: base64
// 4,000,000 chars (~3 MB raw, ~90 sec) — usse zyada 400.
// Language: "hi" default (app hi-IN hai), "en" allowed, baaki 400.
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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

  async function resolveKey(provider: string, envName: string): Promise<string> {
    const envKey = Deno.env.get(envName) || "";
    if (envKey) return envKey;
    try {
      const { data } = await svc.from("provider_keys").select("key_value").eq("provider", provider).maybeSingle();
      return data?.key_value || "";
    } catch (_e) { return ""; }
  }

  const { audio_base64, language } = await req.json().catch(() => ({}));
  if (typeof audio_base64 !== "string" || !audio_base64.trim()) {
    return json({ error: "audio_base64 required" }, 400);
  }
  if (audio_base64.length > 4000000) {
    return json({ error: "audio too large (max ~90 sec)" }, 400);
  }
  const lang = language === undefined || language === "" ? "hi" : language;
  if (lang !== "hi" && lang !== "en") {
    return json({ error: "language must be hi or en" }, 400);
  }
  let audioBytes: Uint8Array;
  try {
    const bin = atob(audio_base64);
    audioBytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) audioBytes[i] = bin.charCodeAt(i);
  } catch (_e) {
    return json({ error: "audio_base64 invalid" }, 400);
  }
  if (audioBytes.length === 0) return json({ error: "audio_base64 invalid" }, 400);

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
  if (!dgKey) return json({ error: "stt keys not configured" }, 503);
  try {
    const r = await fetch(
      `https://api.deepgram.com/v1/listen?model=nova-2&language=${lang}&smart_format=true`,
      {
        method: "POST",
        headers: {
          Authorization: `Token ${dgKey}`,
          "Content-Type": "audio/wav",
        },
        signal: AbortSignal.timeout(60000),
        body: audioBytes,
      },
    );
    if (!r.ok) return json({ error: "stt provider failed" }, 502);
    const j = await r.json();
    const transcript = j.results?.channels?.[0]?.alternatives?.[0]?.transcript || "";
    if (!transcript.trim()) return json({ error: "stt transcript empty" }, 502);
    return json({ transcript, language: lang });
  } catch (_e) {
    return json({ error: "stt provider failed" }, 502);
  }
});
