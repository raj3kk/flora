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
// v4 (admin panel PHASE 2, 2026-10-29): global_instructions row
//   (id=1, panel se set) STYLE ke saath judti hai — instructions
//   + knowledge, proxy lane par hi asar (app-direct lanes apne
//   prompts use karte hain). Combined cap: global + prompt ≤ 4000
//   (global pehle, prompt truncate; akela prompt > 4000 ab bhi
//   reject — v3 wala cost control). Table na ho to global khaali
//   (fail-open, v3 jaisa jawab).
// v5 (LFMS licence gate, 2026-10-29): har call par user ki
//   licence/usage state check (service-role, JWT match ke baad,
//   provider call se pehle — key burn nahi hogi). Pro valid
//   (active + unexpired) -> chalne do. Free -> aaj ka usage < 600
//   talk sec ho to chalne do, nahi to 429 free_limit (503 nahi).
//   Usage maujooda public.usage counter se padha jaata hai
//   (usage-count/app-reporting se bharta hai, UTC day) — ye gate
//   sirf PADHTA hai, increment kabhi nahi karta (double-count
//   nahi). DB error par fail-open (allow). Device binding yahan
//   check NAHI hoti (entitlement + cap hi) — binding
//   licence-verify (app start) par enforce hoti hai.
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

// S2-1 (Cycle 122 FIX): CORS block — live probe me OPTIONS par
// 405 aata tha (koi CORS header nahi), browser preflight fail.
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

  // v4: global instructions/knowledge (panel se set, id=1).
  // Table na ho / row na ho to khaali — fail-open, v3 jaisa jawab.
  let globalText = "";
  try {
    const { data: g } = await svc.from("global_instructions")
      .select("instructions,knowledge").eq("id", 1).maybeSingle();
    const parts: string[] = [];
    if (g?.instructions?.trim()) parts.push("Global instructions:\n" + g.instructions.trim());
    if (g?.knowledge?.trim()) parts.push("Global knowledge:\n" + g.knowledge.trim());
    globalText = parts.join("\n\n");
  } catch (_e) { globalText = ""; }

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

  // v5 licence gate (LFMS): provider call se pehle, key burn nahi.
  // Pro valid (active + unexpired) -> allow. Free -> aaj ka usage
  // (public.usage, UTC day, usage-count se bharta hai) < 600 talk
  // sec ho to allow, nahi to 429 free_limit + imaandaar message.
  // Gate sirf padhta hai, increment nahi karta. DB error par
  // fail-open (allow). Device binding yahan check nahi hoti.
  async function licenceAllowed() {
    try {
      const { data: rows } = await svc.from("licences")
        .select("plan_id,status,expires_at").eq("user_id", user_id).eq("status", "active");
      const nowMs = Date.now();
      const hasPro = (rows || []).some((r) =>
        r.plan_id === "pro" && (!r.expires_at || new Date(r.expires_at).getTime() >= nowMs));
      if (hasPro) return true;
      const day = new Date().toISOString().slice(0, 10);
      const { data: u } = await svc.from("usage").select("talk_seconds")
        .eq("user_id", user_id).eq("day", day).maybeSingle();
      return (u?.talk_seconds || 0) < 600;
    } catch (_e) { return true; }
  }
  if (!(await licenceAllowed())) {
    return json({ error: "free_limit", message: "Free limit poora ho gaya hai (600 sec/day) — Pro lene par unlimited milega." }, 429);
  }

  if (prompt.length > 4000) {
    return json({ error: "prompt too long (max 4000 chars)" }, 400);
  }

  // v4 combined cap: global + prompt ≤ 4000 (global pehle, prompt truncate).
  // S1-7 (Cycle 122 FIX): pehle global 4000 par prompt 0 chars tak
  // chupchaap kat jaata tha — model ko user ka sawal mile bina hi
  // jawab dena padta tha. Ab: prompt poora kat jaye to imaandaar
  // 400; aanshik kate to response me prompt_truncated flag.
  if (globalText.length > 4000) globalText = globalText.slice(0, 4000);
  let effPrompt = prompt;
  if (globalText.length + effPrompt.length > 4000) {
    effPrompt = effPrompt.slice(0, Math.max(0, 4000 - globalText.length));
  }
  const promptTruncated = effPrompt.length < prompt.length;
  if (!effPrompt.length) {
    return json({ error: "prompt truncated to empty by global instructions — global instructions chhota karo ya prompt dobara bhejo" }, 400);
  }
  const globalSuffix = globalText ? "\n\n" + globalText : "";

  const geminiKey = await resolveKey("gemini", "GEMINI_API_KEY");
  if (geminiKey) {
    // v2.22: chain me verified-working models bhi (user key par
    // 2.5-flash/lite 404 dete hain; flash-lite-latest + 3.5 chalte hain).
    // S1-29 (Cycle 122 FIX): key URL query (?key=) me nahi — header
    // (x-goog-api-key) me, taaki key logs/URLs me na ghume.
    // S1-16 (Cycle 122 FIX): provider fetch par 30s timeout — pehle
    // koi timeout nahi tha, hang par call latakti rehti thi. 30s
    // engineering default hai (koi product number nahi); parent
    // badal sakta hai.
    for (const model of ["gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-lite-latest", "gemini-3.5-flash"]) {
      try {
        const r = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json", "x-goog-api-key": geminiKey },
            signal: AbortSignal.timeout(30000),
            body: JSON.stringify({
              contents: [{ parts: [{ text: `${STYLE}${globalSuffix} Sawal: ${effPrompt}` }] }],
            }),
          },
        );
        if (r.ok) {
          const j = await r.json();
          const reply = j.candidates?.[0]?.content?.parts?.[0]?.text || "";
          if (reply.trim()) return json({ reply, lane: "gemini", ...(promptTruncated ? { prompt_truncated: true } : {}) });
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
        signal: AbortSignal.timeout(30000),
        body: JSON.stringify({
          model: "openai/gpt-oss-20b",
          messages: [
            { role: "system", content: STYLE + globalSuffix },
            { role: "user", content: effPrompt },
          ],
        }),
      });
      if (r.ok) {
        const j = await r.json();
        const reply = j.choices?.[0]?.message?.content || "";
        if (reply.trim()) return json({ reply, lane: "groq", ...(promptTruncated ? { prompt_truncated: true } : {}) });
      }
    } catch (_e) { /* honest error below */ }
  }

  // S2-2 (Cycle 122 FIX): pehle sab provider fail hone par bhi 503
  // "brain keys not configured" milta tha — keys configured hon tab
  // bhi farq nahi dikhta tha. Ab: koi key hi nahi to 503 (keys
  // missing); key thi par provider fail hue to 502 (provider down).
  if (geminiKey || groqKey) return json({ error: "brain provider unavailable" }, 502);
  return json({ error: "brain keys not configured" }, 503);
});
