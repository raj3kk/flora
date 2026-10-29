// Flora brain-proxy Edge Function — Phase B step 2 (server-side brain).
// POST { user_id, device_id, prompt } + Authorization: Bearer <user JWT>
//   -> { reply, lane }.
// Step 2 me JWT gate lag gaya hai: anon client se JWT ka user nikala
// jaata hai (na mile to 401); body ka user_id JWT user se match nahi
// kare to 403 — koi doosre user ke naam par key burn nahi kar sakta.
// Provider keys server-side ONLY (env, never in repo/app):
//   GEMINI_API_KEY, GROQ_API_KEY (set in Supabase dashboard by owner).
// v3 (admin panel build 2026-10-29): key resolution = env FIRST
//   (dashboard secret jeetega), else provider_keys DB row
//   (panel se set ki gayi key, service-role read). Koi key na
//   mile to pehle wala 503 hi rahega. Baaki logic (JWT gate,
//   model chain, caps) bilkul wahi hai.
// Order: Gemini first, Groq fallback, then honest error. Keys never logged.
// Style: short-casual replies default (same instruction as app BrainClient).
// Prompt cap: 4000 chars (lambi prompt reject, cost control).
// DEPLOY + KEYS PENDING: ye repo copy hai — dashboard editor se deploy
// karna + keys set karna baaki hai; app wiring uske baad hoga (aaj app
// proxy se wired nahi hai, isliye ye change kuch todta nahi).
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const STYLE = "Tum Flora ho, ek friendly AI assistant. User se aise baat karo " +
  "jaise dost se casual conversation ho — chhota jawab do, aam taur par " +
  "1-2 line me, lamba ya formal mat bano. Hindi/English mix me, user jis " +
  "language me bole usi me jawab do. Kaam ki baat pehle; extra detail " +
  "sirf poochhe par.";

serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });

  // JWT gate (step 2): caller ka Supabase Auth JWT chahiye.
  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const jwtUserId = userData.user.id;
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  // Key resolution (v3): env FIRST, else provider_keys DB row.
  // DB key kabhi log nahi hogi, kabhi response me nahi jayegi.
  async function resolveKey(provider: string, envName: string): Promise<string> {
    const envKey = Deno.env.get(envName) || "";
    if (envKey) return envKey;
    try {
      const { data } = await svc.from("provider_keys").select("key_value").eq("provider", provider).maybeSingle();
      return data?.key_value || "";
    } catch (_e) { return ""; }
  }

  const { user_id, device_id, prompt } = await req.json().catch(() => ({}));
  if (!user_id || !device_id || typeof prompt !== "string" || !prompt.trim()) {
    return json({ error: "user_id, device_id, prompt required" }, 400);
  }
  if (user_id !== jwtUserId) return json({ error: "user_id does not match token" }, 403);
  if (prompt.length > 4000) {
    return json({ error: "prompt too long (max 4000 chars)" }, 400);
  }

  const geminiKey = await resolveKey("gemini", "GEMINI_API_KEY");
  if (geminiKey) {
    // v2.22: chain me verified-working models bhi (user key par
    // 2.5-flash/lite 404 dete hain; flash-lite-latest + 3.5 chalte hain).
    for (const model of ["gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-lite-latest", "gemini-3.5-flash"]) {
      try {
        const r = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              contents: [{ parts: [{ text: `${STYLE} Sawal: ${prompt}` }] }],
            }),
          },
        );
        if (r.ok) {
          const j = await r.json();
          const reply = j.candidates?.[0]?.content?.parts?.[0]?.text || "";
          if (reply.trim()) return json({ reply, lane: "gemini" });
        }
      } catch (_e) { /* next model, then Groq */ }
    }
  }

  const groqKey = await resolveKey("groq", "GROQ_API_KEY");
  if (groqKey) {
    try {
      const r = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${groqKey}` },
        body: JSON.stringify({
          model: "openai/gpt-oss-20b",
          messages: [
            { role: "system", content: STYLE },
            { role: "user", content: prompt },
          ],
        }),
      });
      if (r.ok) {
        const j = await r.json();
        const reply = j.choices?.[0]?.message?.content || "";
        if (reply.trim()) return json({ reply, lane: "groq" });
      }
    } catch (_e) { /* honest error below */ }
  }

  return json({ error: "brain keys not configured" }, 503);
});
