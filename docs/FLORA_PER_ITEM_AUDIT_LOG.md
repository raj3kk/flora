# Per-item audit log (naya gate rule 2026-10-28 ke hisaab se)

## v1.6 Journal (commit 5824a6ac) — AUDIT PASS
- Code audit: JournalStore DataStore pattern (Memories jaisa), mood
  state use se pehle declare, brace/paren balance 0, koi permission
  nahi, koi server/share nahi.
- Build test: run 40620751322 completed success, artifact 54,314,991
  bytes.
- Test-plan entry: docs/FLORA_JOURNAL_TEST_ENTRY.md (6 steps).
- Fix: koi fix nahi chahiye tha.

## v1.7 Attachments (commit 3bac6363) — AUDIT FAIL, FIX CHAL RAHA HAI
- Code audit (pehli baar): order/braces/permissions/talk-limit/PDF
  note — sab theek laga, lekin ek type galti chhoot gayi.
- Build test: run 40621405269 completed FAILURE, step "Build debug
  APK", ek hi error: FloraApp.kt:531 "Type mismatch: inferred type
  is Uri? but Uri was expected".
- Root cause: `val uri = attachedUri` (nullable copy) ko
  openInputStream(uri) me diya; if-check copy ko non-null nahi banata.
- Fix: `val uri = attachedUri ?: return@Button` (commit 3c4f2cc9,
  sirf ye ek line; personas is commit me nahi).
- Fix build: run 40621805299 completed SUCCESS, artifact 54,320,297
  bytes — attachments audit AB POORA PASS hai (code + build + test
  entry). Iske baad personas push hua (v1.8).
- Test-plan entry: docs/FLORA_ATTACHMENTS_TEST_ENTRY.md (6 steps).
- Baaki: fix build ka result aana baaki hai; PASS hote hi attachments
  audit poora hoga, phir personas push hoga.
- Lesson: AGENTS.md me "nullable-state capture trap" darj hai.

## v1.8 Personas (commit 7c0359b7) — AUDIT PASS
- Code audit: PERSONA key + Settings selector + brain prompt prefix
  (generic + attachment text dono jagah); persona state launcher se
  pehle (782 < 786); brace/paren balance 0; koi creator persona copy
  nahi; default flora.
- Build test: run 40623043708 completed success, artifact 54,325,468
  bytes.
- Test-plan entry: docs/FLORA_PERSONAS_TEST_ENTRY.md (6 steps).

## v1.9 Themes + Orb — AUDIT PASS
- Code audit: THEME key + MaterialTheme wrap + warm/dark/light
  schemes (sirf colors); orb Canvas-only, busy par pulse, idle par
  static; theme states launcher se pehle (FloraApp 99, Settings 784
  < launcher 786); brace/paren balance 0.
- Build test: run 40624017595 completed success, artifact 54,331,069
  bytes. Pushed content GitHub par bhi verify hua (MaterialTheme,
  FloraOrb(busy), floraColorScheme, versionCode 20 sab maujood).
- Test-plan entry: docs/FLORA_THEMES_TEST_ENTRY.md (7 steps).

## v2.0 Study/Coding/Market — AUDIT PASS
- Code audit: study (DuckDuckGo, 5 sources + Source note),
  stockPrice (Yahoo Finance + stale-price note), websiteStarter
  (sirf local text, file save nahi); braces 0; koi key use nahi.
  Tavily lane is push me NAHI tha (v2.1 ke liye alag rakha gaya).
- Build test: run 40624515878 completed success, artifact 54,338,988
  bytes.
- Test-plan entry: docs/FLORA_TOOLS_TEST_ENTRY.md (6 steps).

## v2.1 Keys + rotation — push hua, CI result pending
- Code audit: rotation order Gemini 1, 2, Groq 1, 2; khali key
  skip; key values kabhi log nahi hoti; Tavily lane key ho to
  pehle, fail ho to DuckDuckGo fallback; Brave/SerpAPI/Maps sirf
  save, lane nahi (UI me saaf likha hai); states launcher se
  pehle (775 < 793); braces 0 sab files me.
- Test-plan entry: docs/FLORA_KEYS_TEST_ENTRY.md (6 steps).

