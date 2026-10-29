// Flora heartbeat Edge Function — PHASE 2 (app presence, last_seen only).
// POST { user_id } + Authorization: Bearer <user JWT>.
// JWT gate: anon client se JWT ka user nikalo (na mile to 401);
// body ka user_id JWT user se match nahi kare to 403 — koi
// doosre user ke naam par heartbeat nahi bhej sakta.
// Kaam: heartbeats table me { user_id, last_seen: now } upsert.
// S1-37 throttle (Cycle 1 FIX): 60s ke andar dobara call aaye
// to bina upsert ke { ok:true, throttled:true } — last_seen ki
// accuracy par 60s ka farq acceptable hai (panel "last seen"
// dikhata hai, "online" kabhi nahi likhta).
// "Online" status nahi hai — sirf aakhri seen time hai; panel
// isko "last seen" ke roop me dikhata hai, "online" kabhi nahi
// likhta. Koi aur data (location, screen, activity) kabhi nahi
// liya jata. Deploy se pehle admin_panel_phase2.sql chalao.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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

Deno.serve(async (req) => {
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
  const { user_id } = await req.json().catch(() => ({}));
  if (!user_id) return json({ error: "user_id required" }, 400);
  if (user_id !== userData.user.id) return json({ error: "user_id does not match token" }, 403);
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  // S1-37 per-user throttle: pichhla last_seen 60s se naya ho to
  // upsert skip (har call par write nahi — app har minute se
  // zyada heartbeat bhej sakta hai, DB write bachti hai).
  const { data: prev } = await svc.from("heartbeats").select("last_seen").eq("user_id", user_id).maybeSingle();
  if (prev?.last_seen && Date.now() - new Date(prev.last_seen).getTime() < 60 * 1000) {
    return json({ ok: true, throttled: true });
  }
  const { error } = await svc.from("heartbeats").upsert(
    { user_id, last_seen: new Date().toISOString() },
    { onConflict: "user_id" },
  );
  if (error) return json({ error: error.message }, 500);
  return json({ ok: true, throttled: false });
});
