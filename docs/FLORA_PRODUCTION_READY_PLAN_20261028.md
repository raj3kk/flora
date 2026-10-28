# Flora — Production-Ready Plan: 100% tak (2026-10-28)

## 100% ka matlab (user orders ke saath)
- 100% = neeche ke saare committed scope items code-complete + CI-green + R3 audit 0 + ek final deploy + final phone phase sign-off.
- Isme NAHI hai (user-deferred ya faisle se bahar):
  - Grievance officer — user ne kaha "baad me hoga" (post-launch item, 100% ko nahi rokega).
  - Neural voice engine (Kokoro/Piper) — user ne Lily FINAL ki hai; neural option drop hua.
  - SIM call me TTS inject (Android public API se reliably possible nahi) — speaker loop hi product hai.
  - WhatsApp call API (koi official API nahi) — accessibility swipe-answer hi max hai.
  - WhatsApp silent auto-reply (CEO decision: kabhi chhupke nahi) — sirf read + user-confirmed wa.me send.
  - Play Store listing — sideload hi design hai (signing key lost).
- Current state (estimate, imaandari se): code + CI level par ~35%. Phone-verified hissa v2.20 tak ka hai; v2.22 ka phone test 0% (single-deploy order se).

## Phase 0 — Unblock (aaj raat, chal raha hai) — ~60% hua
Bacha hai:
- G19 fix: ShizukuBridge `newProcess` private error → grant transact path → fix2 (getTransactionCode 2-arg, AAR-verified) push ho gaya, CI result pending (fix agent ka kaam poora, main agent ne fix2 lagaya).
- GitHub secrets: SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY — HO GAYA 23:03 IST (dono naam GitHub secrets page par listed, API se verify).
- flora-apk bucket (public) — HO GAYA 23:03 IST (public bucket ban gaya, PUBLIC badge ke saath).
- Deepgram key khud nikalna + Supabase Edge Functions secret DEEPGRAM_API_KEY (browser task chal raha hai).
Exit gate: G19 head par CI SUCCESS; dono secrets listed; bucket maujood; DEEPGRAM_API_KEY listed.
Owner: builder + browser (user ka koi kaam nahi).

## Phase 1 — Core automation poora (chunk 2) — ~10% hua
Bacha hai:
- Overlay mic button service (SYSTEM_ALERT_WINDOW, har screen par).
- G3 full: VoiceInteractionService (default assistant asli me).
- "Code likho" branch (G15 ka baaki hissa).
- Hisaab branch ("kya kiya, kya karna hai, cheezein kahan hain").
- Pending-question auto-resume (permission Allow ke baad bina dobara bole jawab).
- LicenceClient.refreshSession() wiring app start par.
Exit gate: har item par per-item audit (declaration order, balance, permission, privacy) + CI SUCCESS + audit log entry.
Owner: builder.

## Phase 2 — Memory + encryption poora (G18) — ~25% hua
Bacha hai: journal, docs, routines, jobs ko encrypted store par lana (memories pehle se encrypted hain).
Exit gate: koi plain-text user data file/server/backup par nahi; audit log entry.
Owner: builder.

## Phase 3 — Voice maturity — ~40% hua
Bacha hai:
- Neural wakeword: 6 maujood clips (New_recording_28–33) se train → TFLite model → app me integrate + threshold (prototype MFCC pehle se hai).
- Deepgram style voice (G1): key aane ke baad Lily-style Deepgram voice chunna + app wiring.
- STT lanes wiring: Deepgram REST + streaming barge-in ko system STT fallback ke saath jodna (code pehle se hai, wiring + key baaki).
- Guardian threshold phone par validate karna (final phone phase me).
Exit gate: code + CI green; asli phone test Phase 8 me.
Owner: builder (key browser se, test user ke phone par).