## v2.1 Keys + rotation — code audit done, push v2.0 audit ke baad
- Code audit: rotation order Gemini 1, 2, Groq 1, 2; khali key
  skip; key values kabhi log nahi hoti; Tavily lane key ho to
  pehle, fail ho to DuckDuckGo fallback; Brave/SerpAPI/Maps sirf
  save, lane nahi (UI me saaf likha hai); states launcher se
  pehle (775 < 793); braces 0 sab files me.
- Test-plan entry: docs/FLORA_KEYS_TEST_ENTRY.md (6 steps).
- Push plan: docs/FLORA_V21_PUSH_PLAN.md; pre-push checklist pass.

## v2.0 Study/Coding/Market + v2.1 Keys — code local taiyaar, push themes audit ke baad
- v2.0: SearchTools study/stockPrice/websiteStarter + ToolsScreen
  sections; code audit done (braces 0); test entry
  docs/FLORA_TOOLS_TEST_ENTRY.md (6 steps).
- v2.1: Gemini/Groq second keys + Tavily lane + Brave/SerpAPI/Maps
  fields (sirf save, lane nahi — UI me saaf likha hai); rotation
  order Gemini 1, 2, Groq 1, 2; key values kabhi log nahi hoti;
  code audit done (braces 0, states launcher se pehle 775 < 793);
  test entry docs/FLORA_KEYS_TEST_ENTRY.md (6 steps).
- v2.2 Voice/Face match: design taiyaar, test entry
  docs/FLORA_VOICE_FACE_TEST_ENTRY.md (phone test se hi audit poora
  hoga, CI par nahi).
  NOTE (audit fix 2026-10-28): ye item ab v2.3 hai — v2.2 FCM ne le
  liya hai; upar wali line ka "v2.2" purana label tha.

## v2.1 Keys + rotation — audit PASS (2026-10-28)
- Run 40625192196 completed success, artifact 54,350,099 bytes,
  expired: false. Upar wali "CI result pending" entry stale thi.

## v2.2 FCM wiring — audit PASS (2026-10-28)
- Run 40627580432 completed success, artifact 54,540,574 bytes,
  expired: false. Project flora-f22a7, package com.flora.app.
- Test entry: docs/FLORA_FCM_TEST_ENTRY.md (6 steps).

## v2.2.1 audit stale-fix — audit PASS (2026-10-28)
- Run 40628284467 completed success, artifact 54,540,426 bytes,
  expired: false. Repo-verified: WAKE_WORD_DECISION_PENDING =
  false, Terms FCM line nayi ("client taiyaar hai..."), purani
  "jab available ho" line 0, versionCode 24 / 2.2.1-rebuild,
  full audit doc repo me (8007 bytes).
- Test entry: docs/FLORA_V221_TEST_ENTRY.md (Terms wording
  check; koi naya feature nahi, isliye koi naya behaviour
  check nahi).

## v2.3 — PASS (2026-10-28)
- Commit a38c69ee, versionCode 25 / 2.3.0-rebuild, CI run
  40629941547: completed / success, artifact flora-debug-apk
  59,388,952 bytes, expired false.
- Scope: face-match (photo enrolment) + Supabase anon default.
  Voice-match v2.4 me gaya (scope change push plan me darj).
- Code audit: declaration order, brace/paren, permissions,
  privacy (templates on-device encrypted, token kabhi log nahi),
  no copy/old-key — PASS (continuous audit doc me darj).
- Phone test baaki: docs/FLORA_FACE_TEST_ENTRY.md (7 steps).
  Face-match akela PIN unlock nahi karta — ye test me hai.

## v2.4 — PASS (2026-10-28)
- Commit c9c47cc0, versionCode 26 / 2.4.0-rebuild, CI run
  40630716314: completed / success, artifact flora-debug-apk
  59,400,661 bytes, expired false.
- Scope: wake word v0 — MFCC template, Settings enrolment
  (3 samples), foreground service, default OFF. Voice-match
  is push me nahi hai (alag item).
- Code audit: declaration order (wake states launcher se
  pehle), brace/paren, permissions, privacy (audio kabhi
  save/server par nahi, template encrypted on-device),
  no copy/old-key — PASS.
- Phone test baaki: docs/FLORA_WAKE_TEST_ENTRY.md (8 steps).
  Threshold 0.60 shuruaati guess hai — false trigger ka
  risk test entry me pehle se likha hai.
