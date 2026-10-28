// DRAFT ONLY — NOT deployed, NOT wired.
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
// - DEPLOY BLOCKER (abhi bhi khula hai): is draft me caller
//   auth check NAHI hai. Deploy se pehle zaroori hai: function
//   par JWT verification ON + caller admin hai ye check, aur
//   device_token caller se lene ki jagah server push_tokens
//   table se user_id ke hisaab se uthaye. Ye blocker khula
//   rahega jab tak admin panel Phase A ka auth design na ban
//   jaye — is function ko is haalat me deploy NAHI karna.
// Deno Deploy / Supabase Edge runtime ke liye likha gaya hai.

const PROJECT_ID = "flora-f22a7";

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
    const { device_token, title, body } = await req.json();
    if (!device_token || !title || !body) {
      return new Response("missing fields", { status: 400 });
    }
    // Sirf fixed templates — caller ka manmana text nahi chalega.
    const allowedTemplates: Record<string, string> = {
      trial_ending: "Aapka free trial khatam hone wala hai.",
      pro_expiry: "Aapka Pro plan khatam hone wala hai.",
    };
    const templateBody = allowedTemplates[title];
    if (!templateBody) {
      return new Response("template not allowed", { status: 400 });
    }
    const saJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
    if (!saJson) return new Response("server not configured", { status: 500 });
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
            token: device_token,
            notification: { title, body: templateBody },
            android: { priority: "HIGH" },
          },
        }),
      },
    );
    return new Response(await res.text(), { status: res.status });
  } catch (_e) {
    return new Response("send failed", { status: 500 });
  }
});