## Phase 4 — Server + proxies live (S3) — ~30% hua
Bacha hai:
- Deploy (Supabase dashboard code editor se — GitHub push se deploy nahi hota): admin-approve (ii, website-path), licence-verify (NULL-claim), brain-proxy step 2, tts-proxy.
- Proxy keys self-service (Deepgram ke baad wahi tareeka): GEMINI_API_KEY + GROQ_API_KEY Supabase secrets me.
- App wiring: brain-proxy step 2 + tts-proxy endpoints se jodna (per-user key passthrough, koi global mixing nahi).
- G17 domain pinning: app sirf apne domain se baat kare.
- Redirect URL safai: Supabase Auth config se purana deploy URL hatana.
- Agar licences.device_id NOT NULL nikla to ek dashboard SQL: device_id DROP NOT NULL (sirf tab, jab NULL insert fail ho).
Exit gate: live checks — 7/7 functions no-header POST par 401 (auth gate), version endpoint 200, app Settings se key check sahi, licence claim ka live test (final phase me).
Owner: builder + browser (dashboard user ke login se).

## Phase 5 — Maya parity breadth — ~20% hua
Bacha hai (har item: code + CI + audit):
- G7 in-app browser; G9 live wallpaper; G10 Home sync server half; G12 sub-agent orchestrator (job runner pehle se hai); G15 code loop poora (website starter + code-likho + publish — publish G19 me hai); G16 Chrome/GitHub/Vercel voice ops (login user khud ek baar karega, password kabhi bola/store nahi hoga); G8 lock screen (sirf PIN-escapable, user kabhi lock me nahi fansega); G11 Termux bridge (har command par user consent, kabhi silent nahi); G13 PC link; G14 Google link sign-in (app link ke liye password typing ki jagah).
- WhatsApp: read-only + wa.me user-confirmed send (G19 swipe-answer ke upar).
- Har optional item ka CEO resolution: ya to banao, ya drop karne ki wajah likh do — bina wajah koi item adhura nahi chhutega.
Exit gate: per-item audit + CI green; drop hue items ki wajah audit log me.
Owner: builder (login/consent user ke haath me, final phase me).

## Phase 6 — R3 final audit → 0 issues — 0% hua
Bacha hai: poori layer audit round 2 (audit → fix → audit jab tak 0 na ho): code audit, permission/host scan (koi hardcoded key nahi, koi creator copy nahi), privacy text (server par jaane wale prompts ka disclosure), licence/payment flow re-check, download page + website re-verify.
Exit gate: audit log me 0 open issues. Iske bina final deploy nahi hoga.
Owner: builder (+ watcher har 5 min par check karega).

## Phase 7 — Single final deploy — 0% hua
Bacha hai (sab ek saath, ek hi baar):
- v2.22.0-rebuild (versionCode 44) APK: CI build → flora-apk bucket par flora-latest.apk upload (auto, secrets + bucket Phase 0 se).
- Download page flip: direct download link primary (SwissTransfer v2.20 tab tak fallback).
- Live verify: /, /api/app/version, /login, download URL — sab 200; APK download + install check.
Exit gate: live routes 200 + download page par latest version. Beech ka koi version release nahi (single-deploy order).
Owner: builder (koi user kaam nahi).

## Phase 8 — Final phone phase (user) — 0% hua
Bacha hai (sab kuch banne ke BAAD, user ke phone par — pehle nahi):
- App link: app Settings → Website account link (wahi email+password) → phir admin flag UPDATE (is par admin ruka hai).
- Roles taps: default assistant + default dialer (user khud set karega); battery-unrestricted + Vivo auto-start ON; Accessibility ON (+ Android 13 "Allow restricted settings" sideload ke liye).
- Keys: Gemini/Groq/Deepgram apni keys app Settings me (links + bina-save check pehle se bana hai).
- Phone test checklist: wake (Hey Flora), baat (Lily), camera dekhna, screen dekhna, call UI, website banao + publish, notification batao, jobs/routines, guardian enrol, licence proof + pehla website signup.
Exit gate: checklist ka har point phone par pass; jo fail ho wo Phase 6 loop me wapas (fix → audit → dobara test).
Owner: user (phone) + builder (fix).

## Order + dependency (koi phase skip nahi)
0 (unblock) → 1 + 2 (parallel ho sakte hain, alag files) → 3 → 4 → 5 → 6 (0 issues) → 7 (ek deploy) → 8 (phone sign-off).
Grievance post-launch me alag se hoga. Koi bhi phase "done" tabhi jab uska exit gate poora ho — CI in_progress/fail = NOT done.
