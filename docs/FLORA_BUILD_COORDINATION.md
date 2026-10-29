# Flora build coordination — kaun kya kar raha hai (2026-10-29 ~09:57 IST)

User order: parallel kaam, koi rukawat nahi, koi conflict nahi, sab ko pata ho kon
kya kar raha hai, koi duplicate/gadbadi nahi. Ye file isi ka map hai. Har builder
apne tree ke bahar haath nahi lagayega.

## Active builders

| Builder | Owned tree | Kaam | Status |
|---|---|---|---|
| Phase 8 app builder | app/ + supabase/functions/home-sync + supabase/sql/home_sync_table.sql | Kokoro neural voice, feature-flag client, G17 pinning, home-sync JWT, app-link flow fix | RUNNING (09:34 se) |
| LFMS server builder | supabase/functions/ (home-sync CHHOD kar) + supabase/sql/licence_system.sql + web/admin.html (Licences + Plan features) + docs audit log | Licence-verify v2, brain-proxy v5 licence gate, tts-proxy v3 gate, admin-licences, admin-plan-features, admin-approve minimal | RUNNING (09:57 se) |
| LFMS audit | READ-ONLY → docs/FLORA_LICENCE_SYSTEM_AUDIT.md | Licence system ka poora audit (tables/functions/website/app/panel/security) | RUNNING (09:57 se) |
| Panel phase 1 + 2 | — | DONE (4609ed7d, 6e36b72d; CI green) | DONE |
| Wave B app LFMS builder | app/ (licence fill screen, masked reveal+copy, plan-features client enforcement, revoked messages) | Phase 8 land hone ke BAAD spawn hoga (app/ takrao se bachne ke liye) | PENDING on Phase 8 |

## Contracts (sab isi par banenge — badla to main agent batayega)

- Feature flags: POST functions/v1/feature-flags, anon Bearer, {flags:{talk, calls,
  sms, whatsapp, browser, macros, image_gen, screen_vision, camera, overlay_mic,
  wakeword, home, routines, form_fill}}; missing key → true (fail-open).
- Licence key: FLORA-XXXX-XXXX-XXXX, server generate, DB unique, collision retry.
- licence-verify v2: {valid, status: active/revoked/expired/superseded/
  bound_mismatch, plan, features[], expires_at, bound_device_short, reason?}.
- Rules: ek licence ek time me ek device (first claim bind; 15-DAY TRANSFER RULE
  barkaraar — 15 din se pehle nayi device par device_locked, 15 din poore par
  transfer, purana device free; admin rebind = wait ka bypass, audit ke saath);
  ek user ki ek active licence (nayi purani ko 'superseded' karti hai); har licence
  unique; revoke sirf admin (+ reason + audit); key masked by default (last 4),
  click par reveal + copy (app + panel). KEY ADDITIVE HAI: keyless rows
  (payment-approved, user+device verify) bina key ke waise hi chalengi.
- Brain/TTS gate: Pro valid → chalo; Free → usage_today_seconds < 600 → chalo,
  nahi to free_limit (429); counters maujooda usage system ke reuse honge.

## Deploy order (EK session, Phase 8 ke baad)

licence_system.sql → panel tables (phase1/2, agar baaki) → functions (licence-
verify v2, brain-proxy v5, tts-proxy v3, admin-licences, admin-plan-features,
admin-approve minimal, phase1/2 set, send-push local, home-sync) → schema checks
(licences columns, plans/settings columns, licences.device_id nullable) → dead CI
step web UI removal → push-token-sync JWT gate update.

## Rules

- Apne tree ke bahar mat jao. Doosre builder ki file chhedni ho to main agent ko
  bolo, khud mat karo.
- Koi secret/key/password read/push nahi. Koi approve/reject test nahi.
- KABHI [skip ci] nahi. .github/workflows/ API se nahi (web UI only).
- CI green + audit log entry ke bina koi builder DONE nahi.