- v2.5 FCM token sync (2026-10-28): push-token-sync Edge Function DEPLOYED + live-verified (invalid->400 detail, test-UUID->ok:true); app: FloraConfig path, LicenceClient.syncPushToken, FloraApp token-fetch sync, FloraMessagingService onNewToken sync; balance 0/0/0 all touched files; CI pending; phone test entry FLORA_FCM_SYNC_TEST_ENTRY.md. AUDIT: code PASS, build PENDING.
- v2.5 FCM token sync PASS (2026-10-28 ~10:18 IST): CI run 40633826232 completed success, job build success, artifact 12792533866 flora-debug-apk 59,405,126 bytes, expired false. AUDIT: code PASS + build PASS + test entry FLORA_FCM_SYNC_TEST_ENTRY.md = DONE (phone test still pending separately).
- v2.6 usage wiring PASS (2026-10-28 ~10:26 IST): CI run 40634394059 completed success, artifact 12792629668 flora-debug-apk 59,405,561 bytes, expired false. AUDIT: code PASS + build PASS + test entry FLORA_USAGE_TEST_ENTRY.md = DONE (phone test still pending separately).

## v2.7 (Round 1 strings fix) — audit entry (push pending)
- Item: Terms Pro text + website Pro text me jhootha vaada
  (live wallpaper / conversation mode Pro feature) hata kar
  imaandari se "abhi available nahi, baad me aayenge" likha.
- Code audit: LegalScreens brace/paren 0/0; sirf text change,
  koi logic change nahi; koi copy/old-key violation nahi.
- Build test: CI run (v2.7) — result push ke baad verify hoga.
- Phone test entry: Terms screen kholo, Pro line padho —
  wallpaper/conversation "abhi nahi" dikhna chahiye.
- Web: admin.html Audit list wired + plans text honest;
  index/pricing text honest. Deploy ke baad 200-check hoga.

## v2.8 app-link — CODE PREPARED LOCALLY, PUSH PENDING v2.7 CI PASS (per-item gate)
- Item: Settings "Website account link" (email+password -> Auth
  password grant; uid+refresh DataStore me, access token kabhi
  save nahi; unlink button; purani random-UUID licence orphan
  warning UI me). Licence/usage/token-sync call sites ab
  effectiveUserId() use karte hain (linked ho to Auth uid,
  warna random uuid — backward compatible).
- Code audit: FloraPreferences + LicenceClient + FloraApp
  brace/paren 0/0/0; link states line 836 launcher se PEHLE;
  password state screen-only, kabhi log/store nahi; koi
  service_role key nahi; koi copy/old-key violation nahi.
- Build test: PENDING — v2.7 CI PASS ke baad hi v2.8 push
  hoga, tab CI + artifact size verify hoga.
- Phone test entry: TODO (link/unlink/Pro-dikhna steps) —
  push ke saath likhi jayegi.

## v2.8 app-link — PUSHED (v2.7 CI PASS ke baad, gate clear)
- v2.7 CI: run 40636820057 completed success, artifact
  flora-debug-apk 59,408,672 bytes, expired false. Isliye
  v2.8 push ka gate khula.
- Build test (v2.8): CI run — result push ke baad verify hoga.
- Phone test entry: docs/FLORA_APP_LINK_TEST_ENTRY.md (6 steps).

## v2.8 app-link — CI PASS VERIFIED (2026-10-28 ~10:57 IST)
- Run 40637321596 completed success, artifact flora-debug-apk
  59,412,409 bytes, expired false. v2.8 = latest PASS.
- Gate clear: Round 2 items ab push ho sakte hain.

## Clean-URL fix — web-only (watcher tick ne pakda)
- Finding: /admin, /pricing, /login, /signup, /pay, /account
  bina .html ke Vercel NOT_FOUND 404 dete the — web/ me koi
  vercel.json rewrites/cleanUrls nahi tha. .html paths par
  7/7 pages 200 the, isliye ye chhota layer issue pehle
  chhoot gaya tha (imaandari se note).
- Fix: web/vercel.json me {"cleanUrls": true} — /admin ab
  /admin.html serve karega; .html paths pehle jaise chalte
  rahenge. Koi app code change nahi, isliye version bump
  nahi (web-only commit).
