// send-push — DEPLOYED live 2026-10-28 ~10:00 IST via
// Supabase dashboard editor; live-verified (code ka apna
// 'unauthorized' 401 aata hai, deprecated SUPABASE_ANON_KEY
// default runtime me inject hota hai). Repo copy = live code.
// FCM server-side send function (Flora project flora-f22a7).
// Deploy tabhi hoga jab Supabase secret route solve ho jaye
// (Management API Flora ke liye dead hai — secret set karne ka
// rasta alag step me). Service account JSON KABHI is file me
// nahi likhna — sirf Supabase secret FIREBASE_SERVICE_ACCOUNT_JSON
// se aayega (poora JSON, env se). Token/device-token kabhi log nahi.
//
// Kaam: POST { user_id | device_token, title, body } → FCM HTTP v1
// send → sirf 2-3 tarah ke push (trial khatam, Pro expiry).
//
// AUDIT FIX 2026-10-28 (deploy se pehle, abhi draft me hi):
// - Arbitrary title/body ALLOWED NAHI hai — sirf neeche wale
//   templates chalenge, warna 400. Warna koi bhi is function
//   ko spam push bhejne ke liye use kar sakta tha.
// - AUTH FIX 2026-10-28 (draft me lag gaya, deploy baaki):
//   caller ko Authorization Bearer user JWT dena hoga; function
//   anon client se user nikalta hai, service client se
//   users.is_admin check karta hai (na ho to 403). Device token
//   caller se NAHI liya jata — server push_tokens table se
//   user_id ke hisaab se uthaya jata hai (latest updated_at).
//   Token na mile to 404, fake success nahi.
// - DEPLOY abhi bhi baaki hai: function code deploy ka rasta
//   (CLI/dashboard) solve hona baaki hai — dashboard secret
//   set ho chuka hai, function deploy nahi hua hai.
// PHASE 2 (2026-10-29): send attempt ke baad push_log insert
//   (user_id + template + ok — token kabhi log nahi hota).
//   push_log table admin_panel_phase2.sql se banti hai; table
//   na ho to insert fail hoga par send response wahi rahega
//   (best-effort catch). YE FILE LOCAL ONLY HAI — GitHub repo
//   me send-push maujood nahi hai, isliye ise git push me MAT
//   daalo; deploy dashboard editor se local file se hoga.
// Deno Deploy / Supabase Edge runtime ke liye likha gaya hai.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const PROJECT_ID = "flora-f22a7";

// S2-1 (Cycle 122 FIX, LOCAL DRAFT ONLY — dashboard redeploy par
// hi live hoga): CORS block — panel browser calls par preflight
// ke liye. OPTIONS par 200 + wahi headers har response par;
// JWT/admin gates waise hi.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

async function accessToken(serviceAccount: any): Promise<string> {
  // Google OAuth2 JWT flow (RS256) — service_account JSON se.
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const claim = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };
  const enc = (o: unknown) =>
    btoa(JSON.stringify(o)).replaceAll("+", "-").replaceAll("/", "_")
      .replaceAll("=", "");
  const unsigned = enc(header) + "." + enc(claim);
  const pem = serviceAccount.private_key
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const raw = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", raw, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false, ["sign"],
  );
  const sig = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned),
  );
  const jwt = unsigned + "." + btoa(
    String.fromCharCode(...new Uint8Array(sig)),
  ).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt,
  });
  const data = await res.json();
  return data.access_token as string;
}

Deno.serve(async (req: Request) => {
  try {
    if (req.method === "OPTIONS") {
      return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
    }
    if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
    const { user_id, template } = await req.json();
    if (!user_id || !template) {
      return new Response("missing fields", { status: 400, headers: { ...CORS_HEADERS } });
    }
    // Sirf fixed templates — caller ka manmana text nahi chalega.
    const allowedTemplates: Record<string, { title: string; body: string }> = {
      trial_ending: {
        title: "Flora trial khatam hone wala hai",
        body: "Aapka free trial khatam hone wala hai. Pro lene ke liye app me dekho.",
      },
      pro_expiry: {
        title: "Flora Pro khatam hone wala hai",
        body: "Aapka Pro plan khatam hone wala hai. Renew karne ke liye app me dekho.",
      },
    };
    const tpl = allowedTemplates[template];
    if (!tpl) {
      return new Response("template not allowed", { status: 400, headers: { ...CORS_HEADERS } });
    }
    // Caller auth: JWT + admin check (admin-approve draft wala pattern).
    const url = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const authHeader = req.headers.get("Authorization") || "";
    const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
    const { data: userData, error: userErr } = await anon.auth.getUser();
    if (userErr || !userData?.user) {
      return new Response("unauthorized", { status: 401, headers: { ...CORS_HEADERS } });
    }
    const svc = createClient(url, serviceKey);
    const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", userData.user.id).maybeSingle();
    if (!adminRow?.is_admin) {
      return new Response("admin only", { status: 403, headers: { ...CORS_HEADERS } });
    }
    // Token server se: push_tokens, latest updated_at wala.
    const { data: tokenRow } = await svc.from("push_tokens")
      .select("fcm_token").eq("user_id", user_id)
      .order("updated_at", { ascending: false }).limit(1).maybeSingle();
    if (!tokenRow?.fcm_token) {
      return new Response("no device token for user", { status: 404, headers: { ...CORS_HEADERS } });
    }
    const saJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
    if (!saJson) return new Response("server not configured", { status: 500, headers: { ...CORS_HEADERS } });
    const token = await accessToken(JSON.parse(saJson));
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + token,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: tokenRow.fcm_token,
            notification: { title: tpl.title, body: tpl.body },
            android: { priority: "HIGH" },
          },
        }),
      },
    );
    // FCM ka response wapas do, lekin device token kabhi log nahi.
    // PHASE 2: send history push_log me (best-effort — table na ho
    // to bhi send response wahi rahega).
    // S1-31 (Cycle 122 FIX, LOCAL DRAFT ONLY): pehle raw FCM body
    // wapas jaati thi (shape alag). Ab {ok, message_id} JSON —
    // panel ka single-target display JSON.stringify par hi chalta
    // hai, bulk path sirf ok/fail ginta hai, isliye panel nahi
    // tootega. Token kabhi response me nahi aata.
    const ok = res.ok;
    try {
      await svc.from("push_log").insert({ user_id, template, ok });
    } catch (_e) { /* log fail = send fail nahi */ }
    const fcmText = await res.text();
    let messageId = null;
    try { messageId = JSON.parse(fcmText)?.name || null; } catch (_e) { /* raw text, id nahi */ }
    return new Response(
      JSON.stringify(ok ? { ok: true, message_id: messageId } : { ok: false, error: "send failed" }),
      { status: res.status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  } catch (_e) {
    return new Response("send failed", { status: 500, headers: { ...CORS_HEADERS } });
  }
});
