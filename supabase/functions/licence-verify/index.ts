// Flora licence-verify Edge Function (server-authoritative, no secrets in code).
// POST { user_id, device_id } -> { plan, status } from public.licences.
// Env (set in Supabase dashboard, never in repo): SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const { user_id, device_id } = await req.json().catch(() => ({}));
  if (!user_id || !device_id) return new Response("user_id + device_id required", { status: 400 });
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data, error } = await supabase.from("licences")
    .select("plan_id,status,expires_at").eq("user_id", user_id).eq("device_id", device_id).maybeSingle();
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  if (!data) return new Response(JSON.stringify({ plan: "free", status: "free" }), { headers: { "Content-Type": "application/json" } });
  const expired = data.expires_at && new Date(data.expires_at) < new Date();
  return new Response(JSON.stringify({
    plan: expired ? "free" : data.plan_id,
    status: expired ? "expired" : data.status,
  }), { headers: { "Content-Type": "application/json" } });
});
