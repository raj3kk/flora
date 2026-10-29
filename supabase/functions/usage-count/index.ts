// Flora usage-count Edge Function v2 — LFMS (JWT gate + alias fix).
// POST { user_id?, device_id?, talk_seconds?, seconds?, tool_calls? }
//   + Authorization: Bearer <anon key ya user JWT>
//   -> { counted, day, talk_seconds } ya { counted:false, detail }.
// v1 me koi JWT NAHI tha — koi bhi kisi bhi user_id par usage add
// kar sakta tha (free-cap DoS + panel totals pollution, audit B7).
// v2 gate (home-sync pattern):
//   LINKED path (user JWT mila): body user_id JWT user se match
//     hona chahiye (nahi to 403); usi user par increment.
//   UNLINKED path (koi user JWT nahi — anon key ya random-UUID
//     app): body ka user_id KABHI trust nahi hota. device_id se
//     users row dhoondho (users.device_id unique hai) — mili to
//     us owner par increment (server-resolved, caller-supplied
//     nahi); na mili to { counted:false, detail } — koi row nahi
//     banti, kisi user_id par write nahi hota.
//   NOTE (Wave B ke liye): app ka countUsage aaj device_id NAHI
//     bhejta (sirf user_id + seconds) — isliye unlinked reports
//     tab tak counted:false rahenge jab tak app device_id na
//     bhejne lage (ek line ka app change). Linked users JWT
//     bhejne par turant linked path par aa jayenge.
// ALIAS FIX (app<->server mismatch): app `seconds` bhejta hai,
//   v1 sirf `talk_seconds` padhta tha — isliye reports 0 add
//   karti thi (server usage hamesha ~0). v2 dono maanta hai:
//   talk_seconds mile to wahi, nahi to seconds. Double-count se
//   bachne ke liye kabhi dono ka jod nahi hota.
// Response shape: app `counted` + `detail` padhta hai (v1 ye
//   fields bhejta hi nahi tha — isliye app hamesha not-counted
//   samajhta tha). Soft failures 200 + counted:false par aate
//   hain (app result ignore karta hai, crash nahi hona chahiye);
//   sirf linked-path mismatch 403 hai (security, hard).
// Free cap (600 talk sec/day) yahan ENFORCE nahi hota — ye
//   counter hai; enforcement brain-proxy v5 / tts-proxy v3 gate
//   me hai (isi table ko padh kar, UTC day).
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });
  const body = await req.json().catch(() => ({}));
  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData } = await anon.auth.getUser().catch(() => ({ data: null }));
  const svc = createClient(url, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  // Target user tay karo (linked vs unlinked path).
  let targetUserId = null;
  if (userData?.user) {
    // Linked path: caller-match zaroori.
    if (!body.user_id || body.user_id !== userData.user.id) {
      return json({ error: "user_id does not match token" }, 403);
    }
    targetUserId = userData.user.id;
  } else {
    // Unlinked path: body user_id kabhi trust nahi hota.
    if (!body.device_id) {
      return json({ counted: false, detail: "device_id required (app update: countUsage me device_id bhejo)" });
    }
    const { data: owner } = await svc.from("users").select("id").eq("device_id", body.device_id).maybeSingle();
    if (!owner) {
      return json({ counted: false, detail: "device not registered (link ke baad sync hoga)" });
    }
    targetUserId = owner.id;
  }

  // Seconds alias: talk_seconds canonical, nahi to seconds.
  let secs = 0;
  if (typeof body.talk_seconds === "number" && body.talk_seconds > 0) secs = body.talk_seconds;
  else if (typeof body.seconds === "number" && body.seconds > 0) secs = body.seconds;
  let tools = 0;
  if (typeof body.tool_calls === "number" && body.tool_calls > 0) tools = body.tool_calls;

  const day = new Date().toISOString().slice(0, 10);
  const { data: row } = await svc.from("usage").select("*").eq("user_id", targetUserId).eq("day", day).maybeSingle();
  if (row) {
    const { error } = await svc.from("usage").update({
      talk_seconds: row.talk_seconds + secs, tool_calls: row.tool_calls + tools,
    }).eq("id", row.id);
    if (error) return json({ error: error.message }, 500);
    return json({ counted: true, day, talk_seconds: row.talk_seconds + secs });
  }
  const { error } = await svc.from("usage").insert({ user_id: targetUserId, day, talk_seconds: secs, tool_calls: tools });
  if (error) return json({ error: error.message }, 500);
  return json({ counted: true, day, talk_seconds: secs });
});
