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
- G19 fix: ShizukuBridge `newProcess` private error → grant transact path → fix2 (getTransactionCode 2-arg, AAR-verified) push ho gaya (bd1cda18), CI SUCCESS (run 40737122451). Deepgram key Supabase secret me set hai (2026-10-28 ~23:21 IST, user ki di hui key se) — proxy deploy Phase 4 me.
- GitHub secrets: SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY — HO GAYA 23:03 IST (dono naam GitHub secrets page par listed, API se verify).
- flora-apk bucket (public) — HO GAYA 23:03 IST (public bucket ban gaya, PUBLIC badge ke saath).
- Deepgram key + Supabase Edge Functions secret DEEPGRAM_API_KEY — HO GAYA 23:21 IST (secret listed, user ki di hui key se).
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

## Phase 7 — Single final deploy — DONE 2026-10-29 ~01:30 IST
- v2.22.0-rebuild (versionCode 44) APK: CI run 40752222457 se final APK
  (112,590,325 bytes, sha256 6987e62590b09396b5474ea2256ce9d8a0d27dbe5c73150d5ebaa788729a5e09,
  package com.flora.app) — artifact se download + aapt2 badging se verify.
- DELIVERABLE CHANGE (verified 2026-10-28 ~23:47 IST):
  flora-apk Supabase bucket route DEAD hai — free plan par file-size limit
  50MB fixed hai (per-bucket aur global dono raise karne ki koshish fail;
  Pro bina user approval ke nahi liya jayega) aur APK ~112MB hai, isliye
  bucket me flora-latest.apk kabhi upload nahi hoga. Download page ko
  bucket URL par flip MAT karna.
- ROUTE CHANGE (user decision 2026-10-29 ~01:14 IST): GitHub Release asset
  ka raasta bhi DROP — repo raj3kk/flora PRIVATE hai, isliye uska release
  asset public download nahi hoga. User ne SwissTransfer chuna; repo
  private hi rahega, Supabase Pro is raaste ke liye nahi liya jayega.
- Final link (SwissTransfer, user-approved sender email flipify.com@gmail.com,
  email-verification code se confirm, koi account nahi, kuch pay nahi):
  https://www.swisstransfer.com/dl/01a18414-976d-71f9-b762-b18a70bbbd5b
  (flora-v2.22.0.apk, 107.4 MB, 250 max downloads, no password).
  Link 13 Nov 2026 tak active hai (15 din); uske baad sender email par
  extension invite aayega (+15 din). Download page isi link par flip ho
  gaya hai (commit 11cd3ac0); purana "Direct download (aa raha hai)" note
  hata diya gaya (wo Supabase auto-upload raasta dead hai).
- REFRESH PLAN (har release + har expiry par): SwissTransfer link permanent
  nahi hai. Har naye PASS build par naya upload + page update hoga; kisi
  release ke bina bhi expiry (13 Nov 2026, phir extension ke baad) se
  pehle extension confirm karni hai, warna dobara upload + page update.
  Owner: main agent (user ko expiry se pehle batana hai).
- Live verify (2026-10-29 ~01:30 IST): / 200, /login 200, /download 200
  (v2.22 + naya link dikhta hai), SwissTransfer link 200. APK download +
  install check Phase 8 phone phase me hoga.
Exit gate: live routes 200 + download page par latest version. Beech ka koi
version release nahi hua (single-deploy order barkarar).
Owner: main agent (user ka kaam: sirf sender email approval + verification code).
NOTE (baaki): android.yml me dead flora-apk upload step (har build par 413,
continue-on-error se chupchaap fail) abhi bhi maujood hai — use web UI se
hatana baaki hai (API se workflow file edit nahi hoti).

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