- Verify: naye deploy par clean URLs 200-check hoga.
- Verify (naya deploy flora-f83x55cea READY): clean URLs
  /admin /pricing /login /signup /pay /account sab 200;
  .html paths 308 redirect karke clean URL par 200; root 200.
  Clean-URL issue CLOSED.

## admin-list function + admin Users/Usage wiring — DEPLOYED + gate verified
- Function admin-list dashboard editor se deploy hui
  ("Successfully deployed edge function"); ab total 6
  functions live hain.
- Live verify: bina header gateway 401; anon-key Bearer par
  code ka apna {"error":"unauthorized"} 401 — code deploy
  hua aur chal raha hai, iska proof hai. Asli list (users/
  usage/licences) tabhi test hogi jab admin user banega.
- admin.html: loadAdminList() wired (session token se call,
  users + licences + usage render; koi token/UTR render nahi).
  Push ke baad naye deploy par admin page 200-check hoga.

## v2.9 retention + dormant note — PUSHED (v2.8 CI PASS ke baad)
- Item A (dormant note): settings/plans ko schema file me
  "dormant config, runtime source-of-truth nahi" likha gaya;
  koi table delete nahi hui. Live-verified: settings 0 rows,
  plans 2 seeds, koi code inko read nahi karta.
- Item B (retention): JournalStore + MemoriesStore me
  pruneOlderThan (cutoff 30 din); app-start par chupchaap
  chalta hai, fail ho to start nahi rukega; bina date wali
  entries kabhi auto-delete nahi hoti. Memories ki nayi
  entries ab date ke saath save hoti hain (purani plain
  strings waise hi padhi jaati hain — migration-safe).
  Privacy + Settings text precise wording me badla gaya.
- Code audit: 4 files brace/paren 0/0 (push se pehle check).
- Build test: CI run — result push ke baad verify hoga.
- Phone test entry: docs/FLORA_RETENTION_TEST_ENTRY.md
  (30-din prune ka phone proof 30 din baad hi possible hai
  — ye limit entry me imaandari se likhi hai).

## Free "15 tools" claim — HONEST TEXT FIX (PREP, push v2.9 CI PASS ke baad)
- Problem: "lagbhag 15 tools" ka vaada tha par 15 tools ki
  koi list kabhi define nahi hui; ToolsScreen me 7 sections
  + baaki screens ke tools hain, count ka koi matlab nahi.
  Fake gating lagana free users se chalte hue tools chheenna
  hota — bina user order ke nahi kiya gaya.
- Fix: har jagah "app me maujood saare tools" likha gaya
  (FloraConfig comment, FloraApp, LegalScreens, pricing);
  unused FREE_TOOLS_COUNT constant hata diya gaya.
- Ye claim-fix hai, feature-removal nahi — free users ko
  kuch kam nahi milega.

## v2.9 retention — CI PASS VERIFIED (2026-10-28 ~11:12 IST)
- Run 40638744449 completed success, artifact flora-debug-apk
  59,413,304 bytes, expired false. v2.9 = latest PASS.
- Admin-wiring run 40638511792 bhi success (59,412,232 bytes).
- Gate clear: v2.10 push ho sakta hai.

## v2.10 free-tools honest text + dormant CLOSED repo note — PUSHED
- "15 tools" vaada hata kar "app me maujood saare tools"
  likha (FloraConfig, FloraApp, LegalScreens, pricing);
  unused FREE_TOOLS_COUNT constant hataya. Ye claim-fix hai,
  free users ko kuch kam nahi milega.
- Round 1 items 3-4 CLOSED section ab repo doc me hai
  (pehle sirf local file me tha — sudhaar).
- Build test: CI run — result push ke baad verify hoga.

## v2.10 honest text — CI PASS + LIVE VERIFIED (2026-10-28 ~11:21 IST)
- Run 40639602390 completed success, artifact flora-debug-apk
  59,412,864 bytes, expired false. v2.10 = latest PASS.
- Live: naye deploy flora-fxx247dqz-webbuilder1.vercel.app
  READY; pricing clean URL 200 aur naya wording "maujood
  saare tools" live confirm hua (purane host par purana text
  dikhna sirf stale deploy tha, outage nahi).

## Test-row cleanup — DONE + VERIFIED (2026-10-28 ~11:30 IST)
- Dashboard SQL se sirf meri test rows delete ki gayi:
  push_tokens (device test-device-v25) aur users (test UUID).
  Dono DELETE bina error ke chale (Supabase "0 rows" dikhata
  hai kyunki DELETE koi row wapas nahi karta — ye normal hai).
