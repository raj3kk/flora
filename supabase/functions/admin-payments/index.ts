// admin-payments — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga).
// Kaam: saare users ke payment_submissions ki list dena. RLS se
// client ko sirf apni rows dikhti hain — isliye server function
// zaroori hai, warna admin ko doosre users ke payments kabhi
// nahi dikhenge. Approve/reject ISME NAHI hai — wo kaam sirf
// admin-approve karta hai (website-path ii wala), ye sirf list
// deta hai.
// POST { status?: "pending" | "approved" | "rejected" }
//   + Authorization: Bearer <user JWT>.
// 401: JWT nahi; 403: is_admin nahi. Limit 200, latest first.
// Response ADMIN-ONLY hai: isme UTR + note + reviewed_by/at +
// reject_reason bhi hote hain (admin ko UTR match karke approve
// karna hai) — ye response kabhi anon client ko nahi milega.
// Kabhi wapas NAHI: koi secret/key value.

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
  } catch (_e) { /* emails best-effort */ }
  return emails;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), { status, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) return json({ error: "unauthorized" }, 401);
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", userData.user.id).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);
  const body = await req.json().catch(() => ({}));
  const status = body.status;
  if (status !== undefined && status !== "pending" && status !== "approved" && status !== "rejected") {
    return json({ error: "status must be pending/approved/rejected" }, 400);
  }
  let q = svc.from("payment_submissions")
    .select("id,user_id,amount_paisa,utr,note,status,created_at,reviewed_by,reviewed_at,reject_reason")
    .order("created_at", { ascending: false }).limit(200);
  if (status) q = q.eq("status", status);
  const { data: payments, error } = await q;
  if (error) return json({ error: error.message }, 500);
  const emails = await loadEmails(svc);
  const out = (payments || []).map((p: any) => ({ ...p, user_email: emails[p.user_id] || null }));
  // S1-36 (Cycle 1 FIX, ADDITIVE): total DB count (status filter
  // ke saath) + truncated flag. Count query na chale to total
  // null rahega (panel isko "unknown" samjhe, 0 nahi).
  let total: number | null = null;
  try {
    let cq = svc.from("payment_submissions").select("*", { count: "exact", head: true });
    if (status) cq = cq.eq("status", status);
    const { count, error: countErr } = await cq;
    if (!countErr && typeof count === "number") total = count;
  } catch (_e) { /* total null hi rahega */ }
  return json({ ok: true, payments: out, total, truncated: (payments || []).length >= 200 });
});
