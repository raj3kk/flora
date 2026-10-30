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

// S2-1 (Cycle 122 FIX): CORS block — browser preflight ke liye.
// OPTIONS par 200 + wahi headers har response par; JWT gate
// waisa hi rahega.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req) => {
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), {
      status,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  try {
    if (req.method === "OPTIONS") {
      return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
    }
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

    // S1-8 (Cycle 122 FIX — prior-owner guard, registration path
    // par): device_id pehle se kisi DOOSRE user ke naam par ho to
    // overwrite NAHI — 403 + imaandaar message. Ye command path
    // NAHI banata (command endpoint abhi bhi maujood nahi hai);
    // sirf registration hijack vector band hota hai. Wahi user
    // dobara register kare to upsert se box_code update hota hai
    // (legit re-register chalta rahega). G10 command revisit
    // abhi bhi AAKHRI phase me HELD hai.
    const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const { data: priorBox } = await svc.from("home_boxes").select("user_id").eq("device_id", deviceId).maybeSingle();
    if (priorBox && priorBox.user_id !== userId) {
      return json({ ok: false, error: "device already registered to another account" }, 403);
    }
    // Service-role upsert: ek device_id = ek row (box badle to
    // update, naya device ho to insert). user_id audit ke liye
    // bhi save hota hai (kis account ka box hai).
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
    // S1-21 (Cycle 122 FIX): pehle har exception 400 + String(e)
    // leak karta tha (andar ki detail response me). Ab generic
    // 500 + detail sirf Edge logs me.
    console.error(e);
    return json({ ok: false, error: "store failed" }, 500);
  }
});
