// admin-list — DEPLOYED live 2026-10-28 ~10:59 IST via
// Supabase dashboard editor; live-verified auth gate (bina
// header gateway 401, anon-key Bearer par code ka apna
// {"error":"unauthorized"} 401). Asli list tabhi jab admin
// user banega (JWT + is_admin check ke baad).
// Kaam: admin ko Users + Usage + Licences ki list dena — kyunki
// in tables par RLS ON hai par koi policy nahi hai, isliye
// client-side (anon key) list hamesha khaali aati hai. Ye
// function service_role se padhta hai, isliye admin check
// function ke andar hi hota hai (koi bhi caller list nahi
// dekh sakta).
//
// POST {} (body khaali bhi chalega)
// Headers: Authorization: Bearer <user JWT> (Supabase Auth se).
// Steps:
// 1. JWT se user nikalo (anon client, auth.getUser). Na mile -> 401.
// 2. Service client se users row check: is_admin=true na ho -> 403.
// 3. users (id, device_id, is_admin, limit 200), usage (limit
//    200, day desc), licences (limit 200) wapas do.
// Kabhi wapas NAHI: email (public users me hai hi nahi), FCM
// token, UTR, koi secret. Payment rows isme nahi hain (wo
// admin page RLS se hi list karta hai).
// Deploy se pehle: is file ko Supabase dashboard editor me
// "Deploy a new function > Via Editor" se admin-list naam se
// banana hoga — DEPLOYED 2026-10-28 ~10:59 IST, auth gate
// live-verified. Repo copy = live code.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) {
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: { "Content-Type": "application/json" } });
  }
  const adminId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) {
    return new Response(JSON.stringify({ error: "admin only" }), { status: 403, headers: { "Content-Type": "application/json" } });
  }
  const { data: users } = await svc.from("users").select("id,device_id,is_admin").limit(200);
  const { data: usage } = await svc.from("usage").select("user_id,day,talk_seconds,tool_calls").order("day", { ascending: false }).limit(200);
  const { data: licences } = await svc.from("licences").select("user_id,plan_id,status,expires_at").limit(200);
  return new Response(JSON.stringify({ ok: true, users: users || [], usage: usage || [], licences: licences || [] }), { headers: { "Content-Type": "application/json" } });
});
