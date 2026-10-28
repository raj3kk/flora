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

## Download page — LIVE VERIFIED (2026-10-28 ~12:50 IST)
- Naya deploy flora-2318h9ew5-webbuilder1.vercel.app READY;
/download clean URL 200, SwissTransfer link + SHA-256 page par
maujood, index bhi 200. Download page ab live hai.

## Brain proxy Phase B step 1 — CODE READY, deploy baaki
- supabase/functions/brain-proxy/index.ts likha: Gemini ->
  Groq fallback, short-casual style, prompt cap 4000 chars,
  keys sirf env se (GEMINI_API_KEY/GROQ_API_KEY), kabhi log
  nahi hongi. Keys missing hon to saaf 503.
- HONEST LIMIT: JWT gate abhi nahi (app me Auth session nahi
  hai) — key-burn risk step 2 me band hoga. Owner keys tabhi
  daale jab proxy istemal chahiye.

## Brain proxy Phase B step 1 — DEPLOYED + LIVE TEST PASS (2026-10-28 ~12:51 IST)
- Dashboard editor se NAYA function brain-proxy banaya,
  confirmation "Successfully deployed edge function" mili.
  Koi purana function/secret/table nahi chheda; GEMINI_API_KEY
  / GROQ_API_KEY unset hain (owner baad me set karega).
