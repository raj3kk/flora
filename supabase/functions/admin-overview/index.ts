// Flora admin-overview Edge Function — PHASE 2 (CEO overview aggregates).
// POST {} + Authorization: Bearer <user JWT> (Supabase Auth se).
// Admin check admin-approve pattern par: JWT se user nikalo
// (na mile -> 401); users.is_admin=true na ho -> 403.
// Wapas deta hai (sirf admin ko — emails/UTRs isi me hain):
//   users_total, linked, admins, pro_active, free_count,
//   pro_expiring_7d, revenue_paisa, pending_count, pending_paisa,
//   pending_actions (pending payments + unclaimed licences —
//   PURANA field waisa hi hai, panel shayad padhta hai) +
//   S2-4 (Cycle 2 FIX, ADDITIVE): pending_payments (pending
//   payments ki count) aur unclaimed_licences (NULL-device Pro
//   licences ki count) alag-alag — koi maujooda field na hati
//   hai, na badli hai.
//   usage_today_seconds, usage_today_tools,
//   recent_payments (5), recent_audit (5), push_recent (10),
//   heartbeats ({user_id: last_seen}), heartbeats_24h,
//   warnings (jo section load nahi hua uska naam — partial data
//   par chup nahi raha jata).
// Kabhi wapas NAHI: FCM token, key value, koi secret.
// Function-to-function live probe nahi karta (andar se possible
// nahi) — counts hi kaafi hain. Deploy se pehle
// admin_panel_phase2.sql chalao (push_log + heartbeats ke liye);
// wo tables na hon to un sections me warning aayega, baaki
// aggregates phir bhi milenge.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// S2-1 (Cycle 2 FIX, PARTIAL — sirf free functions): CORS preflight.
// Browser calls (panel/website, Authorization header wali) par
// OPTIONS ko 200 + allow headers do; wahi headers har response
// par bhi. Ye sirf browser preflight kholta hai — JWT/admin
// gates waise hi rahenge (CORS auth nahi hatata). LFMS functions
// (licence-verify, brain-proxy, tts-proxy, admin-licences,
// admin-plan-features, push-token-sync, usage-count, send-push)
// par CORS abhi NAHI laga hai, isliye poora S2-1 item QUEUED
// barkaraar hai — ye partial fix akela panel unblock nahi karega.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
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
  if (userErr || !userData?.user) {
    return json({ error: "unauthorized" }, 401);
  }
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", userData.user.id).maybeSingle();
  if (!adminRow?.is_admin) {
    return json({ error: "admin only" }, 403);
  }
  const warnings: string[] = [];
  const now = new Date();
  const today = now.toISOString().slice(0, 10);
  const in7d = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
  const in24h = new Date(now.getTime() - 24 * 60 * 60 * 1000);

  // Emails (best-effort — listUsers fail ho to map khaali rahega).
  // S1-13: admin-list jaisi paging (10 pages x 100) — pehle sirf
  // 1 page tha, ab 10 pages tak. 10 pages poore bhare hon to
  // emails_truncated=true (aur emails ho sakti hain).
  const emails: Record<string, string> = {};
  let emailsTruncated = false;
  try {
    for (let page = 1; page <= 10; page++) {
      const { data: lu } = await svc.auth.admin.listUsers({ page, perPage: 100 });
      if (!lu?.users?.length) break;
      for (const u of lu.users) {
        if (u.email) emails[u.id] = u.email;
      }
      if (lu.users.length < 100) break;
      if (page === 10) emailsTruncated = true;
    }
  } catch (_e) { /* emails map khaali rahega */ }

  const { data: users } = await svc.from("users").select("id,device_id,is_admin").limit(1000);
  if (!users) warnings.push("users");
  const usersTotal = (users || []).length;
  const linked = (users || []).filter((u) => !!u.device_id).length;
  const admins = (users || []).filter((u) => u.is_admin).length;

  const { data: lics, error: licErr } = await svc.from("licences")
    .select("user_id,device_id,plan_id,status,expires_at").limit(1000);
  if (licErr) warnings.push("licences");
  const activePro = (lics || []).filter((l) =>
    l.plan_id === "pro" && l.status === "active" &&
    (!l.expires_at || new Date(l.expires_at) >= now));
  const proUsers = new Set(activePro.map((l) => l.user_id));
  const proExpiring = activePro.filter((l) =>
    l.expires_at && new Date(l.expires_at) <= in7d).length;
  const unclaimed = activePro.filter((l) => l.device_id === null).length;

  const { data: pays, error: payErr } = await svc.from("payment_submissions")
    .select("id,user_id,amount_paisa,utr,note,status,created_at,reviewed_by,reviewed_at")
    .order("created_at", { ascending: false }).limit(1000);
  if (payErr) warnings.push("payments");
  const payRows = pays || [];
  let revenuePaisa = payRows.filter((p) => p.status === "approved")
    .reduce((s, p) => s + (p.amount_paisa || 0), 0);
  const pendRows = payRows.filter((p) => p.status === "pending");
  let pendingPaisa = pendRows.reduce((s, p) => s + (p.amount_paisa || 0), 0);
  // S1-13: revenue/pending par DB aggregate (PostgREST sum) try
  // karo; supported na ho ya error aaye to row sums hi rahenge
  // + truncated flag batayega ki sums adhoore ho sakte hain.
  let revenuePaisaExact = false, pendingPaisaExact = false;
  try {
    const { data: revAgg, error: revErr } = await svc.from("payment_submissions")
      .select("amount_paisa.sum()").eq("status", "approved");
    if (!revErr && revAgg && revAgg.length && typeof (revAgg[0] as any).sum === "number") {
      revenuePaisa = (revAgg[0] as any).sum;
      revenuePaisaExact = true;
    }
    const { data: pendAgg, error: pendErr } = await svc.from("payment_submissions")
      .select("amount_paisa.sum()").eq("status", "pending");
    if (!pendErr && pendAgg && pendAgg.length && typeof (pendAgg[0] as any).sum === "number") {
      pendingPaisa = (pendAgg[0] as any).sum;
      pendingPaisaExact = true;
    }
  } catch (_e) { /* aggregate na chale to row sums + truncated flag */ }
  const recentPayments = payRows.slice(0, 5).map((p) => ({
    id: p.id, user_email: emails[p.user_id] || null, user_id: p.user_id,
    amount_paisa: p.amount_paisa, utr: p.utr, note: p.note,
    status: p.status, created_at: p.created_at,
  }));

  const { data: usage, error: useErr } = await svc.from("usage")
    .select("user_id,day,talk_seconds,tool_calls")
    .order("day", { ascending: false }).limit(500);
  if (useErr) warnings.push("usage");
  let usageTodaySeconds = 0, usageTodayTools = 0;
  for (const r of (usage || [])) {
    if (String(r.day || "").slice(0, 10) === today) {
      usageTodaySeconds += r.talk_seconds || 0;
      usageTodayTools += r.tool_calls || 0;
    }
  }

  const { data: audit, error: audErr } = await svc.from("admin_audit")
    .select("admin_user_id,action,target_user_id,created_at")
    .order("created_at", { ascending: false }).limit(5);
  if (audErr) warnings.push("audit");
  const recentAudit = (audit || []).map((a) => ({
    admin_user_id: a.admin_user_id, action: a.action,
    target_user_id: a.target_user_id, created_at: a.created_at,
  }));

  const { data: plog, error: plogErr } = await svc.from("push_log")
    .select("user_id,template,sent_at,ok")
    .order("sent_at", { ascending: false }).limit(10);
  if (plogErr) warnings.push("push_log");
  const pushRecent = (plog || []).map((p) => ({
    user_email: emails[p.user_id] || null, user_id: p.user_id,
    template: p.template, sent_at: p.sent_at, ok: p.ok,
  }));

  const { data: beats, error: beatErr } = await svc.from("heartbeats")
    .select("user_id,last_seen").limit(1000);
  if (beatErr) warnings.push("heartbeats");
  const heartbeats: Record<string, string> = {};
  let heartbeats24h = 0;
  for (const b of (beats || [])) {
    heartbeats[b.user_id] = b.last_seen;
    if (b.last_seen && new Date(b.last_seen) >= in24h) heartbeats24h++;
  }

  return json({
    users_total: usersTotal, linked, admins,
    pro_active: proUsers.size, free_count: usersTotal - proUsers.size,
    pro_expiring_7d: proExpiring,
    revenue_paisa: revenuePaisa,
    pending_count: pendRows.length, pending_paisa: pendingPaisa,
    pending_actions: pendRows.length + unclaimed,
    pending_payments: pendRows.length,
    unclaimed_licences: unclaimed,
    usage_today_seconds: usageTodaySeconds, usage_today_tools: usageTodayTools,
    recent_payments: recentPayments, recent_audit: recentAudit,
    push_recent: pushRecent, heartbeats, heartbeats_24h: heartbeats24h,
    warnings,
    // S1-13 ADDITIVE: limit par pahunchne par truncated=true —
    // panel in totals ko "poora" mat samjhe. revenue/pending par
    // DB aggregate chal gaya ho to *_exact=true (tab truncated
    // ka us sum par asar nahi).
    truncated: {
      users: (users || []).length >= 1000,
      licences: (lics || []).length >= 1000,
      payments: payRows.length >= 1000,
      usage: (usage || []).length >= 500,
      heartbeats: (beats || []).length >= 1000,
      emails: emailsTruncated,
    },
    revenue_paisa_exact: revenuePaisaExact,
    pending_paisa_exact: pendingPaisaExact,
  });
});
