// Flora feature-flags Edge Function — PHASE 2 (server-side flag store).
// POST { user_id?: string } + Authorization: Bearer <anon key ya user JWT>
//   -> { flags: { talk, calls, sms, whatsapp, browser, macros,
//        image_gen, screen_vision, camera, overlay_mic, wakeword,
//        home, routines, form_fill } } — sab boolean.
// S1-11 contract (Cycle 1 FIX, Wave B/app team ke liye — CONTRACT CHANGE):
//   - Global scope: bina auth rehta hai (jaise pehle) — global
//     flags secret nahi hain, isliye isme koi JWT check nahi.
//   - Per-user scope (body me user_id maujood ho): user JWT
//     zaroori + caller-match (JWT user != body user_id -> 403).
//     Bina JWT ke per-user request par sirf global flags milenge
//     (fail-open barkaraar) + response me per_user_note hoga ki
//     per-user overrides ke liye JWT chahiye.
//   - S2-7 (Cycle 2 FIX): JWT-MISMATCH par bhi fail-open —
//     seedha 403 bina flags ke NAHI; balki no-JWT path jaisa:
//     global flags + per_user_applied:false + per_user_note
//     (mismatch ka zikr). Per-user override mismatch par KABHI
//     nahi diya jayega (S1-11 ka security maksad barkaraar —
//     sirf global flags milenge).
//   - App contract change: per-user overrides chahiye to request
//     me user JWT (Authorization: Bearer <user JWT>) bhejna hoga;
//     sirf anon key se per-user override NAHI milega.
// S1-34 precedence contract (define hi kiya gaya hai, enforce nahi):
//   effective feature = plan-entitlement AND flag-toggle (sabse
//   restrictive jeetega — plan OFF ya flag OFF, koi ek bhi OFF ho
//   to feature OFF). Enforcement app client (Wave B) + licence-
//   verify v2 (LFMS, queued) me hoga; ye function sirf flag-
//   toggle deta hai, plan-entitlement nahi deta.
// Logic: feature_flags table se scope='global' rows base + scope=
// body user_id wali rows per-key override (sirf JWT-match par);
// jo key na mile → true (fail-open — table/function ka missing
// hona app nahi todega). Table read service-role se (RLS
// no-policy — client se zero access).
// Deploy se pehle: supabase/sql/admin_panel_phase2.sql chalao;
// table na ho to bhi ye function sab true dega (catch → defaults).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// ZP-K7 scope clarification (CEO batch 2026-10-31, D-15 — agle asli server
// push ke saath ride, solo push nahi; sirf comment, behavior change nahi):
// is file ka CORS block SIRF is function (feature-flags) par lagta hai —
// ye kisi doosre function ke CORS ka daava nahi karta, aur S3-3/S3-4
// allowlist (Vercel domains only, wildcard nahi) ka faisla alag se
// Part 2 doc me darj hai, is comment se nahi badalta.
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

const FLAG_KEYS = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill",
];

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const { user_id } = await req.json().catch(() => ({}));
  const perUserRequested = typeof user_id === "string" && !!user_id;
  const flags: Record<string, boolean> = {};
  for (const k of FLAG_KEYS) flags[k] = true;
  // S1-11: per-user scope par JWT + caller-match. Bina JWT ke
  // fail-open: sirf global flags, note ke saath (403 nahi — purani
  // anon-key calls tootengi nahi, bas override nahi milega).
  let perUserApplied = false;
  let perUserNote: string | null = null;
  if (perUserRequested) {
    try {
      const url = Deno.env.get("SUPABASE_URL")!;
      const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
      const authHeader = req.headers.get("Authorization") || "";
      const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
      const { data: userData, error: userErr } = await anon.auth.getUser();
      if (userErr || !userData?.user) {
        perUserNote = "per-user overrides ke liye user JWT chahiye (bina JWT ke sirf global flags mile hain)";
      } else if (userData.user.id !== user_id) {
        // S2-7 fail-open: mismatch par 403 bina flags ke nahi —
        // global flags + note (per-user override kabhi nahi).
        perUserNote = "per-user overrides nahi mile: user_id token se match nahi karta (sirf global flags mile hain)";
      } else {
        perUserApplied = true;
      }
    } catch (_e) {
      perUserNote = "per-user overrides ke liye user JWT chahiye (bina JWT ke sirf global flags mile hain)";
    }
  }
  try {
    const svc = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );
    const { data } = await svc.from("feature_flags").select("key,enabled,scope");
    if (data) {
      for (const row of data) {
        if (row.scope === "global" && FLAG_KEYS.includes(row.key)) {
          flags[row.key] = !!row.enabled;
        }
      }
      if (perUserApplied) {
        for (const row of data) {
          if (row.scope === user_id && FLAG_KEYS.includes(row.key)) {
            flags[row.key] = !!row.enabled;
          }
        }
      }
    }
  } catch (_e) { /* fail-open: defaults (sab true) hi rahenge */ }
  const out: Record<string, unknown> = { flags };
  if (perUserRequested) out.per_user_applied = perUserApplied;
  if (perUserNote) out.per_user_note = perUserNote;
  return json(out);
});
