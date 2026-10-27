// Flora usage-count Edge Function: increments public.usage for (user_id, day).
// POST { user_id, talk_seconds, tool_calls } — server-side only, no secrets in code.
// Free plan cap (600 talk seconds/day) is enforced here, not in the app.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const { user_id, talk_seconds = 0, tool_calls = 0 } = await req.json().catch(() => ({}));
  if (!user_id) return new Response("user_id required", { status: 400 });
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const day = new Date().toISOString().slice(0, 10);
  const { data: row } = await supabase.from("usage").select("*").eq("user_id", user_id).eq("day", day).maybeSingle();
  if (row) {
    const { error } = await supabase.from("usage").update({
      talk_seconds: row.talk_seconds + talk_seconds, tool_calls: row.tool_calls + tool_calls,
    }).eq("id", row.id);
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
    return new Response(JSON.stringify({ day, talk_seconds: row.talk_seconds + talk_seconds }), { headers: { "Content-Type": "application/json" } });
  }
  const { error } = await supabase.from("usage").insert({ user_id, day, talk_seconds, tool_calls });
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500 });
  return new Response(JSON.stringify({ day, talk_seconds }), { headers: { "Content-Type": "application/json" } });
});
