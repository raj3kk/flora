// whatsapp-template-send — Item 36 (SD-P2) server relay, CODE + DEPLOY-READY.
// Spec: ~/workspace/apk-recovery/sd-p0/FLORA_ITEM36_TEACHONCE_SERVER_SPEC.md
// (§2 minimal spec — path (a) user-authorized WhatsApp Business Cloud API
// templates ke through send). Path (b) user-confirmed wa.me path app-only
// hi rahega; is function ka usse koi lena-dena nahi hai.
//
// EK call = EK recipient, EK template, EK send. Array/recipients-list
// wali request reject hoti hai (bulk ka raasta spec me nahi hai).
//
// GATE ORDER (spec §2.4, badla nahi jayega): JWT → whatsapp flag →
// licence/usage → template-authorization → idempotency (duplicate key
// par pehle wale attempt ka result, provider dobara call nahi) →
// confirmation → rate-limit → config → confirmation consume → provider.
//
// LIVE SEND IS CODE SE AKELA ENABLE NAHI HOTA — deploy-ready ka matlab:
// code + tables SQL taiyaar hain. Live send ke liye ALAG se chahiye:
//   (1) supabase/sql/whatsapp_template_send_tables.sql dashboard me
//       chalana (tables banane ke liye);
//   (2) Supabase Edge secrets WHATSAPP_BUSINESS_TOKEN aur
//       WHATSAPP_PHONE_NUMBER_ID set karna (values kabhi repo me nahi);
//   (3) authorization + confirmation records banane ka raasta (Lane A
//       app-side / future endpoint — ye spec uska design nahi karta,
//       is function me record-creation endpoint nahi hai; ye function
//       sirf pehle se maujood active records verify karta hai);
//   (4) paid sends ke liye alag explicit spend approval (spec §6 —
//       is code me spend approval shamil nahi hai);
//   (5) function ka dashboard deploy (parent ka kaam).
// Inke bina function fail-closed rahega (koi send nahi, saaf error).
//
// DRAFT-PROPOSED NUMBERS (spec §2.5/§2.6 — parent-confirm PENDING,
// tay faisla nahi; sirf constants hain, structure badle bina tune ho
// sakte hain): rate limit 30 sends / user / rolling 24h, burst 5 /
// 10 minute; confirmation freshness window 10 minute.
//
// DISCLOSED IMPLEMENTATION CHOICES (spec me silent points):
//   (a) Licence-gate DB error par FAIL-CLOSED reject hai — brain-proxy
//       v5 fail-open karta hai, par send par money/impersonation ka
//       stake hai, isliye yahan fail-open nahi.
//   (b) Business-rule rejections (flag/template/confirmation) HTTP 200
//       + { ok:false } par aati hain (usage-count ke soft-failure
//       pattern jaisa; app `ok` flag padhta hai). Auth 401/403,
//       validation 400, free-limit/rate-limit 429 hain.
//   (c) Flag row na mile to ON mana jata hai (domain fail-open,
//       feature-flags function jaisa); flag query ERROR par reject.
//   (d) Graph API version v21.0 (spec version pin nahi karta).
//   (e) Wahi idempotency key dobara aaye to pehle wale attempt ka
//       result milega — chahe wo rejection hi kyun na ho. Naya send
//       chahiye to nayi key (+ zaroorat par nayi confirmation).
//   (f) Config-missing par confirmation consume NAHI hoti (koi send
//       attempt hua hi nahi); provider-fail par confirmation consumed
//       ho jati hai (retry nayi confirmation se — spec §2.8).
//
// PRIVACY (spec §2.7): poora recipient number, template params ki
// values, aur token KABHI log/record/response me nahi aate. Records
// me sirf recipient SHA-256 hash + aakhri 4 ank hote hain; params se
// sirf count darj hota hai (values nahi). Provider request me number
// bhejna hi padta hai (API ko number chahiye) — wo log nahi hai.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// Domain pattern (S2-1): CORS preflight — OPTIONS par 200 + wahi
// headers har response par; JWT/gates waise hi rahenge.
const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

// Spec §2.6/§2.5 DRAFT-PROPOSED constants (parent-confirm pending).
const RATE_LIMIT_24H = 30;
const BURST_LIMIT_10M = 5;
const CONFIRM_FRESHNESS_MS = 10 * 60 * 1000;

