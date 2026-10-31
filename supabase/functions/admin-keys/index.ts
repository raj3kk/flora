// admin-keys — NEW, admin panel full build 2026-10-29 (repo copy;
// deploy dashboard editor se hoga; pehle admin_panel_tables.sql
// chalana zaroori hai, warna table-missing error aayega).
// Kaam: panel se provider keys set/status/delete karna.
// POST { action: "status" | "set" | "delete", provider, key_value? }
//   + Authorization: Bearer <user JWT>.
// Allowlist: gemini, groq, deepgram — iske bahar koi provider
// nahi. Table: provider_keys (service-role-only; RLS ON, koi
// policy nahi — client se koi read/write nahi).
// Key VALUE response me KABHI nahi aayegi, log me KABHI nahi
// aayegi. Status sirf booleans deta hai (present/missing).
// ENCRYPTION (CEO batch 2026-10-31, D-21 FIX): pehle set action
// key_value ko PLAINTEXT store karta tha (admin_panel_tables.sql
// ka security note ye khud kehta tha). Ab set AES-GCM encrypt
// karke store karta hai (format enc:v1:<base64(iv+ciphertext)>),
// key = SHA-256(PROVIDER_KEYS_ENCRYPTION_KEY dashboard secret).
// Secret na ho to set fail-closed 503 deta hai — plaintext write
// kabhi nahi. Status me providers_encrypted boolean map bhi hai
// (format batata hai, value kabhi nahi) — migration verify ke liye.
// Legacy plaintext rows migration ke dauran resolvers se waise hi
// padhi jayengi (deploy ke turant baad fallback tootne na paye);
// migration: secret set + deploy ke baad panel se har provider ki
// key dobara set karo, phir status me sab encrypted:true verify karo.
// Dashboard secrets (env) hamesha pehle jeetenge — DB key sirf
// fallback hai (brain-proxy v3 + tts-proxy v2 me resolution).
// S1-33 (Cycle 1 FIX): status response me farq saaf hai — ye
// status SIRF DB row (provider_keys) ka hai. Function Supabase
// dashboard secrets (env) ko dekh hi nahi sakta, isliye env key
// hone par bhi yahan "DB key: Missing" dikh sakta hai; panel
// isko env status mat samjhe (providers booleans + providers_label
// dono DB-row ke hain, env ka koi daava nahi).
// S1-32 (Cycle 1 FIX): har mutating action (set/delete) par
// admin_audit insert hota hai (admin-approve pattern: action naam
// + before/after JSON). Audit-json me key VALUE kabhi nahi —
// sirf provider + present/missing.
// 401: JWT nahi; 403: is_admin nahi; 400: galat action/provider.

// S4-2-free (Cycle 4 FIX): 500-path par DB error text (error.message)
// caller ko wapas NAHI milta — sirf generic message ("load failed" /
// "store failed") + detail console.error me (Edge logs me). 400s
// validation messages + success paths waise hi hain.

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

const PROVIDERS = ["gemini", "groq", "deepgram"];

// AES-GCM helpers (WebCrypto, Deno). Key material kabhi log/response me nahi.
async function encryptionKey(): Promise<CryptoKey | null> {
  const secret = Deno.env.get("PROVIDER_KEYS_ENCRYPTION_KEY") || "";
  if (!secret) return null;
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret));
  return crypto.subtle.importKey("raw", digest, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
}
async function encryptValue(plain: string): Promise<string | null> {
  const key = await encryptionKey();
  if (!key) return null;
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const cipher = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, new TextEncoder().encode(plain));
  const combined = new Uint8Array(iv.length + cipher.byteLength);
  combined.set(iv, 0);
  combined.set(new Uint8Array(cipher), iv.length);
  let bin = "";
  for (const b of combined) bin += String.fromCharCode(b);
  return "enc:v1:" + btoa(bin);
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
  const adminId = userData.user.id;
  const { data: adminRow } = await svc.from("users").select("is_admin").eq("id", adminId).maybeSingle();
  if (!adminRow?.is_admin) return json({ error: "admin only" }, 403);
  // S1-32 audit helper: audit fail se action nahi rukega; key
  // VALUE kabhi audit-json me nahi (sirf provider + present/missing).
  async function audit(actionName: string, afterJson: Record<string, unknown>) {
    try {
      await svc.from("admin_audit").insert({
        admin_user_id: adminId, action: actionName,
        target_user_id: null, after_json: afterJson,
      });
    } catch (_e) { /* audit fail se action nahi rukega */ }
  }
  const body = await req.json().catch(() => ({}));
  const { action, provider } = body;
  if (action === "status") {
    const { data, error } = await svc.from("provider_keys").select("provider, key_value");
    if (error) { console.error(error); return json({ error: "load failed" }, 500); }
    const rows = new Map((data || []).map((r: any) => [r.provider, r.key_value]));
    const providers: Record<string, boolean> = {};
    const providersLabel: Record<string, string> = {};
    const providersEncrypted: Record<string, boolean> = {};
    for (const p of PROVIDERS) {
      providers[p] = rows.has(p);
      providersLabel[p] = rows.has(p) ? "DB key: Present" : "DB key: Missing";
      providersEncrypted[p] = typeof rows.get(p) === "string" && (rows.get(p) as string).startsWith("enc:v1:");
    }
    return json({ ok: true, providers, providers_label: providersLabel, providers_encrypted: providersEncrypted,
      note: "Ye status sirf DB (provider_keys) row ka hai — Supabase dashboard secrets (env) is function ko dikhte hi nahi, isliye env key hone par bhi yahan 'DB key: Missing' dikh sakta hai. providers_encrypted sirf format batata hai, value kabhi nahi." });
  }
  if (!PROVIDERS.includes(provider)) {
    return json({ error: "provider must be gemini/groq/deepgram" }, 400);
  }
  if (action === "set") {
    const keyValue = typeof body.key_value === "string" ? body.key_value.trim() : "";
    if (!keyValue) return json({ error: "key_value required" }, 400);
    // Fail-closed: encryption secret na ho to plaintext write kabhi nahi.
    const stored = await encryptValue(keyValue);
    if (!stored) return json({ error: "encryption_not_configured", message: "PROVIDER_KEYS_ENCRYPTION_KEY dashboard secret set karo, phir key dobara daalo." }, 503);
    const { data: beforeRow } = await svc.from("provider_keys").select("provider").eq("provider", provider).maybeSingle();
    const { error } = await svc.from("provider_keys").upsert({
      provider, key_value: stored, updated_at: new Date().toISOString(),
    });
    if (error) { console.error(error); return json({ error: "store failed" }, 500); }
    await audit("key_set", { provider, before_present: !!beforeRow, after_present: true });
    return json({ ok: true });
  }
  if (action === "delete") {
    const { data: beforeRow } = await svc.from("provider_keys").select("provider").eq("provider", provider).maybeSingle();
    const { error } = await svc.from("provider_keys").delete().eq("provider", provider);
    if (error) { console.error(error); return json({ error: "store failed" }, 500); }
    await audit("key_deleted", { provider, before_present: !!beforeRow, after_present: false });
    return json({ ok: true });
  }
  return json({ error: "action must be status/set/delete" }, 400);
});
