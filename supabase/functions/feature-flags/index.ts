// Flora feature-flags Edge Function — PHASE 2 (server-side flag store).
// POST { user_id?: string } + Authorization: Bearer <anon key>
//   -> { flags: { talk, calls, sms, whatsapp, browser, macros,
//        image_gen, screen_vision, camera, overlay_mic, wakeword,
//        home, routines, form_fill } } — sab boolean.
// NO user-JWT check (global config hai, secret nahi). Gateway
// verify ON ho to bhi anon key Bearer pass ho jata hai (anon key
// khud project ka valid JWT hai); verify OFF ho to bhi chalta hai.
// Logic: feature_flags table se scope='global' rows base + scope=
// body user_id wali rows per-key override; jo key na mile → true
// (fail-open — table/function ka missing hona app nahi todega).
// Table read service-role se (RLS no-policy — client se zero access).
// Deploy se pehle: supabase/sql/admin_panel_phase2.sql chalao;
// table na ho to bhi ye function sab true dega (catch → defaults).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FLAG_KEYS = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill",
];

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  const { user_id } = await req.json().catch(() => ({}));
  const flags: Record<string, boolean> = {};
  for (const k of FLAG_KEYS) flags[k] = true;
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
      if (typeof user_id === "string" && user_id) {
        for (const row of data) {
          if (row.scope === user_id && FLAG_KEYS.includes(row.key)) {
            flags[row.key] = !!row.enabled;
          }
        }
      }
    }
  } catch (_e) { /* fail-open: defaults (sab true) hi rahenge */ }
  return json({ flags });
});