const E164_RE = /^\+[1-9]\d{1,14}$/;
const TEMPLATE_NAME_RE = /^[a-z0-9_]{1,512}$/;
const LANGUAGE_RE = /^[a-z]{2,3}(_[A-Z]{2})?$/;
const UUID_RE =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

type SupaClient = ReturnType<typeof createClient>;

async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(text),
  );
  return [...new Uint8Array(digest)]
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

// template_params -> Meta body parameters. Object ho to values insertion
// order me, array ho to usi order me. Values sirf text ban sakti hain.
// Galat shape / zyada params / lambi value par null (400).
function bodyParameters(
  params: unknown,
): { type: "text"; text: string }[] | null {
  if (params === undefined || params === null) return [];
  let values: unknown[];
  if (Array.isArray(params)) {
    values = params;
  } else if (typeof params === "object") {
    values = Object.values(params as Record<string, unknown>);
  } else {
    return null;
  }
  if (values.length > 20) return null;
  const out: { type: "text"; text: string }[] = [];
  for (const v of values) {
    if (typeof v !== "string" && typeof v !== "number" && typeof v !== "boolean") {
      return null;
    }
    const t = String(v);
    if (t.length > 1024) return null;
    out.push({ type: "text", text: t });
  }
  return out;
}

Deno.serve(async (req) => {
  const json = (obj: unknown, status = 200) =>
    new Response(JSON.stringify(obj), {
      status,
      headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
    });
  try {
    if (req.method === "OPTIONS") {
      return new Response(null, { status: 200, headers: { ...CORS_HEADERS } });
    }
    if (req.method !== "POST") {
      return new Response("POST only", { status: 405, headers: { ...CORS_HEADERS } });
    }

    const body: unknown = await req.json().catch(() => null);
    // Bulk ka raasta band: array request seedha reject (spec §2.1).
    if (Array.isArray(body)) {
      return json({ ok: false, error: "array recipients not allowed", error_code: "array_not_allowed" }, 400);
    }
    if (!body || typeof body !== "object") {
      return json({ ok: false, error: "invalid request body", error_code: "invalid_request" }, 400);
    }
    const b = body as Record<string, unknown>;

    // JWT gate (spec §2.3 — brain-proxy/licence-verify pattern):
    // Bearer user JWT chahiye; na mile to 401.
    const url = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const authHeader = req.headers.get("Authorization") || "";
    if (!authHeader.startsWith("Bearer ")) {
      return json({ ok: false, error: "unauthorized", error_code: "unauthorized" }, 401);
    }
    const anon = createClient(url, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await anon.auth.getUser();
    if (userErr || !userData?.user) {
      return json({ ok: false, error: "unauthorized", error_code: "unauthorized" }, 401);
    }
    const jwtUserId = userData.user.id;
    const svc: SupaClient = createClient(
      url,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    // Caller-match: body user_id JWT user se match na kare to 403.
    if (typeof b.user_id !== "string" || !b.user_id) {
      return json({ ok: false, error: "user_id required", error_code: "invalid_request" }, 400);
    }
    const userId = b.user_id;
    if (userId !== jwtUserId) {
      return json({ ok: false, error: "user_id does not match token", error_code: "user_mismatch" }, 403);
    }

    // Field validation (spec §2.2).
    if (
      typeof b.template_name !== "string" ||
      !TEMPLATE_NAME_RE.test(b.template_name)
    ) {
      return json({ ok: false, error: "invalid template_name", error_code: "invalid_request" }, 400);
    }
    if (
      typeof b.template_language !== "string" ||
      !LANGUAGE_RE.test(b.template_language)
    ) {
      return json({ ok: false, error: "invalid template_language", error_code: "invalid_request" }, 400);
    }
    if (
      typeof b.recipient_e164 !== "string" ||
      !E164_RE.test(b.recipient_e164)
    ) {
      return json({ ok: false, error: "invalid recipient_e164", error_code: "invalid_request" }, 400);
    }
    if (
      typeof b.confirmation_id !== "string" ||
      !UUID_RE.test(b.confirmation_id)
    ) {
      return json({ ok: false, error: "invalid confirmation_id", error_code: "invalid_request" }, 400);
    }
    if (
      typeof b.idempotency_key !== "string" ||
      !UUID_RE.test(b.idempotency_key)
    ) {
      return json({ ok: false, error: "invalid idempotency_key", error_code: "invalid_request" }, 400);
    }
    const params = bodyParameters(b.template_params);
    if (params === null) {
      return json({ ok: false, error: "invalid template_params", error_code: "invalid_request" }, 400);
    }
    const templateName = b.template_name;
    const templateLanguage = b.template_language;
    const recipient = b.recipient_e164;
    const confirmationId = b.confirmation_id;
    const idempotencyKey = b.idempotency_key;
    const recipientHash = await sha256Hex(recipient);
    const recipientLast4 = recipient.slice(-4);

    // Audit record insert (har attempt darj hoga — spec §2.7).
    // Poora number / params values kabhi record me nahi.
    async function recordAttempt(
      status: "sent" | "failed" | "rejected",
      extra: Record<string, unknown>,
    ): Promise<string | null> {
      try {
        const { data, error } = await svc
          .from("whatsapp_template_sends")
          .insert({
            user_id: userId,
            template_name: templateName,
            template_language: templateLanguage,
            recipient_hash: recipientHash,
            recipient_last4: recipientLast4,
            confirmation_id: confirmationId,
            authorization_id: extra.authorization_id ?? null,
            idempotency_key: idempotencyKey,
            provider_message_id: extra.provider_message_id ?? null,
            status,
            error_code: extra.error_code ?? null,
          })
          .select("id")
          .single();
        if (error || !data) return null;
        return (data as { id: string }).id;
      } catch (_e) {
        return null;
      }
    }
    // Rejection: record + { ok:false } response.
    async function reject(
      errorCode: string,
      message: string,
      httpStatus: number,
      authorizationId: string | null = null,
    ) {
      const sendId = await recordAttempt("rejected", {
        error_code: errorCode,
        authorization_id: authorizationId,
      });
      return json(
        { ok: false, error: message, error_code: errorCode, send_id: sendId },
        httpStatus,
      );
    }

    // Flag gate (spec §2.3): whatsapp flag OFF ho to send se pehle
    // reject, provider tak call nahi. Global base + per-user override
    // (feature-flags function jaisa); row na mile to ON (fail-open);
    // query ERROR par fail-closed reject.
    let flagOn = true;
    try {
      const { data: flagRows, error: flagErr } = await svc
        .from("feature_flags")
        .select("key,enabled,scope")
        .eq("key", "whatsapp");
      if (flagErr) throw flagErr;
      for (const row of flagRows || []) {
        if (row.scope === "global") flagOn = !!row.enabled;
      }
      for (const row of flagRows || []) {
        if (row.scope === userId) flagOn = !!row.enabled;
      }
    } catch (_e) {
      return reject("store_unavailable", "flag store unavailable", 200);
    }
    if (!flagOn) {
      return reject("flag_off", "whatsapp feature flag OFF hai — send band hai", 200);
    }

    // Licence gate (spec §2.4 — brain-proxy v5 jaisa): Pro active +
    // unexpired -> allow; Free -> aaj ka talk usage < 600 sec ho to
    // allow, nahi to 429. Gate sirf padhta hai, increment nahi karta.
    // DEVIATION (disclosed): DB error par FAIL-CLOSED reject hai —
    // brain-proxy fail-open karta hai, par send par money stake hai.
    let licenceOk: boolean;
    try {
      const { data: rows, error: licErr } = await svc
        .from("licences")
        .select("plan_id,status,expires_at")
        .eq("user_id", userId)
        .eq("status", "active");
      if (licErr) throw licErr;
      const nowMs = Date.now();
      const hasPro = (rows || []).some((r) =>
        r.plan_id === "pro" &&
        (!r.expires_at || new Date(r.expires_at).getTime() >= nowMs));
      if (hasPro) {
        licenceOk = true;
      } else {
        const day = new Date().toISOString().slice(0, 10);
        const { data: u, error: uErr } = await svc
          .from("usage")
          .select("talk_seconds")
          .eq("user_id", userId)
          .eq("day", day)
          .maybeSingle();
        if (uErr) throw uErr;
        licenceOk = (u?.talk_seconds || 0) < 600;
      }
    } catch (_e) {
      return reject("store_unavailable", "licence store unavailable", 200);
    }
    if (!licenceOk) {
      return reject(
        "free_limit",
        "Free limit poora ho gaya hai (600 sec/day) — Pro lene par unlimited milega.",
        429,
      );
    }

    // Template-authorization gate (spec §2.5): server par active
    // record maujood hona chahiye — app ka daava kaafi nahi.
    let authorizationId: string | null = null;
    try {
      const { data: authRow, error: authErr } = await svc
        .from("whatsapp_template_authorizations")
        .select("id")
        .eq("user_id", userId)
        .eq("template_name", templateName)
        .eq("template_language", templateLanguage)
        .eq("status", "active")
        .order("authorized_at", { ascending: false })
        .limit(1)
        .maybeSingle();
      if (authErr) throw authErr;
      if (!authRow) {
        return reject(
          "template_not_authorized",
          "template authorize nahi hai — pehle app me is template ko authorize karo",
          200,
        );
      }
      authorizationId = (authRow as { id: string }).id;
    } catch (_e) {
      return reject("store_unavailable", "authorization store unavailable", 200);
    }

    // Idempotency gate (spec §2.6): wahi key dobara aaye to provider
    // ko dobara call NAHI — pehle wale attempt ka result wapas.
    try {
      const { data: prev, error: prevErr } = await svc
        .from("whatsapp_template_sends")
        .select("id,provider_message_id,status,error_code")
        .eq("user_id", userId)
        .eq("idempotency_key", idempotencyKey)
        .order("created_at", { ascending: false })
        .limit(1)
        .maybeSingle();
      if (prevErr) throw prevErr;
      if (prev) {
        const p = prev as {
          id: string;
          provider_message_id: string | null;
          status: string;
          error_code: string | null;
        };
        if (p.status === "sent") {
          return json({
            ok: true,
            send_id: p.id,
            provider_message_id: p.provider_message_id,
            status: "sent",
          });
        }
        return json({
          ok: false,
          error: p.error_code || "previous attempt failed",
          error_code: p.error_code || "previous_failed",
          send_id: p.id,
        });
      }
    } catch (_e) {
      return reject("store_unavailable", "send store unavailable", 200);
    }

    // Per-send confirmation gate (spec §2.5): confirmation isi user
    // ki ho, isi template + isi recipient ke liye ho, unused ho,
    // aur freshness window (DRAFT-PROPOSED 10 min) ke andar ho.
    try {
      const { data: conf, error: confErr } = await svc
        .from("whatsapp_send_confirmations")
        .select("id,template_name,template_language,recipient_hash,confirmed_at,consumed")
        .eq("id", confirmationId)
        .eq("user_id", userId)
        .maybeSingle();
      if (confErr) throw confErr;
      if (!conf) {
        return reject("confirmation_not_found", "confirmation record nahi mila", 200, authorizationId);
      }
      const c = conf as {
        template_name: string;
        template_language: string;
        recipient_hash: string;
        confirmed_at: string;
        consumed: boolean;
      };
      if (
        c.template_name !== templateName ||
        c.template_language !== templateLanguage ||
        c.recipient_hash !== recipientHash
      ) {
        return reject(
          "confirmation_mismatch",
          "confirmation is template/recipient se match nahi karti",
          200,
          authorizationId,
        );
      }
      if (c.consumed) {
        return reject(
          "confirmation_consumed",
          "confirmation pehle use ho chuki hai — dobara bhejna ho to nayi confirmation chahiye",
          200,
          authorizationId,
        );
      }
      const ageMs = Date.now() - new Date(c.confirmed_at).getTime();
      if (!Number.isFinite(ageMs) || ageMs < 0 || ageMs > CONFIRM_FRESHNESS_MS) {
        return reject(
          "confirmation_expired",
          "confirmation purani ho gayi hai (10 minute window) — nayi confirmation chahiye",
          200,
          authorizationId,
        );
      }
    } catch (_e) {
      return reject("store_unavailable", "confirmation store unavailable", 200, authorizationId);
    }

    // Rate-limit gate (spec §2.6 DRAFT-PROPOSED): 30 / rolling 24h,
    // burst 5 / 10 minute — sirf 'sent' gine jate hain.
    try {
      const now = Date.now();
      const dayAgo = new Date(now - 24 * 3600 * 1000).toISOString();
      const tenMinAgo = new Date(now - 10 * 60 * 1000).toISOString();
      const { data: dayRows, error: dayErr } = await svc
        .from("whatsapp_template_sends")
        .select("id")
        .eq("user_id", userId)
        .eq("status", "sent")
        .gte("created_at", dayAgo);
      if (dayErr) throw dayErr;
      if ((dayRows || []).length >= RATE_LIMIT_24H) {
        return reject("rate_limited", "limit poori ho gayi hai (30 sends / 24 ghante)", 429, authorizationId);
      }
      const { data: burstRows, error: burstErr } = await svc
        .from("whatsapp_template_sends")
        .select("id")
        .eq("user_id", userId)
        .eq("status", "sent")
        .gte("created_at", tenMinAgo);
      if (burstErr) throw burstErr;
      if ((burstRows || []).length >= BURST_LIMIT_10M) {
        return reject("burst_limited", "burst limit poori ho gayi hai (5 sends / 10 minute)", 429, authorizationId);
      }
    } catch (_e) {
      return reject("store_unavailable", "send store unavailable", 200, authorizationId);
    }

    // Config gate (spec §2.8): token/config missing ya galat ho to
    // fail-closed — koi send nahi, saaf error, token value kabhi
    // error me nahi. Confirmation yahan consume nahi hoti (koi send
    // attempt hua hi nahi — config theek hone par retry ho sakta hai).
    const waToken = Deno.env.get("WHATSAPP_BUSINESS_TOKEN") || "";
    const waPhoneId = Deno.env.get("WHATSAPP_PHONE_NUMBER_ID") || "";
    if (!waToken || !waPhoneId) {
      const sendId = await recordAttempt("failed", {
        error_code: "config_missing",
        authorization_id: authorizationId,
      });
      return json(
        { ok: false, error: "server not configured", error_code: "config_missing", send_id: sendId },
      );
    }

    // Single-use consume (spec §2.5): provider call se pehle consumed
    // mark karo — race par 0 rows mile to dobara use mana jayega.
    try {
      const { data: upd, error: updErr } = await svc
        .from("whatsapp_send_confirmations")
        .update({ consumed: true })
        .eq("id", confirmationId)
        .eq("consumed", false)
        .select("id");
      if (updErr) throw updErr;
      if (!upd || (upd as unknown[]).length === 0) {
        return reject(
          "confirmation_consumed",
          "confirmation pehle use ho chuki hai — dobara bhejna ho to nayi confirmation chahiye",
          200,
          authorizationId,
        );
      }
    } catch (_e) {
      return reject("store_unavailable", "confirmation store unavailable", 200, authorizationId);
    }

    // Provider call — Meta WhatsApp Business Cloud API. Number sirf
    // is API request me jata hai (API ko chahiye); log/record me nahi.
    const templatePayload: Record<string, unknown> = {
      name: templateName,
      language: { code: templateLanguage },
    };
    if (params.length > 0) {
      templatePayload.components = [{ type: "body", parameters: params }];
    }
    let providerMessageId: string | null = null;
    let providerOk = false;
    try {
      const res = await fetch(
        `https://graph.facebook.com/v21.0/${waPhoneId}/messages`,
        {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${waToken}`,
            "Content-Type": "application/json",
          },
          signal: AbortSignal.timeout(30000),
          body: JSON.stringify({
            messaging_product: "whatsapp",
            to: recipient,
            type: "template",
            template: templatePayload,
          }),
        },
      );
      const pj = await res.json().catch(() => ({})) as Record<string, unknown>;
      const mid = (pj.messages as { id?: unknown }[] | undefined)?.[0]?.id;
      if (res.ok && typeof mid === "string" && mid) {
        providerOk = true;
        providerMessageId = mid;
      }
    } catch (_e) {
      // Provider fail: neeche 'failed' record + imaandaar report.
      // Chhupchaap retry kabhi nahi (spec §2.8).
      providerOk = false;
    }
    if (!providerOk) {
      const sendId = await recordAttempt("failed", {
        error_code: "provider_error",
        authorization_id: authorizationId,
      });
      return json(
        { ok: false, error: "provider_error", error_code: "provider_error", send_id: sendId },
      );
    }
    const sendId = await recordAttempt("sent", {
      provider_message_id: providerMessageId,
      authorization_id: authorizationId,
    });
    return json({
      ok: true,
      send_id: sendId,
      provider_message_id: providerMessageId,
      status: "sent",
    });
  } catch (_e) {
    // Top-level fail: koi detail, number, token, ya params kabhi nahi.
    return new Response(
      JSON.stringify({ ok: false, error: "internal_error", error_code: "internal_error" }),
      { status: 500, headers: { ...CORS_HEADERS, "Content-Type": "application/json" } },
    );
  }
});
