// Flora tts-proxy Edge Function (S3, repo copy — deploy pending).
// POST { text } + Authorization: Bearer <user JWT> -> audio bytes.
// JWT gate brain-proxy step 2 jaisa: anon client se user nikalo,
// na mile to 401. Provider: Deepgram TTS (server-side key ONLY).
// Key unset ho to 503 "tts keys not configured" — jhootha audio nahi.
// Text cap: 1000 chars (cost control). User ka text kabhi log nahi.
// DEPLOY + KEYS PENDING: dashboard editor se deploy + DEEPGRAM_API_KEY
// set karna baaki hai; app wiring (system TTS se proxy par) uske baad
// hoga — aaj app system TTS par hai, isliye ye change kuch todta nahi.
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);

  const { text } = await req.json().catch(() => ({}));
  if (typeof text !== "string" || !text.trim()) {
    return json({ error: "text required" }, 400);
  }
  if (text.length > 1000) {
    return json({ error: "text too long (max 1000 chars)" }, 400);
  }

  const dgKey = Deno.env.get("DEEPGRAM_API_KEY") || "";
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
