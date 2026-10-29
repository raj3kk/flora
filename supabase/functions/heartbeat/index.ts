// Flora heartbeat Edge Function — PHASE 2 (app presence, last_seen only).
// POST { user_id } + Authorization: Bearer <user JWT>.
// JWT gate: anon client se JWT ka user nikalo (na mile to 401);
// body ka user_id JWT user se match nahi kare to 403 — koi
// doosre user ke naam par heartbeat nahi bhej sakta.
// Kaam: heartbeats table me { user_id, last_seen: now } upsert.
// "Online" status nahi hai — sirf aakhri seen time hai; panel
// isko "last seen" ke roop me dikhata hai, "online" kabhi nahi
// likhta. Koi aur data (location, screen, activity) kabhi nahi
// liya jata. Deploy se pehle admin_panel_phase2.sql chalao.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
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
  const { user_id } = await req.json().catch(() => ({}));
  if (!user_id) return json({ error: "user_id required" }, 400);
  if (user_id !== userData.user.id) return json({ error: "user_id does not match token" }, 403);
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { error } = await svc.from("heartbeats").upsert(
    { user_id, last_seen: new Date().toISOString() },
    { onConflict: "user_id" },
  );
  if (error) return json({ error: error.message }, 500);
  return json({ ok: true });
});
