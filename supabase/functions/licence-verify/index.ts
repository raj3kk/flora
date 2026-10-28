// Flora licence-verify Edge Function (server-authoritative, no secrets in code).
// POST { user_id, device_id } -> { plan, status } from public.licences.
// 1-phone / 15-day change rule (server-side):
//   - Pro licence ek waqt par sirf EK device par active rehti hai.
//   - Naye device se check aaye aur user ki pro licence kisi doosre
//     device par active ho:
//       * activated_at se 15 din poore NA hue hon -> BLOCKED:
//         plan "free" + device_locked true + device_change_available_at
//         (purani device par licence chalti rehti hai, kuch move nahi hota).
//       * 15 din poore ho gaye hon -> TRANSFER: licence row naye device
//         par move ho jaati hai (device_id + activated_at update) aur
//         pro plan wapas milta hai. Purana device phir free ho jaata hai.
//   - Free plan par koi device lock nahi (free har device par free hai).
// Env (set in Supabase dashboard, never in repo): SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FIFTEEN_DAYS_MS = 15 * 24 * 60 * 60 * 1000;

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const { user_id, device_id } = await req.json().catch(() => ({}));
  if (!user_id || !device_id) return new Response("user_id + device_id required", { status: 400 });
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const json = (obj, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { "Content-Type": "application/json" } });

  const { data, error } = await supabase.from("licences")
    .select("plan_id,status,expires_at").eq("user_id", user_id).eq("device_id", device_id).maybeSingle();
  if (error) return json({ error: error.message }, 500);
  if (data) {
    const expired = data.expires_at && new Date(data.expires_at) < new Date();
    return json({
      plan: expired ? "free" : data.plan_id,
      status: expired ? "expired" : data.status,
    });
  }

  // Is device par koi licence row nahi: kya user ki pro licence kisi
  // doosre device par active hai? (latest activated_at wali pehle.)
  const { data: others, error: othersError } = await supabase.from("licences")
    .select("id,plan_id,status,expires_at,activated_at")
    .eq("user_id", user_id).order("activated_at", { ascending: false });
  if (othersError) return json({ error: othersError.message }, 500);
  const now = new Date();
  const pro = (others || []).find((r) =>
    r.plan_id === "pro" && r.status === "active" &&
    (!r.expires_at || new Date(r.expires_at) >= now));
  if (!pro) return json({ plan: "free", status: "free" });

  const activatedAt = new Date(pro.activated_at);
  const availableAt = new Date(activatedAt.getTime() + FIFTEEN_DAYS_MS);
  if (now < availableAt) {
    return json({
      plan: "free",
      status: "free",
      device_locked: true,
      device_change_available_at: availableAt.toISOString(),
    });
  }

  // 15 din poore: licence naye device par transfer karo.
  const { error: moveError } = await supabase.from("licences")
    .update({ device_id, activated_at: now.toISOString() }).eq("id", pro.id);
  if (moveError) return json({ error: moveError.message }, 500);
  return json({ plan: "pro", status: "active", device_changed: true });
});
