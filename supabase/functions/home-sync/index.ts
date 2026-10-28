// Home sync (v2.22, Phase 5, G10) — REPO ONLY, deploy
// Phase 4 me Supabase dashboard se hoga (GitHub push se
// functions deploy nahi hote).
//
// Dashboard deploy ke waqt chahiye (ek baar, SQL editor):
//   create table if not exists public.home_boxes (
//     device_id text primary key,
//     box_code text not null,
//     updated_at timestamptz default now()
//   );
//   alter table public.home_boxes enable row level security;
//   -- RLS: anon role ko insert/update/select dena hai ya
//   -- function ko service_role se chalana hai (dashboard
//   -- par verify_jwt setting ke saath faisla hoga).
//
// Request: {"device_id": "...", "box_code": "..."}
// Response: {"ok": true} ya {"ok": false, "error": "..."}

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

serve(async (req) => {
  try {
    if (req.method !== "POST") {
      return new Response(JSON.stringify({ ok: false, error: "POST only" }), {
        status: 405,
        headers: { "Content-Type": "application/json" },
      });
    }
    const body = await req.json();
    const deviceId = String(body.device_id ?? "").trim();
    const boxCode = String(body.box_code ?? "").trim();
    if (!deviceId || !boxCode) {
      return new Response(JSON.stringify({ ok: false, error: "device_id and box_code required" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }
    // Asli DB upsert yahan tab aayega jab home_boxes table
    // dashboard par ban jayegi (upar wali SQL). Tab tak
    // honest jawab: received, not stored.
    return new Response(
      JSON.stringify({ ok: true, received: true, stored: false, note: "home_boxes table pending (Phase 4)" }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (e) {
    return new Response(JSON.stringify({ ok: false, error: String(e) }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }
});
