// push-token-sync Edge Function v2 — LFMS (JWT gate + linked/unlinked).
// Kaam: app ka FCM token server ke push_tokens table me
// pahunchana, taaki send-push us user ko push bhej sake.
// v1 me koi JWT NAHI tha (documented risk): koi user_id UUID
// jaan le to uska token overwrite kar sakta tha, aur users row
// kisi bhi random UUID par claim ho jaati thi — yehi wajah thi
// ki 2026-10-29 ko app "Linked" dikhata raha par server par
// admin Auth UID ki row kabhi bani hi nahi (app random UUID
// sync kar raha tha).
// v2 gate (home-sync pattern):
//   LINKED path (user JWT mila): body user_id JWT user se match
//     hona chahiye (nahi to 403); tabhi users row upsert
//     (id=user_id, device_id) + push_tokens upsert. Yehi raasta
//     link flow ki users row banata hai (app link ke baad JWT
//     bhejta hai).
//   UNLINKED path (koi user JWT nahi — anon key / random-UUID
//     app): body ka user_id KABHI trust nahi hota, users row
//     claim NAHI hoti. device_id se users row dhoondho — mili
//     to us owner ke liye token upsert (server-resolved); na
//     mili to ok:false 'device not registered' (link ke baad
//     sync hoga). Fresh unlinked user ka token tab tak nahi
//     judta jab tak wo link na kare — ye security trade hai,
//     regression nahi (purana "kisi bhi UUID par row" hi bug tha).
// POST { user_id?, device_id, fcm_token } (linked me user_id
//   zaroori + JWT match; unlinked me sirf device_id + token).
// Token kabhi log nahi, response me kabhi wapas nahi.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  const body = await req.json().catch(() => ({}));
  const { user_id, device_id, fcm_token } = body;
  if (!device_id || !fcm_token || String(fcm_token).length < 20) {
    return json({ ok: false, detail: "device_id + fcm_token required" }, 400);
  }
  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData } = await anon.auth.getUser().catch(() => ({ data: null }));
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  let targetUserId = null;
  if (userData?.user) {
    // Linked path: caller-match zaroori, tabhi users row claim.
    if (!user_id || !UUID_RE.test(String(user_id)) || user_id !== userData.user.id) {
      return json({ ok: false, detail: "user_id does not match token" }, 403);
    }
    targetUserId = userData.user.id;
    const { error: userErr } = await svc.from("users").upsert(
      { id: targetUserId, device_id }, { onConflict: "id" },
    );
    if (userErr) return json({ ok: false, detail: userErr.message }, 500);
  } else {
    // Unlinked path: body user_id kabhi trust nahi hota, users
    // row claim NAHI hoti. Sirf registered device ka token.
    const { data: owner } = await svc.from("users").select("id").eq("device_id", device_id).maybeSingle();
    if (!owner) {
      return json({ ok: false, detail: "device not registered (link ke baad sync hoga)" });
    }
    targetUserId = owner.id;
  }
  const { error: tokErr } = await svc.from("push_tokens").upsert(
    { user_id: targetUserId, device_id, fcm_token, platform: "android" },
    { onConflict: "user_id,device_id" },
  );
  if (tokErr) return json({ ok: false, detail: tokErr.message }, 500);
  // S1-9 (Cycle 122 FIX): linked sync par purane device ke token
  // residue hatao — 1 user = 1 device model me purana token push
  // bhejta rehta to galat device par notification jaata. Sirf
  // linked path par (caller-match ke baad); unlinked path par
  // delete kabhi nahi (wahan owner server-resolved hai, caller
  // verified nahi). Delete best-effort hai — fail ho to bhi
  // upsert safal hi rahega.
  await svc.from("push_tokens").delete().eq("user_id", targetUserId).neq("device_id", device_id);
  return json({ ok: true });
});
