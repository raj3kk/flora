// Flora gmail-proxy Edge Function — Phase 1 (server-shift) Item D.
// DRAFT ONLY — POORA IMPLEMENTATION CLAIM NAHI HAI.
// Google OAuth client/consent decision USER se pending hai (Google
// Cloud project, OAuth consent screen, gmail.readonly scope, token
// storage decision). Jab tak wo decision + deploy nahi hota, ye
// function 501 "gmail oauth not configured" dega — koi Google call,
// koi token read, koi email read nahi hoga.
// DEPLOY NAHI HUA HAI. Deploy parent-browser deploy session #2 me
// Section F ke saath coordinated hoga; live Gmail claim tab tak nahi.
// APP-SIDE GMAIL OPTION IS ROUND ME NAHI HATA — gate pakka hai:
// app-side option TABHI hatega jab server-side replacement LIVE
// verify ho jaye. Is round me sirf design + draft hai.
//
// Design (poora hone par):
// POST { user_id, action } + Authorization: Bearer <user JWT>
//   -> list: { messages: [{ id, snippet, from, subject, date }] }
//   -> get:  { id, snippet, from, subject, date } (snippet ONLY)
// JWT gate brain-proxy jaisa + caller-match: body user_id JWT user
// se match nahi kare to 403 — koi doosre user ka inbox nahi.
// Token lookup: service-role se gmail_tokens table (user_id par)
// — TABLE ABHI NAHI HAI, isliye lookup fail = 501 (neeche).
// Refresh: Google OAuth token endpoint se (env GOOGLE_OAUTH_CLIENT_ID
// / GOOGLE_OAUTH_CLIENT_SECRET — DONO PENDING, env me nahi hain).
// Scope v1 READ-ONLY (gmail.readonly) — SEND is version me NAHI hai.
// Response me kabhi poora body, attachment ya token nahi jaata —
// snippet + headers (from/subject/date) hi kaafi hain (app ka
// CommConsentScreen aaj bhi sirf snippets dikhata hai).
// Token kabhi log nahi hoga, kabhi response me nahi jayega.
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req) => {
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
  const jwtUserId = userData.user.id;

  const { user_id, action } = await req.json().catch(() => ({}));
  if (!user_id || !action) {
    return json({ error: "user_id, action required" }, 400);
  }
  if (user_id !== jwtUserId) return json({ error: "user_id does not match token" }, 403);
  if (action !== "list" && action !== "get") {
    return json({ error: "action must be list or get" }, 400);
  }

  // DRAFT GATE: OAuth decision pending hai — client id/secret env
  // me nahi hain, gmail_tokens table nahi hai. Isliye yahan hi
  // 501: neeche ka Google code tabhi chalega jab decision + deploy
  // + token row teeno maujood hon. Koi token read nahi hua.
  const clientId = Deno.env.get("GOOGLE_OAUTH_CLIENT_ID") || "";
  const clientSecret = Deno.env.get("GOOGLE_OAUTH_CLIENT_SECRET") || "";
  if (!clientId || !clientSecret) {
    return json({ error: "gmail oauth not configured" }, 501);
  }
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  let tokenRow: any = null;
  try {
    const { data } = await svc.from("gmail_tokens").select("*").eq("user_id", user_id).maybeSingle();
    tokenRow = data;
  } catch (_e) { tokenRow = null; }
  if (!tokenRow) {
    return json({ error: "gmail oauth not configured" }, 501);
  }

  // --- Poora implementation is gate ke baad aayega (decision ke baad):
  // 1. tokenRow.expires_at guzar gaya ho to refresh_token se naya
  //    access token lo (oauth2.googleapis.com/token), row update karo.
  // 2. Gmail API: list -> messages.list (maxResults 10, q inbox),
  //    get -> messages.get (format metadata, headers from/subject/date).
  // 3. Response me sirf id + snippet + from/subject/date — poora
  //    body, attachment, token kabhi nahi.
  // Aaj ye code yahan NAHI hai taaki aadha implementation live hone
  // ka daava na bane — decision ke baad isi file me poora hoga.
  return json({ error: "gmail oauth not configured" }, 501);
});
