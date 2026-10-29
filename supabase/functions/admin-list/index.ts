// admin-list v2 — admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga — live abhi v1 hai, v2 deploy
// ke baad hi emails/push-presence/totals milenge).
// Kaam: admin ko Users + Usage + Licences + push-token presence
// + auth emails + usage totals dena. RLS wali tables par koi
// policy nahi hai, isliye client-side list hamesha khaali aati
// hai — ye function service_role se padhta hai, admin check
// function ke andar hi hota hai (koi bhi caller list nahi dekh
// sakta).
// v2 me naya (v1 se): emails (auth.admin.listUsers se id-match),
// push_tokens presence (token VALUE kabhi nahi bheji jaati — sirf
// haan/naa + latest updated_at), usage totals (mili hui rows ka
// jod; poora all-time tabhi jab rows 1000 ki limit me aa jayein).
// v3 (LFMS, 2026-10-29): licences select me id + device_id +
//   activated_at + masked key (last 4) juda — panel ko revoke/
//   rebind + per-licence list ke liye chahiye. FULL key kabhi
//   nahi aati (masked + last4 hi kaafi hai; full key sirf
//   admin-licences reveal se, audit ke saath, milti hai).
// Kabhi wapas NAHI: FCM token value, UTR, koi secret/key value,
// kisi user ka password/hash.
// POST {} + Authorization: Bearer <user JWT>.
// 401: JWT nahi; 403: is_admin nahi.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

async function loadEmails(svc: any): Promise<Record<string, string>> {
  const emails: Record<string, string> = {};
  try {
    for (let page = 1; page <= 10; page++) {
      const { data, error } = await svc.auth.admin.listUsers({ page, perPage: 100 });
      if (error || !data?.users?.length) break;
      for (const u of data.users) {
        if (u?.id && u?.email) emails[u.id] = u.email;
      }
      if (data.users.length < 100) break;
    }
  } catch (_e) { /* emails best-effort: bina email ke bhi list do */ }
  return emails;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) {
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: { "Content-Type": "application/json" } });
  }
  const adminId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) {
    return new Response(JSON.stringify({ error: "admin only" }), { status: 403, headers: { "Content-Type": "application/json" } });
  }
  const { data: users } = await svc.from("users").select("id,device_id,is_admin").limit(200);
  const { data: usageAll } = await svc.from("usage").select("user_id,day,talk_seconds,tool_calls").order("day", { ascending: false }).limit(1000);
  const { data: licences } = await svc.from("licences").select("id,user_id,device_id,plan_id,status,expires_at,activated_at,licence_key,revoked_reason,revoked_at,superseded_by").limit(200);
  const licencesOut = (licences || []).map((l: any) => ({
    id: l.id, user_id: l.user_id,
    device_id: l.device_id, plan_id: l.plan_id, status: l.status,
    expires_at: l.expires_at, activated_at: l.activated_at,
    has_key: !!l.licence_key,
    masked_key: l.licence_key ? ("FLORA-••••-••••-" + String(l.licence_key).slice(-4)) : null,
    key_last4: l.licence_key ? String(l.licence_key).slice(-4) : null,
    revoked_reason: l.revoked_reason || null, revoked_at: l.revoked_at || null,
    superseded_by: l.superseded_by || null,
  }));
  const { data: pushRows } = await svc.from("push_tokens").select("user_id,updated_at").limit(500);
  const push: Record<string, { has_token: boolean; updated_at: string | null }> = {};
  (pushRows || []).forEach((r: any) => {
    const prev = push[r.user_id];
    if (!prev || (r.updated_at && r.updated_at > (prev.updated_at || ""))) {
      push[r.user_id] = { has_token: true, updated_at: r.updated_at || null };
    }
  });
  const usageTotals: Record<string, { talk_seconds: number; tool_calls: number; days: number }> = {};
  (usageAll || []).forEach((r: any) => {
    const t = usageTotals[r.user_id] || { talk_seconds: 0, tool_calls: 0, days: 0 };
    t.talk_seconds += r.talk_seconds || 0;
    t.tool_calls += r.tool_calls || 0;
    t.days += 1;
    usageTotals[r.user_id] = t;
  });
  const emails = await loadEmails(svc);
  // S1-12 (Cycle 1 FIX): truncation flags + counts — ADDITIVE hi
  // hai, maujooda fields ki shape nahi tooti. Limit par pahunchne
  // par truncated=true (limit ke barabar rows = shayad aur rows
  // hain, panel isko "poora" mat samjhe).
  const usageFetched = usageAll || [];
  return new Response(JSON.stringify({
    ok: true,
    users: users || [],
    usage: usageFetched.slice(0, 200),
    licences: licencesOut,
    push,
    emails,
    usage_totals: usageTotals,
    truncated: {
      users: (users || []).length >= 200,
      licences: licencesOut.length >= 200,
      usage: usageFetched.length >= 1000,
      push: (pushRows || []).length >= 500,
    },
    counts: {
      users: (users || []).length,
      licences: licencesOut.length,
      usage_fetched: usageFetched.length,
      usage_returned: usageFetched.slice(0, 200).length,
      push_users: Object.keys(push).length,
    },
  }), { headers: { "Content-Type": "application/json" } });
});
