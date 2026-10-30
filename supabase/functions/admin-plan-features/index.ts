// Flora admin-plan-features Edge Function — LFMS (plan -> features).
// POST { action, plan?, features? } + Authorization: Bearer <user JWT>.
// Admin check admin-approve pattern par: JWT (401) + is_admin (403).
// Ye mapping feature-flags system se ALAG hai (audit B17): flags
// global/per-user toggles hain, ye PER-PLAN mapping hai —
// licence-verify v2 isi table se features[] deta hai (plan ke
// hisaab se), isliye plans ka single source yehi table hai
// (plans table ko koi code read nahi karta — audit A7).
//   get: { plans: { free: [...], pro: [...] } } — row na ho to
//     defaults (free=[talk,builder], pro=15 flags); table na ho to bhi
//     defaults (fail-open, panel editor khaali nahi dikhega).
//   set { plan, features }: plan free/pro; features array, har
//     entry 15 contract keys me honi chahiye (allowlist); dedupe;
// B5 (2026-10-29): "builder" plan-features key hai — feature-flags ka
// toggle NAHI hai (flags FLAG_KEYS me builder nahi; effective = plan AND
// flag, missing key fail-open = true, isliye builder effectively
// plan-only hai). QUOTA CONTRACT (defaults): Free = max 3 projects +
// max 5 AI builds/day; Pro = unlimited. 1 AI build = 1 code-generation
// call; day = device local day. Quota enforcement APP-side/device-side
// honest gate hai, hard enforcement nahi (server project code nahi
// dekhta); server-side Free limiter brain-proxy 600s/day cap ALAG hai.
// Deploy order: allowlist wali ye function seed ke saath ya usse pehle
// deploy honi chahiye, warna panel Save builder par 400 dega.
//     upsert. Features config hai, secret nahi — isliye values
//     response/audit me dikhana theek hai.
// Deploy se pehle supabase/sql/licence_system.sql chalao.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FLAG_KEYS = [
  "talk", "calls", "sms", "whatsapp", "browser", "macros",
  "image_gen", "screen_vision", "camera", "overlay_mic", "wakeword",
  "home", "routines", "form_fill", "builder",
];

// S2-1 (Cycle 122 FIX): CORS block — panel browser calls par
// preflight ke liye. OPTIONS par 200 + wahi headers har response
// par; JWT/admin gates waise hi.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const DEFAULTS = {
  free: ["talk", "builder"],
  pro: FLAG_KEYS.slice(),
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const adminId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);

  const body = await req.json().catch(() => ({}));
  const { action } = body;

  if (action === "get") {
    const plans = { free: DEFAULTS.free.slice(), pro: DEFAULTS.pro.slice() };
    try {
      const { data } = await svc.from("plan_features").select("plan,features");
      for (const r of (data || [])) {
        if (Array.isArray(r.features) && (r.plan === "free" || r.plan === "pro")) {
          plans[r.plan] = r.features.filter((f) => FLAG_KEYS.includes(f));
        }
      }
    } catch (_e) { /* defaults upar hain */ }
    return json({ ok: true, plans });
  }

  if (action === "set") {
    const { plan, features } = body;
    if (plan !== "free" && plan !== "pro") return json({ error: "plan must be free/pro" }, 400);
    if (!Array.isArray(features)) return json({ error: "features must be an array" }, 400);
    const clean = [];
    for (const f of features) {
      if (!FLAG_KEYS.includes(f)) return json({ error: "feature allowlist me nahi hai: " + String(f) }, 400);
      if (!clean.includes(f)) clean.push(f);
    }
    const { error } = await svc.from("plan_features").upsert(
      { plan, features: clean, updated_at: new Date().toISOString() },
      { onConflict: "plan" },
    );
    if (error) return json({ error: error.message }, 500);
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: "plan_features_set",
        target_user_id: null, after_json: { plan, features: clean },
      });
    } catch (_e) { /* audit fail se set nahi rukega */ }
    return json({ ok: true });
  }

  return json({ error: "action get/set required" }, 400);
});
