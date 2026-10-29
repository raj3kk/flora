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
// 3c. S2-5 (Cycle 2 FIX, non-licence pre-check ONLY): approve
//     par, licence regions se PEHLE, approved-UTR duplicate
//     check — usi UTR par koi DOOSRI submission pehle approved
//     ho chuki ho (status approved, id != current) to 400 +
//     admin_audit, licence NAHI banegi, payment pending hi
//     rahega. Reject path par ye check NAHI hai. UTR khaali/
//     missing ho to check skip (purani rows).
// 3b. Approve par amount check (S1-4, Cycle 1 FIX): approve
//     hamesha 'pro' 30-din licence deta hai — expected amount
//     pricing.html + panel Plans text se hai (dono: Pro
//     ₹199/mahina = 19900 paisa). Map me sirf pro hai, kyunki
//     free ka koi approve path nahi hai; koi aur plan/amount
//     aaye to wo is check se bahar hai (admin manual match hi
//     control rahega — koi amount blindly hardcode nahi kiya
//     gaya). Mismatch par 400 + admin_audit, licence NAHI banegi.
//     amount_paisa missing/non-number ho to check skip (purani
//     rows) — admin manual match hi control rahega.
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

// S2-8 (Cycle 3 FIX): CORS block — baaki 10 free functions jaisa.
// Browser se panel callApprove (Authorization + JSON) par preflight
// OPTIONS bhejta hai; is block ke bina approve/reject button browser
// me block rehta tha. OPTIONS par 200 + CORS headers; wahi headers
// har response par (json/raw 401/403/405 samet). JWT/admin gates
// waise hi rahenge — CORS sirf preflight kholta hai, auth nahi
// hatata. LICENCE-INSERT/SUPERSEDE/STACKING regions ko chhua nahi
// gaya hai. Allow-Methods POST, OPTIONS (ye function POST-only hai).
// S4-4 (Cycle 5 FIX, NON-LICENCE reject-path ONLY): reject-path par
// payment-update fail par error.message ki jagah generic "store failed"
// + console.error(error) detail (sirf Edge logs me). HTTP status 500
// aur response shape waisi hi hain, sirf error string badli hai.
// LICENCE-INSERT/SUPERSEDE/STACKING regions (reject branch ke baad
// shuru hote hain) ko ek line bhi nahi chhua gaya hai; admin-approve
// ki app-path lines (licence upsert fail / payment-update fail,
// S4-2-queued ka hissa) is pass me NAHI hain — sirf reject-path
// wali site badli gayi hai.

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
  }
  if (req.method !== "POST") return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
  const body = await req.json().catch(() => ({}));
  const { payment_id, action, reject_reason } = body;
  if (!payment_id || (action !== "approve" && action !== "reject")) {
    return new Response(JSON.stringify({ error: "payment_id + approve/reject required" }), { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  if (action === "reject" && !reject_reason) {
    return new Response(JSON.stringify({ error: "reject_reason required" }), { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const authHeader = req.headers.get("Authorization") || "";
  const anon = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } } });
  const { data: userData, error: userErr } = await anon.auth.getUser();
  if (userErr || !userData?.user) {
    return new Response(JSON.stringify({ error: "unauthorized" }), { status: 401, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  const adminId = userData.user.id;
  const svc = createClient(url, serviceKey);
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) {
    return new Response(JSON.stringify({ error: "admin only" }), { status: 403, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  const { data: payment } = await svc.from("payment_submissions").select("*").eq("id", payment_id).maybeSingle();
  if (!payment) {
    return new Response(JSON.stringify({ error: "payment not found" }), { status: 404, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  if (payment.status !== "pending") {
    return new Response(JSON.stringify({ error: "already reviewed" }), { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  // S1-4 amount check (approve path par hi; reject par nahi —
  // reject koi licence nahi deta). Licence regions ko chhua
  // nahi gaya hai — ye block unse pehle, alag hai.
  if (action === "approve") {
    const EXPECTED_PAISA_FOR_PRO = 19900; // Pro ₹199/mahina (pricing.html + panel Plans)
    const amt = (payment as any).amount_paisa;
    if (typeof amt === "number" && amt !== EXPECTED_PAISA_FOR_PRO) {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: "payment_approve_amount_mismatch",
        target_user_id: payment.user_id, before_json: payment,
        after_json: { expected_paisa: EXPECTED_PAISA_FOR_PRO, actual_paisa: amt, plan_id: "pro" },
      });
      return new Response(JSON.stringify({ error: "amount mismatch: expected 19900 paisa (Pro ₹199/mahina) for pro licence" }), { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
    }
  }
  // S2-5 approved-UTR duplicate check (approve path par hi;
  // licence-insert/supersede/stacking regions ko chhua nahi
  // gaya hai — ye block unse pehle, alag hai).
  if (action === "approve" && payment.utr) {
    const { data: dupApproved } = await svc.from("payment_submissions")
      .select("id").eq("utr", payment.utr).eq("status", "approved")
      .neq("id", payment_id).limit(1).maybeSingle();
    if (dupApproved) {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: "payment_approve_duplicate_utr",
        target_user_id: payment.user_id, before_json: payment,
        after_json: { duplicate_of_payment_id: dupApproved.id, utr: payment.utr },
      });
      return new Response(JSON.stringify({ error: "duplicate UTR: is UTR par ek payment pehle hi approved ho chuka hai" }), { status: 400, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
    }
  }
  const now = new Date().toISOString();
  if (action === "reject") {
    const { error } = await svc.from("payment_submissions").update({
      status: "rejected", reviewed_by: adminId, reviewed_at: now, reject_reason,
    }).eq("id", payment_id);
    if (error) { console.error(error); return new Response(JSON.stringify({ error: "store failed" }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } }); }
    await svc.from("admin_audit").insert({
      admin_user_id: adminId, action: "payment_rejected",
      target_user_id: payment.user_id, before_json: payment,
      after_json: { status: "rejected", reject_reason },
    });
    return new Response(JSON.stringify({ ok: true }), { headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
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
  // LFMS minimal edit (website-path ONLY — app-path neeche waisa hi
  // hai): key generation + supersede + renewal stacking.
  // Key generator admin-licences wale ka mirror hai (Deno
  // functions file share nahi kar sakte).
  const KEY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
  function genKeyPart() {
    const buf = new Uint32Array(4);
    crypto.getRandomValues(buf);
    let s = "";
    for (let i = 0; i < 4; i++) s += KEY_ALPHABET[buf[i] % KEY_ALPHABET.length];
    return s;
  }
  function genKey() {
    return "FLORA-" + genKeyPart() + "-" + genKeyPart() + "-" + genKeyPart();
  }
  // Default expiry (app-path isi ko use karta hai): ab + 30 din.
  // Website-path me stacking se overwrite hoga (neeche).
  let expires = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
  if (!userRow?.device_id) {
    const { data: existing } = await svc.from("licences").select("id").eq("user_id", payment.user_id).is("device_id", null).order("activated_at", { ascending: false }).limit(1).maybeSingle();
    // Prior actives: stacking base + supersede list (ek hi query).
    const { data: priors } = await svc.from("licences").select("id,expires_at").eq("user_id", payment.user_id).eq("status", "active");
    // Renewal stacking (default rule, user correct kar sakta hai):
    // nayi expiry = max(ab, pehli expiry) + 30 din — jaldi renew
    // par bacha hua time overwrite nahi hota.
    let baseMs = Date.now();
    for (const p of (priors || [])) {
      if (p.expires_at) baseMs = Math.max(baseMs, new Date(p.expires_at).getTime());
    }
    expires = new Date(baseMs + 30 * 24 * 60 * 60 * 1000).toISOString();
    let licErr = null;
    let licenceId = existing ? existing.id : null;
    if (existing) {
      // Reuse path: key waisi hi rehti hai (keyless row keyless
      // hi rahegi — backward compatible; key wali row ki key nahi
      // badalti).
      const r = await svc.from("licences").update({ plan_id: "pro", status: "active", activated_at: now, expires_at: expires }).eq("id", existing.id);
      licErr = r.error;
    } else {
      // Insert path: unique key ke saath (collision par retry, max 5).
      for (let attempt = 0; attempt < 5; attempt++) {
        const key = genKey();
        const r = await svc.from("licences").insert({ user_id: payment.user_id, device_id: null, plan_id: "pro", status: "active", activated_at: now, expires_at: expires, licence_key: key }).select("id").single();
        if (!r.error) { licenceId = r.data.id; break; }
        if (r.error.code === "23505") { licErr = r.error; continue; }
        licErr = r.error; break;
      }
      if (!licenceId && !licErr) licErr = { message: "key generation failed" };
    }
    if (licErr) return new Response(JSON.stringify({ error: "website-path licence failed: " + licErr.message + " — payment left pending" }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
    // Supersede: nayi/reused row chhod kar baaki active rows ko
    // 'superseded' karo (ek active rule — warna do devices par
    // do active rows dono Pro pate hain, 15-day lock trigger hi
    // nahi hota). Order: row pehle bani, tabhi supersede (insert
    // fail hota to user ke paas kuch nahi bachta).
    let supersededCount = 0;
    for (const p of (priors || [])) {
      if (p.id === licenceId) continue;
      const r = await svc.from("licences").update({ status: "superseded", superseded_by: licenceId }).eq("id", p.id);
      if (r.error) return new Response(JSON.stringify({ error: "licence supersede failed: " + r.error.message + " — payment left pending" }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
      supersededCount++;
    }
    const { error: payErr } = await svc.from("payment_submissions").update({
      status: "approved", reviewed_by: adminId, reviewed_at: now,
    }).eq("id", payment_id);
    if (payErr) return new Response(JSON.stringify({ error: payErr.message }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
    await svc.from("admin_audit").insert({
      admin_user_id: adminId, action: "payment_approved_website_path",
      target_user_id: payment.user_id, before_json: payment,
      after_json: { status: "approved", plan_id: "pro", expires_at: expires, device_id: null, superseded_count: supersededCount },
    });
    return new Response(JSON.stringify({ ok: true, licence_activated: true, website_path: true, expires_at: expires }), { headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  }
  // App-path (device_id maujood): pehle wala flow.
  const { error: licErr } = await svc.from("licences").upsert({
    user_id: payment.user_id, device_id: userRow.device_id,
    plan_id: "pro", status: "active", activated_at: now, expires_at: expires,
  }, { onConflict: "user_id,device_id" });
  if (licErr) return new Response(JSON.stringify({ error: licErr.message }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  const { error: payErr } = await svc.from("payment_submissions").update({
    status: "approved", reviewed_by: adminId, reviewed_at: now,
  }).eq("id", payment_id);
  if (payErr) return new Response(JSON.stringify({ error: payErr.message }), { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
  await svc.from("admin_audit").insert({
    admin_user_id: adminId, action: "payment_approved",
    target_user_id: payment.user_id, before_json: payment,
    after_json: { status: "approved", plan_id: "pro", expires_at: expires },
  });
  return new Response(JSON.stringify({ ok: true, licence_activated: true, expires_at: expires }), { headers: { ...CORS_HEADERS, "Content-Type": "application/json" } });
});