- Counts pehle: users 1, push_tokens 1. Counts baad me:
  users 0, push_tokens 0. Baaki tables (payment_submissions,
  admin_audit, licences, usage, plans, settings) chhooi nahi
  gayi. Database ab test data se saaf hai — asli user ka
  pehla data ab signup/phone se hi aayega.

## v2.11 short-casual replies default — PUSHED (user order 2026-10-28 ~12:14 IST)
- BrainClient dono lanes (Gemini + Groq) me system instruction
  badla: pehle "chhota aur saaf" tha, ab "dost se casual
  conversation jaisa, aam taur par 1-2 line, lamba/formal nahi,
  user ki language me, kaam ki baat pehle, detail sirf poochhe
  par". Ye default hai, har chat par lagu hoga.
- Audit: sirf prompt text badla — koi permission, key, privacy,
  ya data flow nahi chheda; braces/parens balance 0/0.
- Build test: CI run — result push ke baad verify hoga.

## v2.11 short-casual — CI PASS VERIFIED (2026-10-28 ~12:20 IST)
- Run 40645704376 completed success, artifact flora-debug-apk
  59,414,028 bytes, expired false. v2.11 = latest PASS.
- Short-casual default ab verified build me hai (phone test par
  style ka asli proof milega — brain ko Settings me key chahiye).

## v2.11 APK content verify + SIZE CORRECTION (2026-10-28 ~12:34 IST)
- aapt2 badging se verify: package com.flora.app,
  versionCode 33, versionName 2.11.0-rebuild, sdk 26/34,
  616 entries, dex + manifest maujood. Yehi asli v2.11 APK hai.
- CORRECTION: ab tak "59,414,028 bytes APK" likha gaya tha —
  wo galat tha. 59,414,028 bytes ARTIFACT ZIP ka size hai.
  Asli APK = 111,637,021 bytes,
  sha256 5e320e94c1f308fc8d1a78ceda5cafe9ec658d40d01da82497924abac40b5a45.
  Aage se artifact size ko APK size mat likhna.
- Download hosting: Muse built-in storage ne 111MB par 413
  Payload Too Large diya (publish nahi hua). Agla raasta:
  Google Drive (connect pending, user step) ya SwissTransfer
  (user approval chahiye) — dono user ke faisle par atke hain.

## Licence 1-phone/15-day rule — DEPLOYED + LIVE TEST PASS (2026-10-28 ~12:40 IST)
- Dashboard editor se licence-verify ka poora code badla,
  confirmation "Successfully updated edge function" mili.
  Koi doosra function/secret/table nahi chheda.
- Live test (bina auth): test UUID + unknown device par
  HTTP 200 {"plan":"free","status":"free"} — expected.
- IMAANDAAR LIMIT: block (15 din se pehle) aur transfer
  (15 din baad) wale raaste abhi test NAHI hue — unke liye
  ek asli pro licence row chahiye, jo admin-approve se tabhi
  banegi jab pehla signup + payment aayega. Code me dono
  raaste maujood hain, runtime proof pending hai.
- Repo copy (supabase/functions/licence-verify/index.ts) deployed
  code se match karti hai; agle push me repo sync hoga.

## Download page + SwissTransfer hosting — PUSHED (2026-10-28 ~12:47 IST)
- JAHPUL wahi SwissTransfer verification code tha — accept hua,
  upload 0% -> 100% poora hua, koi account/payment nahi bana.
- Link: swisstransfer.com/dl/01a1815d-7fd1-71ac-b656-0282f1d52e92
  (flora-v2.11.apk, 106.5 MB, link-only, bina password, koi
  sign-in nahi; page par Download button hai, direct file nahi).
- Validity: 15 din active (12 Nov 2026 tak expire) + email se
  15 din extension = 30 din total. 60 din wala option paid
  Enterprise ka hai, use nahi kiya.
- web/download.html banaya (version, size, SHA-256, sideload
  steps, validity note); index.html ka stale "~15 basic tools"
  bhi isi push me honest text se sudhara; licence-verify repo
  copy deployed code se sync ki gayi.
- Build test: Vercel deploy + /download live check push ke baad.
