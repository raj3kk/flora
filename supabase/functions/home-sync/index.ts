// Home sync (Phase 8 rewrite) — REPO ONLY, deploy Supabase
// dashboard se hoga (GitHub push se functions deploy nahi hote).
// Deploy checklist (parent browser task ke liye):
//   1. Is file ka poora content dashboard code editor me
//      home-sync function me paste + Deploy.
//   2. supabase/sql/home_sync_table.sql SQL editor me chalao
//      (home_boxes table + RLS ON, koi policy nahi —
//      service-role-only by design).
//   3. Function ki "Verify JWT" setting ON rakho (gateway
//      gate) — function ke andar bhi JWT check hai (dohri gate).
//
// Request (app HomeSyncClient se, linked accounts hi):
//   POST { user_id, device_id, box_code }
//   Headers: apikey <anon>, Authorization Bearer <user JWT>
// Response: {"ok": true, "stored": true} ya error.
//   401: JWT nahi / JWT se user nahi mila.
//   403: body user_id JWT user se match nahi karta.
//   400: user_id / device_id / box_code me se koi khaali.
// JWT gate brain-proxy step 2 wale pattern par hai (anon
// client se getUser, phir service-role client se upsert).
// Service-role key kabhi repo/app me nahi — sirf dashboard
// secret me. Koi token/key kabhi log/response me nahi.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

serve(async (req) => {
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  try {
    if (req.method !== "POST") {
      return json({ ok: false, error: "POST only" }, 405);
    }
    // JWT gate: caller ka Supabase Auth JWT chahiye.
    const url = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const authHeader = req.headers.get("Authorization") || "";
    if (!authHeader.startsWith("Bearer ")) {
      return json({ ok: false, error: "unauthorized" }, 401);
    }
    const anon = createClient(url, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await anon.auth.getUser();
    if (userErr || !userData?.user) {
      return json({ ok: false, error: "unauthorized" }, 401);
    }
    const jwtUserId = userData.user.id;

    const body = await req.json().catch(() => ({}));
    const userId = String(body.user_id ?? "").trim();
    const deviceId = String(body.device_id ?? "").trim();
    const boxCode = String(body.box_code ?? "").trim();
    if (!userId || !deviceId || !boxCode) {
      return json({ ok: false, error: "user_id, device_id and box_code required" }, 400);
    }
    if (userId !== jwtUserId) {
      return json({ ok: false, error: "user_id does not match token" }, 403);
    }

    // Service-role upsert: ek device_id = ek row (box badle to
    // update, naya device ho to insert). user_id audit ke liye
    // bhi save hota hai (kis account ka box hai).
    const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const { error: upErr } = await svc.from("home_boxes").upsert(
      {
        device_id: deviceId,
        user_id: userId,
        box_code: boxCode,
        updated_at: new Date().toISOString(),
      },
      { onConflict: "device_id" },
    );
    if (upErr) {
      return json({ ok: false, error: "store failed" }, 500);
    }
    return json({ ok: true, stored: true });
  } catch (e) {
    return json({ ok: false, error: String(e) }, 400);
  }
});
