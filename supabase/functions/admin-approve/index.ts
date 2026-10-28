// admin-approve — DEPLOYED live 2026-10-28 ~10:03 IST via
// Supabase dashboard editor; live-verified (code ka apna
// {"error":"unauthorized"} 401 aata hai). Poora approve flow
// tabhi test hoga jab admin user banega. Repo copy = live code.
// Kaam: payment approve/reject + licence activation + audit.
// Client-side approve isliye nahi, kyunki licences ki live RLS
// policies unknown hain — ye function service_role se chalega,
// isliye RLS bypass hota hai, lekin admin check function ke
// andar hi hota hai (koi bhi caller approve nahi kar sakta).
//
// POST { payment_id, action: "approve" | "reject", reject_reason? }
// Headers: Authorization: Bearer <user JWT> (Supabase Auth se).
//
// Steps:
// 1. JWT se user nikalo (anon client, auth.getUser). Na mile -> 401.
// 2. Service client se users row check: is_admin=true na ho -> 403.
// 3. Payment row load karo: status pending na ho -> 400.
// 4. Approve: payment -> approved + reviewed_by/at; users.device_id
//    se licences upsert (user_id, device_id) plan_id='pro',
//    status='active', expires_at = ab + 30 din; audit insert.
//    users row ya device_id na mile to licence activate NAHI
//    hoga — error saaf return hoga, fake success nahi.
// 5. Reject: reason zaroori; payment -> rejected + audit insert.
// Money note: ye sirf UTR-manual approval hai — koi gateway,
// koi auto-charge nahi. Gateway ke liye alag money approval
// chahiye (user order se bahar hai).
// Deploy se pehle: supabase/functions/admin-approve/index.ts
// me daalkar Supabase CLI/dashboard se deploy karna hoga —
// GitHub push se Edge Function deploy NAHI hota.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("POST only", { status: 405 });
  const body = await req.json().catch(() => ({}));
  const { payment_id, action, reject_reason } = body;
  if (!payment_id || (action !== "approve" && action !== "reject")) {
    return new Response(JSON.stringify({ error: "payment_id + approve/reject required" }), { status: 400, headers: { "Content-Type": "application/json" } });
  }
  if (action === "reject" && !reject_reason) {
    return new Response(JSON.stringify({ error: "reject_reason required" }), { status: 400, headers: { "Content-Type": "application/json" } });
  }
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
  const { data: payment } = await svc.from("payment_submissions").select("*").eq("id", payment_id).maybeSingle();
  if (!payment) {
    return new Response(JSON.stringify({ error: "payment not found" }), { status: 404, headers: { "Content-Type": "application/json" } });
  }
  if (payment.status !== "pending") {
    return new Response(JSON.stringify({ error: "already reviewed" }), { status: 400, headers: { "Content-Type": "application/json" } });
  }
  const now = new Date().toISOString();
  if (action === "reject") {
    const { error } = await svc.from("payment_submissions").update({
      status: "rejected", reviewed_by: adminId, reviewed_at: now, reject_reason,
    }).eq("id", payment_id);
    if (error) return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: { "Content-Type": "application/json" } });
    await svc.from("admin_audit").insert({
      admin_user_id: adminId, action: "payment_rejected",
      target_user_id: payment.user_id, before_json: payment,
      after_json: { status: "rejected", reject_reason },
    });
    return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
  }
  // Approve path, option (ii) website-path (user order 2026-10-28):
  // payer ki users row / device_id na bhi ho to payment approve
  // ho jata hai — licence user_id par banti hai, device_id NULL
  // (UNCLAIMED). Jab app link hoga aur phone licence-verify
  // karega, to NULL-device pro row usi device par claim ho
  // jayegi (licence-verify me claim logic hai) — 15-din ka
  // wait us row par nahi lagta, kyunki wo kisi phone par kabhi
  // chali hi nahi. Order zaroori hai: pehle licence row bane,
  // tabhi payment approved — warna payment pending hi rahega.
  // Schema note: agar licences.device_id NOT NULL ho to NULL
  // insert fail hoga — tab dashboard SQL chahiye:
  // ALTER TABLE public.licences ALTER COLUMN device_id DROP NOT NULL;
  // (ye SQL deploy doc me hai, yahan blind alter nahi hai).
  const { data: userRow } = await svc.from("users").select("device_id").eq("id", payment.user_id).maybeSingle();
  const expires = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
  if (!userRow?.device_id) {
    const { data: existing } = await svc.from("licences").select("id").eq("user_id", payment.user_id).is("device_id", null).order("activated_at", { ascending: false }).limit(1).maybeSingle();
    let licErr = null;
    if (existing) {
      const r = await svc.from("licences").update({ plan_id: "pro", status: "active", activated_at: now, expires_at: expires }).eq("id", existing.id);
      licErr = r.error;
    } else {
      const r = await svc.from("licences").insert({ user_id: payment.user_id, device_id: null, plan_id: "pro", status: "active", activated_at: now, expires_at: expires });
      licErr = r.error;
    }
    if (licErr) return new Response(JSON.stringify({ error: "website-path licence failed: " + licErr.message + " — payment left pending" }), { status: 500, headers: { "Content-Type": "application/json" } });
    const { error: payErr } = await svc.from("payment_submissions").update({
      status: "approved", reviewed_by: adminId, reviewed_at: now,
    }).eq("id", payment_id);
    if (payErr) return new Response(JSON.stringify({ error: payErr.message }), { status: 500, headers: { "Content-Type": "application/json" } });
    await svc.from("admin_audit").insert({
      admin_user_id: adminId, action: "payment_approved_website_path",
      target_user_id: payment.user_id, before_json: payment,
      after_json: { status: "approved", plan_id: "pro", expires_at: expires, device_id: null },
    });
    return new Response(JSON.stringify({ ok: true, licence_activated: true, website_path: true, expires_at: expires }), { headers: { "Content-Type": "application/json" } });
  }
  // App-path (device_id maujood): pehle wala flow.
  const { error: licErr } = await svc.from("licences").upsert({
    user_id: payment.user_id, device_id: userRow.device_id,
    plan_id: "pro", status: "active", activated_at: now, expires_at: expires,
  }, { onConflict: "user_id,device_id" });
  if (licErr) return new Response(JSON.stringify({ error: licErr.message }), { status: 500, headers: { "Content-Type": "application/json" } });
  const { error: payErr } = await svc.from("payment_submissions").update({
    status: "approved", reviewed_by: adminId, reviewed_at: now,
  }).eq("id", payment_id);
  if (payErr) return new Response(JSON.stringify({ error: payErr.message }), { status: 500, headers: { "Content-Type": "application/json" } });
  await svc.from("admin_audit").insert({
    admin_user_id: adminId, action: "payment_approved",
    target_user_id: payment.user_id, before_json: payment,
    after_json: { status: "approved", plan_id: "pro", expires_at: expires },
  });
  return new Response(JSON.stringify({ ok: true, licence_activated: true, expires_at: expires }), { headers: { "Content-Type": "application/json" } });
});