- Live test: POST par HTTP 503 {"error":"brain keys not
  configured"} — keys unset hone par yehi expected hai, isliye
  ye PASS hai, fail nahi. Ab functions total 7 live hain.
- Baaki: owner keys set kare tabhi asli reply test hoga; JWT
  gate + quota step 2 me (app Auth ke baad).

## Backup refresh + restore test — PASS (2026-10-28 ~12:59 IST)
- Naya backup: apk-recovery-backups/backup-20261028-1255/
  apk-recovery.tar.gz, 105,684,969 bytes, 43,557 entries.
- Restore test PASS: tar listing OK; 2 spot-check files
  (audit log + brain-proxy code) byte-identical nikli;
  tar se nikala original Maya APK sha256
  886f6bca...c456 se match (original untouched).
- Flora v2.11 APK sha256 5e320e94...b5a45 manifest me darj
  hai (APK file khud tar se exclude hai — wo GitHub artifact
  + SwissTransfer par maujood hai).
- NOTE: extract karte waqt ownership change block hota hai
  (uid 0) — --no-same-owner se extract karo, content sahi
  milta hai.

## Voice-match frontend validation — PASS (2026-10-28 ~13:05 IST)
- Asli conformer encoder + VAD (HF tflite-hub, Apache-2.0) par
  Lingvo-exact frontend ke saath end-to-end test chala:
  same-speaker cosine mean 0.671, noise ~0.33, silence
  VAD-reject, noise stability 0.877. Script:
  wakeword/validate_voice_match.py (local workspace).
- Kotlin port ke liye: exact TF mel matrix asset me ship
  karna hai, HTK formula nahi. Threshold starting guess
  ~0.45-0.50; final phone test + doosre speaker data se.
- Baaki: Kotlin port + enrol/match UI + phone test (ye item
  phone test ke bina PASS nahi mana jayega).

## v2.12 user-report fixes — PASS (2026-10-28 ~13:20 IST)
- CI run 40651357407 completed success; asli APK 111,653,485 bytes, sha256 1d37653186faf937414596c3a0115a5a74465b5f62126b14540c5ff731ef9181, aapt2: com.flora.app vCode 34 / 2.12.0-rebuild, sdk 26/34. SwissTransfer upload COMPLETE 2026-10-28 ~13:25 IST: link https://www.swisstransfer.com/dl/01a1817e-db1d-7078-97a5-d8ce94a7a170, 106.5 MB (size match), 15 din validity (12 Nov 2026 tak), is baar koi email code nahi maanga gaya, koi account/payment nahi. Download page v2.12 par update + push (e9eb1cde), Vercel deploy verify pending.
- Artifact download note: GitHub 302 ko urllib follow karne par blob host 401 deta hai (surrogate auth header aage badh jata hai) — pehle 302 ka Location pakdo, phir use bina auth header ke fetch karo. Ye pattern agli baar bhi use hoga.
- Shikayat: (1) app khulte hi permissions nahi maangta, (2) key
  daalne par bhi "key daaliye" bolta hai, (3) 16 buttons bikhre
  pade hain, koi arrangement nahi, (4) background me nahi chalta,
  notification nahi aata.
- Jad (code se pakki): (1) startup par koi permission flow tha hi
  nahi — sirf lazy per-feature maanga jata tha; (2) har failure
  par ek hi generic message tha — key saved hai ya nahi, galat
  hai ya quota khatam, farq nahi bataya jata tha; (3) 16 buttons
  2 rows me the, koi menu/group nahi; (4) POST_NOTIFICATIONS
  kabhi startup par maanga hi nahi gaya (Android 13+ par bina
  iske notification dikhta hi nahi), BOOT receiver nahi tha,
  battery-opt flow nahi tha, aur push notification ka id 1001
  Wake Word ke foreground id se takra raha tha.
- Fix: PermissionGateScreen (pehli baar 6 permissions kaaran ke
  saath + Aage badho; deny sirf feature band karta hai, app
  nahi) + Settings se dobara; BrainClient me alag messages
  (khali / galat-expired / quota / network) + Keys-check button
  (models-list GET, koi generation cost nahi, key value kabhi
  screen par nahi); TopAppBar me grouped DropdownMenu (5 groups)
  + screen title; BootReceiver (sirf Wake ON tha to restart);
  Settings me Background section (notification/battery status
  live + battery-opt dialog + Vivo auto-start + refresh);
  push notify id 1001 -> 2001.
- NAHI badla: Accessibility/dialer role/battery hamesha user ki
  marzi se manual hi rahenge (kabhi gate me zabardasti nahi);
  payment hamesha alag approval; koi key value kabhi log nahi.
- Test entry: docs/FLORA_V212_TEST_ENTRY.md (6 sections).
  Phone test + asli push test abhi bhi user-gated hain.

## v2.13 setup wizard + contacts + location — CODE DONE, CI PENDING (2026-10-28 ~13:35 IST)
- SetupWizard.kt (new, 3 steps): Step 1 Welcome; Step 2 Permissions — 8 rows (Mic, Notifications, Camera, Call, Contacts, SMS, Phone state, Location), har row tap = akeli permission, "Sab allow karo" = RequestMultiplePermissions (system dialogs ek-ek karke), "App settings kholo" fallback, mic-zaroori gate ("Aage badho" sirf mic ke baad); Step 3 Background — battery-opt status + dialog, Vivo auto-start, Accessibility status (service list check) + settings. FloraApp gate block wizard se replace; Settings "Permissions dobara dekho" wizard kholta hai.
- Contacts (new ContactLookup.kt): naam se number (exact match pehle, phir partial), koi data save/log/server nahi; Chat "call <naam>" / "sms <naam> <msg>" pehle lookup, phir wahi ek-confirmation flow; na mile to saaf message. Manifest: READ_CONTACTS, ACCESS_FINE_LOCATION.
- Location weather (SearchTools.weatherHere): last-known GPS/NETWORK location se Open-Meteo forecast; lat/lon kahin save nahi; Chat "yahan ka mausam" bina brain key ke jawab deta hai; permission na ho to saaf message, galat mausam nahi.
- 178 audit (docs/FLORA_178_AUDIT_20261028.md): module-wise hai/aadha/nahi/excluded — Default assistant, WhatsApp auto-reply, email, Marketing/Hosting/Whiteboard, live wallpaper, floating charm, skills/sub-agents, Maya Home app UI, neural wake model NAHI likha hai (fake complete nahi).
- Arrangement: dropdown navigation (v2.12) se har page full-screen hai; Settings ka section-filter is push me nahi hua (LazyColumn single-item refactor bada hai — alag item me hoga, yahan jhootha daava nahi).
- Pre-push audit: brace/paren 0/0 (FloraApp, SetupWizard, PermissionGate, SearchTools, ContactLookup); declaration order: lastRequested line 105 < callback 110 < use 166; wizard ke saare launcher step branches ke bahar declare; test entry docs/FLORA_V213_TEST_ENTRY.md (4 sections). Excluded list intact: Maya design/character copy nahi (wizard Flora ke apne shabdon me hai), silent posting nahi (call/SMS confirmation-gated), koi key value log nahi.

## v2.14 continuous mic + turn-taking + background wake — CODE DONE, CI PENDING (2026-10-28 ~13:30 IST)
- ContinuousListener.kt (new): system SpeechRecognizer bina dialog ke; result -> MainActivity -> FloraApp sendChat (v2.14 me Bhejo pipeline sendChat() fun me nikali gayi, holder ke zariye effect se call hoti hai — script se extract, braces/parens verify).
- Voice default: wizard-done + returning-user dono par app khulte hi sunna shuru (mic granted ho to); Mute dabane tak loop (TTS-done par restart, error/chuppi par 600ms restart, 45s safety restart); onPause par stop (foreground continuous), onResume par restart.
- Manual typing BHI rahega: input box + Bhejo unchanged; mute ke baad bhi manual chalta hai.
- Turn-taking: VoiceEngine onSpeechStart/onSpeechDone -> UI indicators "Sun raha hun…" / "Soch rahi hun…" / "Bol rahi hun…"; Stop (continuous mode me) -> "Ruk, kuch bol rahe the?" poochh kar phir sunna shuru.
- Background wake: WakeWordService wake par MainActivity foreground laata hai (flora_wake extra, 2s delay greeting ke baad) — mute ho to bhi isi session ke liye sunna shuru (wake = "ab baat karo" ka ishara).
- Imaandaari se baaki: poora background pipeline (service me brain+commands) nahi hai; barge-in (Flora bol rahi ho tabhi user bole to turant rukna) system recognizer se nahi hota — Stop hi interrupt raasta hai.
- Pre-push audit: brace/paren 0/0 (FloraApp, MainActivity, ContinuousListener, VoiceEngine, Preferences, WakeWordService); test entry docs/FLORA_V214_TEST_ENTRY.md; decisions FLORA_DECISIONS.md (Maya=Flora ek hi app; voice default + manual bhi).

## v2.13 CI FAIL — root cause + fix (2026-10-28 ~13:33 IST)
- CI run 40653036845 (v2.13, ca58a3cf) completed failure; job 122697386646 step 4 Build debug APK fail.
- Root cause (log line 250): SearchTools.kt:135:22 "Unresolved reference: PackageManager" — v2.13 me weatherHere() me PackageManager.PERMISSION_GRANTED use hua par import nahi daala tha (sirf ContextCompat daala tha). Meri galti, ek line ki kami.
- Fix: SearchTools.kt me "import android.content.pm.PackageManager" add kiya. Uske baad poore tree me check kiya — PackageManager use karne wali har file me import maujood hai (MainActivity, AutoActions, ContactLookup, SearchTools, FaceScreen, FloraApp, ScanScreen, SetupWizard, ContinuousListener, WakeWordService — sab OK).
- Ye fix v2.14 ke upar gaya hai (v2.14 me wahi bug tha, isliye uska CI bhi fail hona tay tha) — fix commit ka CI hi ab asli gate hai.

## v2.14-fix CI FAIL #2 — root cause + fix (2026-10-28 ~13:37 IST)
- CI run 40654054803 (fix commit 8da15004) bhi completed failure; job 122700709387 step 4 fail.
- Root cause: PermissionGate.kt lines 30-32 "Expecting a top level declaration" — v2.13 me deprecation note daalte waqt purane doc comment ka opening "/**" replace ho gaya tha, par uski body lines khuli reh gayi thi (bare text). Watcher tick ne isko "orphan comment block" keh kar pehle hi pakda tha, aur maine use cosmetic samajh kar chhod diya tha — ye galti meri hai, doosri baar wahi file.
- Fix: poora header ek hi saaf comment block me merge kar diya (deprecation note + purana note ek block me). Brace/paren balance verify kiya.
- Seekh: watcher ki flag ko "cosmetic" keh kar kabhi mat chhodo — pehle file khol kar dekho, phir faisla karo.

## v2.14-fix2 CI PASS (2026-10-28 ~13:43 IST)
- CI run 40654474955 (fix2 commit a54ea29a) completed success. Asli APK 111,702,752 bytes, sha256 8ab05d7b129ddd087d440a15f9fd8452f0150efd0e52821df63363d14a08a682, aapt2: com.flora.app vCode 36 / 2.14.0-rebuild, sdk 26/34. Artifact zip size 59,480,097 bytes (APK size se alag — phir se wahi galti nahi).
- SwissTransfer upload (v2.14) browser task se chal raha hai; link milte hi download page 2.14 par update hoga.

## v2.14 SwissTransfer + download page (2026-10-28 ~14:35 IST)
- SwissTransfer upload complete (browser task): link https://www.swisstransfer.com/dl/01a181c0-d5cc-724f-8b3c-b73d71d08637, 106.5 MB shown, 15 days, expires 12 Nov 2026. Is baar email code nahi manga gaya. No account, no payment.
- web/download.html v2.14 par update (version, size 111,702,752, SHA 8ab05d7b..., link, what's-new = continuous mic + wizard + contacts + location).

## v2.14 download page deploy VERIFIED (2026-10-28 ~14:36 IST)
- Deploy flora-7mrth3jbk-webbuilder1.vercel.app READY; /download live: v2.14.0-rebuild, size 111,702,752, SHA 8ab05d7b..., SwissTransfer link 01a181c0-..., what's-new continuous mic + wizard + contacts + location. Deploy-verify pending item CLOSED.

## v2.15 CODE DONE / CI PENDING (2026-10-28 ~15:35 IST, user complaints se)
- Brain root cause (VERIFIED web se): Gemini gemini-1.5-flash retired + Groq llama-3.1-8b-instant 16 Aug 2026 ko free/dev se hata — isliye key sahi hone par bhi har lane 404 de rahi thi aur app use "network error" bata raha tha. Fix: Gemini 2.5-flash -> 2.5-flash-lite, Groq openai/gpt-oss-20b -> 120b, har lane ka asli code message me. Brain-proxy repo copy bhi wahi model names par update (dashboard redeploy keys set hone par).
- BackgroundMicService (foreground, mic type, notif 2002): mic ON = service hamesha; foreground STANDBY, back par TAKEOVER (sunna + brain + TTS service me); command background se execute nahi — app aage la kar confirmation (FloraCommandBus). MicOwner se WakeWord loop pause. Boot par wapas chalu karne ki koshish (try/catch).
- PermissionsScreen (Menu): runtime rows + battery unrestricted live + Vivo auto-start + Accessibility live + dialer role — sab ek jagah.
- LicenceScreen (Menu): live server status + steps (signup > UPI > UTR > admin approve) + UPI pay + UTR save. Licence koi type karne wala code nahi — account-linked hai.
- Baat view DEFAULT (voiceView pref, default true): transcript/typing/status-text chhupa, sirf orb + emoji buttons; Chat toggle se transcript wapas.
- Beep mute: sunne ke dauraan SYSTEM+MUSIC+NOTIFICATION mute, result/error/stop par unmute + 20s failsafe. Device ki apni beep par limit imandari se likhi hai.
- ImageTools (Pollinations, bina key): "image banao <prompt>" -> Gallery > Flora me save.
- Audit: 11 kt files brace/paren/bracket 0/0; launcher/state declaration order verify (PermissionsScreen launchers upar, FloraApp voiceView state TopAppBar se pehle).
- Test entry: docs/FLORA_V215_TEST_ENTRY.md (7 sections).

## Screenshot feature plan (2026-10-28 ~15:45 IST, user ke 2 screenshots se)
- Dono screenshots ke 22 feature cards + latest-updates ke 10 points, sab ka audit: code grep se haalat verify (LiveScreen vision HAI, notification listener/watchlist/whiteboard/game/PC-link NAHI, website starter stub, routines teach NAHI, docs voice-create NAHI, torch/brightness NAHI, sleep phrase NAHI, email NAHI).
- Natija: HAI-poora 0, AADHA 16, NAHI 9, romantic persona EXCLUDED (standing rule). Plan doc: docs/FLORA_SCREENSHOT_FEATURE_PLAN_20261028.md — har feature par frontend + backend + database + workflow + gates, 5 phases me order (P1 → P5), har item ka gate (audit → CI → phone test).

## v2.15 CI PASS + APK verified (2026-10-28 ~15:41 IST)
- CI run 40669189539 (commit c1349187) completed success. Asli APK 111,735,544 bytes, sha256 e53590eda1b87ec867d355f175c8e6e1c42f102d7260edf167e228cb51554809, aapt2: com.flora.app vCode 37 / 2.15.0-rebuild, sdk 26/34. Artifact zip size 59,524,009 bytes (APK size se alag).
- SwissTransfer upload (v2.15) browser task se chal raha hai; link milte hi download page 2.15 par update hoga.

## v2.16 CODE DONE / CI PENDING (2026-10-28 ~15:45 IST, user order: image = ChatGPT, install = Play Store)
- Image flow badla (user order): "image banao X" ab ChatGPT app kholta hai → prompt box me prompt bharta hai → Send dabata hai → image ChatGPT me banti hai. ChatGPT na ho to Play Store se install karke wahi flow. Accessibility OFF ho to Flora settings khol kar bolegi. Prompt box/Send na mile to saaf failure + bata kar Pollinations fallback (chhupke nahi).
- Install flow (user order): "X install karo" → jaani apps (ChatGPT, WhatsApp/Business, Instagram, Facebook, YouTube, Chrome, Gmail, Maps, Telegram, Spotify) ka Play Store page → Install (accessibility se) → install detect (launch-intent poll, 180s) → app kholo. Anjaani app → Play Store search kholo, user Install dabaye. Pehle se installed ho to seedha kholo. Paid app ka payment step hamesha user khud Play Store me karega.
- Background: image/install commands ab command-bus se activity me aate hain (foreground me execute, confirmation rule barkarar).
- Audit: 4 kt files brace/paren 0/0; helpers sendChat se pehle declare (594 < 720); imports verify (AppAutomation, FloraAutomationService, delay).
- Test entry: docs/FLORA_V216_TEST_ENTRY.md.

## v2.15 download page LIVE (2026-10-28 ~15:47 IST)
- SwissTransfer link: https://www.swisstransfer.com/dl/01a181ff-71d3-70fa-a0dd-e6b601d9d0bf (106.6 MB, 15 din, 12 Nov 2026 tak; bina password, bina code, bina account/payment).
- Download page update commit 81754db6 → deploy flora-3ktz70dti-webbuilder1.vercel.app READY; /download par live verify: v2.15.0-rebuild, size 111,735,544, SHA e53590ed…, whats-new v2.15. (Pehli fetch purane deploy par lagi thi — commit se match karke sahi deploy verify kiya.)

## v2.17 CODE DONE / CI PENDING (2026-10-28 ~15:50 IST, user order: pehle "kabhi nahi" wale banao)
- Romantic persona: personaPrefix me "romantic" (pyaar bhara, caring tone; explicit/ashleel nahi) + Settings list + voice switch ("romantic mode" / "normal mode").
- Auto Mode (default OFF, user ka switch): ON par call/SMS/app-kholo bina confirm dialog ke seedha execute + AutoLogStore me entry + ongoing notification (id 2003) + "auto mode band" par band. AutoLogScreen (Menu > Auto Log) + 30-din prune. Payment isse kabhi auto nahi (money rule barkarar).
- Nahi khola (saaf wajah ke saath): payment auto-confirm (paisa bina poochhe kat sakta hai) + creator code/UI/assets copy (kanoon + ownership proof nahi) + hard lock jo user khud na khol sake (phone band ho jayega — PIN wala lock aage aayega) + purani keys (hain hi nahi, nayi hi banengi).
- Audit: 6 files brace/paren 0/0; autoMode state (179) + autoLogStore (180) helpers (739) se pehle; Settings call-site args signature order se mile; AutoLogScreen Column fillMaxSize (MemoriesScreen pattern).
- Test entry: docs/FLORA_V217_TEST_ENTRY.md.

## v2.16 CI PASS + APK verified (2026-10-28 ~15:51 IST)
- CI run 40670610839 (commit dd09af19) completed success. Asli APK 111,735,544 bytes, sha256 dc926db0395cc23fd38ed216ab33c4cd65eec2ffa45809e99792e91d7642f468, aapt2: com.flora.app vCode 38 / 2.16.0-rebuild, sdk 26/34. Artifact zip size 59,527,804 bytes. (v2.15 se size barabar hai, SHA alag hai — versionCode se verify kiya gaya hai.)
- SwissTransfer upload (v2.16) browser task se chal raha hai; link milte hi download page 2.16 par update hoga.

## v2.18 CODE DONE / CI PENDING (2026-10-28 ~15:55 IST, user order: pura Flora Auto Mode me hi rahe, bol kar, bina ruke)
- Auto Mode DEFAULT ON (prefs default true; pehle OFF tha): bolte hi kaam shuru, bina har baar poochhe. Band karne ke liye switch OFF ya "auto mode band" (pehle jaisa).
- Routines: Auto Mode ON ho to call/SMS steps bhi seedha chalenge + log honge (pehle skip hote the); OFF ho to pehle wala skip rule wapas.
- Narration: voice commands + routine steps pehle se bol kar chalte the — v2.18 me ye poora cover hai (koi chupchaap kaam nahi). Ek confirmation abhi bhi lagega: payment, delete-all, ek naam ki kai apps, pehli permission, SOS emergency — ye Settings text + test entry me saaf likha hai.
- Audit: 2 files brace/paren 0/0; routine branch me autoMode/autoLogStore/scope sab pehle declare hain; comment bhi update (purana "nahi chalte" wala jhooth nahi rahega).
- Test entry: docs/FLORA_V218_TEST_ENTRY.md.

## v2.17 CI FAIL → ROOT FIX (2026-10-28 ~15:56 IST, meri galti)
- CI run 40671342205 completed failure: FloraApp.kt:180:48 Unresolved reference: appContext.
- Wajah: autoLogStore line appContext ki declaration (line 183) se PEHLE likhi thi — wahi declaration-order trap jo AGENTS.md me darj hai. Fix: autoLogStore ko appContext ke BAAD le jaya gaya + comment bhi sudhara (default OFF → v2.18 default ON).
- Re-audit: brace/paren balance OK; line order grep se verify (appContext < autoLogStore < helpers).

## v2.16 download page LIVE (2026-10-28 ~15:57 IST)
- SwissTransfer link: https://www.swisstransfer.com/dl/01a18209-64fe-73a3-b5e9-3f8163382772 (106.6 MB, 15 din, 12 Nov 2026 tak; bina password, bina code, bina account/payment).
- Download page update commit 0f86c839 → deploy flora-ck70ymb12-webbuilder1.vercel.app READY; /download par live verify: v2.16.0-rebuild, size 111,735,544, SHA dc926db0…, whats-new v2.16 (ChatGPT image + Play Store install).

## v2.18 CI PASS via fix run (2026-10-28 ~16:03 IST)
- Fix run 40672135141 (commit 62b3e2c5) completed SUCCESS. Artifact 12808204318, zip 59,547,075 bytes (exact match).
- Real APK: 111,768,312 bytes, sha256 8d536ae5d1e76a1b06a00c495fcd8adc3872dff996830073832f92a42dadb88a, aapt2: com.flora.app, versionCode 40, versionName 2.18.0-rebuild.
- NOTE: ye tree v2.17-fix + v2.18 content dono rakhta hai (Auto Mode default ON + routine call/SMS auto + romantic persona + Auto Log). v2.18 ka apna run (d3925197) expected FAIL tha — fix run se cover.
- SwissTransfer upload in progress; link milte hi download page update hoga.

## v2.19 dead-code cleanup audit (2026-10-28 ~16:10 IST, user order: bina kaam ka sab hatao)
- REMOVE (grep se zero external ref verify karke): deps appcompat, work-runtime-ktx, room-runtime, ui-tooling-preview (koi @Preview, WorkManager, Room, AppCompat use nahi); file PermissionGate.kt (khud ke header me likha hai v2.13 SetupWizard ne replace kar diya, use nahi hoti); permission ACCESS_NETWORK_STATE (koi ConnectivityManager/activeNetwork use nahi); res/drawable/flora_app_icon.webp (APK me tha par manifest me koi android:icon nahi, koi ref nahi — icon pehle se hi default tha, isliye dikhne me koi farq nahi); res/media-generation-* webp+json (repo clutter, APK me the hi nahi, koi ref nahi).
- KEPT with reason: AutomationGate.kt (call-count zero hai, par ye consent-policy class hai — safety gate ko ref-count par delete nahi kiya; re-wire P-work me flag hai); READ_PHONE_STATE (CallAnnouncementReceiver me TelephonyManager use karta hai); strings.xml (accessibility config use karta hai); saare 19 Screen routes + ExtraScreens (sab use me hain).
- Purane hosts ka sweep: getmaya/hunterislive/flow99/turso — Flora tree me ZERO (clean-room barkarar).
- Audit: gradle + manifest edit ke baad balance check CI par hoga; version 41 / 2.19.0-rebuild.

## v2.18 download page LIVE (2026-10-28 ~16:08 IST)
- SwissTransfer link: https://www.swisstransfer.com/dl/01a18214-1143-73bd-8af4-ddffceecba0e (106.6 MB, 15 din, 12 Nov 2026 tak; bina password, bina account/payment; 30-day option chuna tha par confirmation 15 din + email extension batata hai).
- Download page update commit 1b390874 → deploy flora-h33uq14f0-webbuilder1.vercel.app READY; /download par live verify: v2.18.0-rebuild, size 111,768,312, SHA 8d536ae5…, whats-new v2.18 (Auto Mode default ON + romantic).

## v2.19 CI PASS + APK verify (2026-10-28 ~16:14 IST)
- Run 40673581577 completed SUCCESS (commit 81e1a66a). Artifact 12809722722, zip 59,046,479 bytes (exact match).
- Real APK: 111,265,998 bytes (v2.18 se 502,314 bytes halka — dead deps hatne ka asar), sha256 5c07682999fcd21ca8753039345309285a46416515a623f4b6685d5fe412a71c, aapt2: com.flora.app, versionCode 41, versionName 2.19.0-rebuild.
- APK content check: PermissionGate / flora_app_icon ke 0 matches — dead code APK se gayab, confirm.
- SwissTransfer upload in progress; link milte hi download page update hoga.

## v2.19 download page LIVE (2026-10-28 ~16:23 IST)
- SwissTransfer link: https://www.swisstransfer.com/dl/01a18222-7560-7099-a289-064d7bce1d90 (106.1 MB, 15 din, 12 Nov 2026 tak; bina password, bina account/payment; 30-day selector par bhi confirmation 15 din + email extension).
- Download page update commit 85d35517 → deploy flora-glx5z3j2a-webbuilder1.vercel.app READY; /download par live verify: v2.19.0-rebuild, size 111,265,998, SHA 5c076829…, whats-new v2.19 (cleanup, ~500 KB halka).

## v2.20 CODE DONE / CI PENDING (2026-10-28 ~16:30 IST, user order: technical error kabhi nahi, screen share ek baar permission, training + poora comparison)
- 404 ki asli wajah: gemini-2.5-flash naam SAHI hai (live verify) — user ki key par dono models 404 isliye de rahe hain kyunki us key wale project me Gemini API on nahi hai (ya key Gemini ki nahi hai); Groq keys khaali thi isliye detail me Groq aaya hi nahi. Fix: model auto-discovery (models-list se jo chale wahi use hoga, naam sadne ka khatra khatam) + Keys check me 404 ka alag note (project me API on nahi).
- PERMANENT RULE (user order): Flora chat/TTS me technical baat KABHI nahi bolegi — na code, na model naam, na "key gadbad". Brain fail ho to BasicTalk (local: greeting/haal/naam/time/date/shukriya/alvida/joke) phir warm fallback line. Asli kaaran sirf Settings > Keys check me.
- Training: warm system prompt (muskaan + pyar se, user ke tone me) + TTS pitch 1.1 / rate 0.95 (narm awaaz). VoiceEngine edit me ek baar listener line kat gayi thi — turant wapas lagayi, balance verify.
- Screen share bug ki jad: one-shot capture har baar projection STOP kar deta tha, aur wahi permission token dobara kaam nahi karta → dobara "frame nahi mila". Fix: persistent session (screen chhodne par hi stop), 3 retry, session mare to pyar se dobara permission + dialog khud khole, naya "App kholo aur dekho" flow (naam → app kholo → 3 sec → usi session se dekho).
- Comparison doc: docs/FLORA_MAYA_COMPARISON_20261028.md (server/app + AI use + gadbad table + kaun kis point par behtar).
- Audit: 4 files brace/paren 0/0; retry loop break-sahi; liveSession line order safe (state → openSession → use).
- Test entry: docs/FLORA_V220_TEST_ENTRY.md.

## v2.20 CI PASS + APK verify (2026-10-28 ~16:32 IST)
- Run 40676111594 completed SUCCESS (commit 796ee4e7). Artifact 12810807309, zip 59,057,604 bytes (exact match).
- Real APK: 111,265,998 bytes, sha256 351cb94d795d05f7b1abc5876d68774de5453171353149792bea4ee4f13a96aa, aapt2: com.flora.app, versionCode 42, versionName 2.20.0-rebuild.
- Dex content check: BasicTalk / discoverGeminiModels / liveSession strings classes5+8.dex me mile — naya code APK me confirm (size v2.19 jitna hi hona coincidence hai, sha alag hai).
- SwissTransfer upload in progress; link milte hi download page update hoga.

## v2.20 download page LIVE (2026-10-28 ~16:35 IST)
- SwissTransfer link: https://www.swisstransfer.com/dl/01a1822d-dd0b-73a9-8a2f-f258b6818b22 (106.1 MB, 15 din, 12 Nov 2026 tak; bina password, bina account/payment; 250 max downloads; 30-day selector par bhi confirmation 15 din + email extension).
- Download page update commit fb40bbfc → deploy flora-h6e17i8pq-webbuilder1.vercel.app READY (BUILDING se READY tak dekha); /download par live verify: v2.20.0-rebuild, size 111,265,998, SHA 351cb94d…, whats-new v2.20 (no technical errors + BasicTalk + auto-discovery + screen-share fix).

## v2.21 CODE DONE / CI PENDING (2026-10-28 ~16:40 IST, CEO plan M1: model download system)
- CEO appointment (user order): Muse Flora ka CEO — plan doc docs/FLORA_CEO_PLAN_MAYA_PARITY_PLUS.md (R1 mapping: server parity S1–S8, app parity, automation parity+, model system design, phase order M1→S2→P1→P5→S3→R3, boundaries: paisa alag approval, copy kabhi nahi, privacy).
- ModelDownloader (naya code, Maya behaviour se seekha, copy nahi): manifest 10-min cache, .part download, SHA-256 verify, rename + .ok marker, fail par .part delete, bundled fallback (server na mile to koi error nahi).
- ModelsScreen (Menu > Models): status + Download + Refresh, warm messages only. Wiring: enum MODELS + title + menu + branch (sab pehle declare, baad me use).
- Manifest source of truth: web/models/manifest.json (sirf 2 bundled entries; neural models training/licence ke baad hi aayenge — Maya ke models kabhi nahi).
- Host: Supabase Storage public bucket flora-models (URL FloraConfig me); bucket abhi bana nahi hai — tab tak app bundled list dikhayegi. Bucket dashboard browser task se banega (API ke liye service key nahi hai).
- Audit: 5 files brace/paren 0/0; manifest JSON valid; unused imports hataye.
- Test entry: docs/FLORA_V221_TEST_ENTRY.md.

## v2.21 test-entry naam collision — pakda aur sudhara (2026-10-28 ~16:42 IST)
- FLORA_V221_TEST_ENTRY.md pehle se maujood thi (purani v2.2.1 stale-fix ki) — usko overwrite NAHI kiya; nayi file docs/FLORA_V2_21_TEST_ENTRY.md banayi (v2.21 ke liye). Purani file ka content commit 277d1056 me v2.21 ke saath chala gaya tha (docs-only, code par asar nahi) — sahi test entry is naye commit me hai.

## v2.21 CI PASS + APK verify (2026-10-28 ~16:45 IST)
- Run 40677725705 completed SUCCESS (commit 277d1056). Artifact 12811835263, zip 59,073,025 bytes (exact match).
- Real APK: 111,282,441 bytes, sha256 6fa725ed5e851be53e2a92d02a7e55cbb6edc165349d38d19fe8ec14e310dbfa, aapt2: com.flora.app, versionCode 43, versionName 2.21.0-rebuild.
- Dex content check: ModelDownloader / ModelsScreen / flora-models strings classes4+5+10.dex me — M1 code APK me confirm.
- SwissTransfer upload in progress; link milte hi download page update hoga.

## M1 server LIVE (2026-10-28 ~16:45 IST)
- Supabase Storage bucket flora-models (PUBLIC) ban gaya; manifest.json root par upload hua; public URL khud fetch karke verify kiya: dono bundled models (face-match, wakeword-template) + note — 200 OK.
- Koi paisa nahi, koi paid resource nahi, project me aur kuch chheda nahi (pehle koi bucket tha hi nahi).
- Ab app ka ModelDownloader isi URL se manifest lega (bucket banne se pehle bundled fallback chal raha tha).

## v2.22 mega build chunk 1 — VoiceMatcher port + refresh-renew + Deepgram/Flora faisle (2026-10-28 ~16:50 IST, CI PENDING)
- Single-deploy order (user 16:47 IST): v2.21 link publish NAHI hoga; download page v2.20 par rahega; final release ek hi baar. Code push sirf CI check ke liye.
- Naam CLOSED: Flora hi rahega. Provider CLOSED: Deepgram hi rahega (key pending; tab tak system STT).
- VoiceMatcher.kt (validated Python protocol ka port): 16kHz PCM → 512/hop160 → preemph 0.97 → Hann 511 → rfft 1024 magnitude → TF mel matrix asset (513x128, TF se generate, 262,656 bytes) → log(max(1,mel)) → repeat-first x3 + window 4 stride 3 → VAD 528-d (exp>0.1) → encoder chunks of 2, states carry, d-vector = aakhri row. Threshold 0.50 STARTING GUESS (lock nahi).
- Assets (Apache-2.0, HF tflite-hub, creator ke nahi): conformer_tisid_medium.tflite (22MB), vad_long_model.tflite, vad_long_mean_stddev.csv, mel_matrix_f32.bin + README.
- VoiceVault.kt: enrolled vector KeyStore AES/GCM (PinVault pattern, alag alias) — kabhi plain/server/backup nahi; akela PIN unlock nahi karega (Guardian bhi chahiye).
- LicenceClient.refreshSession(): refresh_token grant se naya token (rotation); access token kabhi save nahi.
- Prefs: DEEPGRAM_API_KEY, VOICE_MATCH_THRESHOLD (0.50 default guess).
- Fixes is chunk me: encoder chunk ko [1,2,512] wrap karne ki galti sudhari ([2,512] sahi hai); VAD output shape dynamic se allocate; DEEPGRAM typo sudhara.
- Baaki is chunk me NAHI (agle chunks me): enrol UI (AudioRecord), Guardian wiring, Deepgram streaming client, P1 ke baaki items, P2–P5, U1–U3, brain-proxy step 2, S2 avatar catalog.
- Audit: 4 files brace/paren balance; mel matrix size exact; assets maujood. Phone test enrol/match UI ke baad (test entry agle chunk me).

## v2.22 chunk 1b — 22MB encoder APK se hatakar server download par (2026-10-28 ~16:55 IST)
- Wajah: 22MB conformer ke saath GraphQL push 499 se fail hua (payload bahut bada). Hal: encoder APK me bundle NAHI hoga — M1 model-download system se server (flora-models bucket) se aayega, SHA verify ke saath. APK ~22MB halka rahega.
- VoiceMatcher: loadModel pehle downloaded file (filesDir/models/voice-encoder/) dekhta hai, phir assets; isAvailable bhi dono raaste maanta hai.
- ModelDownloader.downloadedModelFile() naya public helper (valid .ok ke saath).
- Manifest v2: voice-encoder entry (url = bucket public URL, size 22134696, sha256 7781a316…) + bundled 2 entries.
- Bucket me upload (conformer tflite + manifest v2) browser dashboard task se hoga; tab tak matcher "server par abhi nahi" batayega (koi fake match nahi).
