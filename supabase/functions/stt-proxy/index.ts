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
// STT COUNTING (user decision 2026-10-31): STT seconds bhi isi
// 600s/day talk quota me ginte hain — isliye successful
// transcription ke baad Deepgram ki metadata.duration (ceil,
// max 120s) se usage counter increment hota hai (increment_usage
// RPC, wahi UTC-day boundary jo gate + usage-count use karte
// hain — koi nayi boundary nahi, Free limit 600s wahi hai).
// Sirf Free users count hote hain; Pro unlimited hai isliye app
// countUsage ki tarah skip hota hai. Increment fail ho to
// transcript phir bhi milta hai (fail-open) — counting kabhi
// baat-cheet nahi rokti.
// SCOPE NOTE: ye counting sirf is proxy path ki hai. App ka
// main-flow STT (system recognizer, text-only, koi duration
// source nahi) yahan count nahi hota — uska koi reliable
// duration source code me maujood nahi hai, isliye invent nahi
// kiya gaya (alag gap, alag decision chahiye). Device binding
// yahan check nahi hoti (licence-verify par enforce hoti hai).
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

  // D-21 encryption: enc:v1: rows decrypt (PROVIDER_KEYS_ENCRYPTION_KEY);
  // legacy plaintext rows migration ke dauran waise hi padhi jayengi.
  async function decryptStored(stored: string): Promise<string> {
    if (!stored.startsWith("enc:v1:")) return stored;
    try {
      const secret = Deno.env.get("PROVIDER_KEYS_ENCRYPTION_KEY") || "";
      if (!secret) return "";
      const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret));
      const key = await crypto.subtle.importKey("raw", digest, { name: "AES-GCM" }, false, ["decrypt"]);
      const bin = atob(stored.slice("enc:v1:".length));
      const bytes = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
      const plain = await crypto.subtle.decrypt({ name: "AES-GCM", iv: bytes.slice(0, 12) }, key, bytes.slice(12));
      return new TextDecoder().decode(plain);
    } catch (_e) { return ""; }
  }
  async function resolveKey(provider: string, envName: string): Promise<string> {
    const envKey = Deno.env.get(envName) || "";
    if (envKey) return envKey;
    try {
      const { data } = await svc.from("provider_keys").select("key_value").eq("provider", provider).maybeSingle();
      if (!data?.key_value) return "";
      return await decryptStored(data.key_value);
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

  async function licenceCheck(): Promise<{ allowed: boolean; isPro: boolean }> {
    try {
      const { data: rows } = await svc.from("licences")
        .select("plan_id,status,expires_at").eq("user_id", userData.user.id).eq("status", "active");
      const nowMs = Date.now();
      const hasPro = (rows || []).some((r) =>
        r.plan_id === "pro" && (!r.expires_at || new Date(r.expires_at).getTime() >= nowMs));
      if (hasPro) return { allowed: true, isPro: true };
      const day = new Date().toISOString().slice(0, 10);
      const { data: u } = await svc.from("usage").select("talk_seconds")
        .eq("user_id", userData.user.id).eq("day", day).maybeSingle();
      return { allowed: (u?.talk_seconds || 0) < 600, isPro: false };
    } catch (_e) { return { allowed: true, isPro: false }; }
  }
  const licence = await licenceCheck();
  if (!licence.allowed) {
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
    // STT counting: sirf successful transcription par, sirf Free
    // users ke liye. Duration Deepgram ki metadata.duration se
    // (wahi audio jo process hua — WAV-byte math ka anumaan nahi).
    // Na mile ya invalid ho to count skip (fail-open, invent nahi).
    // Double-count nahi: app ka onSpoken sirf TTS output seconds
    // ginta hai, ye STT input seconds hain — alag quantity.
    let counted = 0;
    if (!licence.isPro) {
      const rawDur = j.metadata?.duration;
      if (typeof rawDur === "number" && rawDur > 0) {
        counted = Math.min(Math.ceil(rawDur), 120);
        try {
          const day = new Date().toISOString().slice(0, 10);
          const { error: incErr } = await svc.rpc("increment_usage", {
            p_user_id: userData.user.id, p_day: day, p_secs: counted, p_tools: 0,
          });
          if (incErr) { console.error("stt count failed (fail-open)"); counted = 0; }
        } catch (_e) { console.error("stt count failed (fail-open)"); counted = 0; }
      }
    }
    return json({ transcript, language: lang, counted_seconds: counted });
  } catch (_e) {
    return json({ error: "stt provider failed" }, 502);
  }
});
