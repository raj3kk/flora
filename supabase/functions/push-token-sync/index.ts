// push-token-sync Edge Function — DRAFT, deploy baaki.
// Kaam: app ka FCM token server ke push_tokens table me
// pahunchana, taaki send-push us user ko push bhej sake.
//
// Kyun alag function: app ke paas Supabase Auth session NAHI
// hai (app userId random UUID hai), isliye client-side
// push_tokens insert RLS policy (auth.uid() = user_id) par
// fail hoga. Ye function service_role se chalta hai.
//
// POST { user_id (uuid), device_id (text), fcm_token }
// Steps:
// 1. Teenon field zaroori; user_id uuid format me hona chahiye.
// 2. users upsert (id=user_id, device_id) — isse ye bhi hota
//    hai ki usage-count ka FK aur admin-approve ka device_id
//    lookup us user ke liye kaam karne lagta hai.
// 3. push_tokens upsert (user_id, device_id) onConflict.
// 4. Token kabhi log nahi, response me kabhi wapas nahi.
// Risk note (imaandari se): is function me user JWT auth
// nahi hai, kyunki app ke paas Auth session nahi hai. Koi
// user_id UUID jaan le to uska token overwrite kar sakta
// hai — lekin user_id sirf us phone + server par hota hai,
// guess karne layak nahi (random UUID). Jab app me Auth
// login aayega, tab is function me JWT check lagana hoga.
// Deploy: Supabase dashboard editor se (route proven
// 2026-10-28) — GitHub push se Edge Function deploy NAHI hota.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const body = await req.json().catch(() => ({}));
  const { user_id, device_id, fcm_token } = body;
  if (!user_id || !UUID_RE.test(String(user_id))) {
    return new Response(JSON.stringify({ ok: false, detail: "user_id required (uuid)" }), { status: 400, headers: { "Content-Type": "application/json" } });
  }
  if (!device_id || !fcm_token || String(fcm_token).length < 20) {
    return new Response(JSON.stringify({ ok: false, detail: "device_id + fcm_token required" }), { status: 400, headers: { "Content-Type": "application/json" } });
  }
  const svc = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { error: userErr } = await svc.from("users").upsert(
    { id: user_id, device_id }, { onConflict: "id" },
  );
  if (userErr) return new Response(JSON.stringify({ ok: false, detail: userErr.message }), { status: 500, headers: { "Content-Type": "application/json" } });
  const { error: tokErr } = await svc.from("push_tokens").upsert(
    { user_id, device_id, fcm_token, platform: "android" },
    { onConflict: "user_id,device_id" },
  );
  if (tokErr) return new Response(JSON.stringify({ ok: false, detail: tokErr.message }), { status: 500, headers: { "Content-Type": "application/json" } });
  return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
});