## Maya-gap closure — CEO order 2026-10-28 ~23:08 IST
- User order: Flora Maya se kisi bhi cheez me peeche nahi rehni chahiye; har gap par research + audit + plan me add + implement, one by one, audit ke saath, bina ruke. In-app browser Maya jaisa Flora ke ANDAR bhi chahiye — fully functional, automated, production-ready.
- Gap → phase mapping (10 points jin par Maya aage thi):
  1. Asli awaaz (3 TTS lanes) → Phase 3 (Deepgram style voice + wiring; system fallback).
  2. STT (3 lanes) → Phase 3.
  3. Wakeword (5 models + VAD + turn/addressee) → Phase 3 (6 maujood clips se train; turn/addressee zaroorat ho to Phase 5 me).
  4. Guardian model maturity → Phase 3 (threshold phone validation Phase 8 me).
  5. Screen breadth (54 routes / 45 voice destinations) → Phase 5 + route-parity check (neeche).
  6. In-app browser → Phase 5 G7 (full scope neeche).
  7. Lock screens → Phase 5 G8 (PIN-escapable).
  8. Home relay server → Phase 5 G10.
  9. Licence system live → Phase 4 + 8.
  10. Shipped stability → Phase 6 + 7 + 8.
- G7 in-app browser — FULL SCOPE (Maya parity + Flora-ahead):
  - BrowserScreen (nayi screen, Screen enum + menu + navigation): WebView, JS on (modern sites ke liye), DOM storage on, file access OFF, universal file access OFF, mixed content NEVER, Safe Browsing on, SSL error par kabhi chupchaap proceed nahi (warning dikhao, user ka faisla).
  - Navigation: back/forward/reload, URL bar (type karke jao), progress bar, error page, is session ki history.
  - Voice automation: "browser kholo", "browser me <site/url> kholo", "browser me wapas jao / aage jao / reload karo", "ye page padh kar sunao / is page par kya hai" (JS se title + body text nikal kar sunao), "pehla/doosra link kholo" (JS se links list).
  - Downloads: DownloadManager se, har download se pehle user confirmation (kabhi silent download nahi).
  - Password: WebView me password save nahi hoga; login user khud ek baar karega, password kabhi bola/store nahi hoga.
  - Privacy: browser history sirf phone par, server par nahi.
  - Exit gate: per-item audit + CI green + phone test Phase 8 (site kholo, wapas/aage, padh kar sunao, link kholo).
- Route-parity check (Phase 5, G7 ke saath): Maya ke 45 voice destinations ki list ke saamne Flora ke branches milao; jo destination Flora me nahi hai us par ya to branch banao ya drop ki wajah likh do. Target: Flora ke voice destinations >= Maya ke, koi destination bina wajah missing nahi.
- Route-parity COUNT (sweep 2026-10-29, gina hua): FloraApp.kt me 122 unique voice phrases (exact + prefix, dedupe karke) + 26-screen "kholo" map + BasicTalk ki 13 small-talk branches; Screen enum ke 28/28 screens voice se reachable hain (kholo map ke 26 + "yaadein kholo" → MEMORIES + "camera dekho" → CAMERA_LOOK + "browser kholo" → BROWSER). Ginti ke hisaab se Flora ke voice destinations Maya ke 45 se zyada hain — target number se poora hai; phone par behaviour test Phase 8 me baaki hai.
- Flora aage hai (defend, Phase 6 regression check): call UI (Maya dialer nahi thi), encrypted on-device memory, camera vision, keys UI/checks, live-update voice — inme regression nahi aana chahiye.
- Explicitly out of scope (CEO decision, chhupaya nahi): image generation (Maya Pro-Max gated; Flora me iska koi user order nahi) — user order aane par alag se dekha jayega.
- Execution: builder ek-ek phase sequentially karta hai (Phase 1 → 2 → 3 → 5 → 6 → 7-prep), har phase par audit + CI green tabhi agla phase. Phase 4 dashboard deploy + Phase 8 phone phase is builder ke scope se bahar hain (dashboard main agent ke browser se, phone user se) — unke liye code-ready state chhodni hai, daava nahi karna.
