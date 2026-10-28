// Flora brain-proxy Edge Function — Phase B step 1 (server-side brain).
// POST { user_id, device_id, prompt } -> { reply }.
// Provider keys server-side ONLY (env, never in repo/app):
//   GEMINI_API_KEY, GROQ_API_KEY (set in Supabase dashboard by owner).
// Order: Gemini first, Groq fallback, then honest error. Keys never logged.
// Style: short-casual replies default (same instruction as app BrainClient).
// HONEST LIMIT (step 1): no JWT auth gate yet — app ke paas abhi Auth
// session nahi hai, isliye koi bhi user_id+device_id prompt bhej sakta
// hai. Key-burn risk step 2 me band hoga (app Auth login ke baad JWT
// gate + per-user quota). Tab tak owner keys tabhi daale jab proxy ka
// istemal chahiye; keys missing hon to function saaf 503 deta hai.
// Prompt cap: 4000 chars (lambi prompt reject, cost control).
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const STYLE = "Tum Flora ho, ek friendly AI assistant. User se aise baat karo " +
  "jaise dost se casual conversation ho — chhota jawab do, aam taur par " +
  "1-2 line me, lamba ya formal mat bano. Hindi/English mix me, user jis " +
  "language me bole usi me jawab do. Kaam ki baat pehle; extra detail " +
  "sirf poochhe par.";

serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const { user_id, device_id, prompt } = await req.json().catch(() => ({}));
  if (!user_id || !device_id || typeof prompt !== "string" || !prompt.trim()) {
    return new Response(JSON.stringify({ error: "user_id, device_id, prompt required" }), { status: 400 });
  }
  if (prompt.length > 4000) {
    return new Response(JSON.stringify({ error: "prompt too long (max 4000 chars)" }), { status: 400 });
  }
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });

  const geminiKey = Deno.env.get("GEMINI_API_KEY") || "";
  if (geminiKey) {
    try {
      const r = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiKey}`,
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
    } catch (_e) { /* fallback to Groq */ }
  }

  const groqKey = Deno.env.get("GROQ_API_KEY") || "";
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
