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

## v2.21 upload DONE but HELD (2026-10-28 ~16:48 IST, single-deploy order)
- SwissTransfer link ban gaya (106.1 MB, 12 Nov 2026 tak) — par user ke 16:47 IST order ("sab ek sath, tab deploy") ke hisaab se ye link download page par NAHI dala gaya aur release ke roop me pesh NAHI kiya gaya.
- Link audit record me surakshit hai; final mega build (v2.22) aane par ye supersede ho jayega. Download page v2.20 par hi rahega.

## v2.22 chunk 2 — Naam-se-kaam + poochho-seekho + dead-code sweep (2026-10-28 ~16:57 IST, CI PENDING)
- ContactResolver: seekha hua → exact → ek → ambiguous (poochho) → not-found (poochho) → no-permission. Pehle wale ko chupke se chunna BAND.
- FloraApp call+sms: dono site resolver par; ambiguous par "Ram naam ke kai log mile — X, Y. Kisko? Naam bolo ya pehla/doosra bolo."; not-found par "poora number de do ya sahi naam batao, main yaad rakh lungi"; jawab par seekhna + poora kaam (runContactAction). Naya command aaye to purana pending khatam; "chhodo" par cancel.
- Learned contacts: DataStore JSON (sirf phone par, kabhi server nahi) — "Home" jaisa group bhi ek baar batane par seekh jayega.
- Sweep: same-file duplicate fun = sirf ScreenCapture overload nikla; outer one-shot captureFrame (v2.20 bug wala) ZERO caller nikla → HATA DIYA; stale comment bhi sudhara. findNumber ab sirf definition bacha hai (koi caller nahi).
- Crash-evidence: FloraApplication me handler — crash.log phone par (200KB cap), phir default handler (na ho to process kill — tooti haalat me zinda nahi rahegi).
- Plan update: competitor benchmark (Maya), "Maya me nahi, Flora me hoga" 9 items, production-ready bar, full permanent approval (paisa + irreversible bahar).
- Baaki (jhooth nahi): WhatsApp automation P4 me baaki hai; enrol UI + Deepgram streaming client agle chunks me; generic "wahan" clarification contacts ke bahar abhi format-ask tak hai.

## v2.22 chunk 3 — Enrol UI + Guardian voice wiring + Deepgram REST + Settings key field (2026-10-28 ~17:00 IST, CI PENDING)
- VoiceRecorder: 16kHz mono PCM16, N seconds, permission/init fail par null (koi fake sample nahi); audio kabhi save/upload nahi.
- VoiceEnrolScreen (Menu > Awaaz sikhao): 3 samples x 3 second → VoiceMatcher.enrol → KeyStore me average d-vector; enrolled status + clear bhi.
- Guardian wiring: Chat lock par PIN sahi hone ke baad, enrolled + matcher available ho to awaaz ka check (3s sample → match → unlock); na ho to pehle jaisa PIN-only. Face akela kabhi unlock nahi karta (pehle se rule).
- GuardianGate: voiceVerifiedThisSession + markVoiceVerified + canUnlockFull(pin, voiceEnrolled); purana canUnlock barkarar.
- DeepgramClient (REST step 1): WAV POST /v1/listen (nova-2, hi) → transcript; key khali ho to null. Streaming WS barge-in agle chunk me (is par barge-in ka daava nahi).
- Settings: Deepgram key field + load + save (baaki keys jaisa hi pattern).
- Audit: 5 files brace/paren balance; VOICE_ENROL wiring (enum+title+menu+branch) grep-verify.
- Baaki: Deepgram streaming WS, wake tune, job runner, auto-memory, voice-per-persona, P2–P5, S2/S3, R3.

## v2.22 chunk 4 — AutoMemory + voice-per-persona + job runner (2026-10-28 ~17:05 IST, CI PENDING)
- AutoMemory: sendChat se user ke fact-statements seekhe jate hain (naam, pasand, rishtedaar ka naam, rehne ki jagah) — MemoriesStore me, phone par hi, duplicate nahi. Sawaal (?) kabhi learn nahi hote; commands pattern match hi nahi karte.
- Voice-per-persona: VoiceEngine.setPersona + speak par apply — romantic: pitch 1.25/rate 0.90; baaki (flora/saathi/guru): 1.1/0.95. Wiring: romantic/normal voice commands + Settings persona row. Sirf tone, koi voice copy nahi.
- Job runner: JobStore (DataStore, ek routine = ek roz ka schedule) + JobScheduler (AlarmManager, exact mile to exact warna inexact fallback; WorkManager nahi — v2.19 me hata tha) + JobAlarmReceiver (due-flag SharedPreferences + notification + agle din reschedule) + BootReceiver reschedule + manifest (receiver + SCHEDULE_EXACT_ALARM).
- Routines screen: har routine par HH:MM field + "Roz is waqt chalao" + "Schedule hatao" + schedule status.
- CHAT khulte hi due-check: Auto Mode ON ho to due routines auto-run (wahi queue path), OFF ho to sirf message. Payment/delete steps routine me hain hi nahi; call/SMS steps pehle wale Auto Mode rule par.
- Audit: 8 files brace/paren balance; manifest receiver + permission grep-verify; String.format Java-static form (Kotlin .format route se bacha gaya).
- Baaki P1: wake tune, Deepgram streaming barge-in, brain-context me memories ka istemal; phir P2–P5, S2/S3, R3, EK final deploy.

## v2.22 chunk 1b/2/3 CI FAIL — root fix (2026-10-28 ~17:10 IST)
- Wajah (job logs se, guess nahi): VoiceMatcher.kt me `import java.io.File` missing tha — chunk 1b me downloaded-encoder branch aane par `File?` return type unresolved hua, aur usi se lines 85-87 ke overload/length errors cascade hue (ek hi jad, teen errors).
- Fix: sirf ek line — `import java.io.File` (FileInputStream/FileChannel pehle se the). Brace/paren balance verify.
- Is fix ke baad chunk 4 ka run (a89072f8) bhi isi code par chalega — uska result aane par dobara check hoga; fail ho to wahi job-log route.

## v2.22 chunk 5 — Brain-context memories (2026-10-28 ~17:15 IST, CI PENDING)
- BrainClient.ask(prompt, memoryContext = "") — Gemini + Groq dono lanes me memory line judti hai (sirf tab istemal, kabhi seedha repeat nahi). Default "" se purane callers (routine steps) pehle jaise hi.
- FloraApp chat path: brain.ask se pehle MemoriesStore se aakhri 20 facts padhe jate hain (phone par hi, server par save nahi hote); khali hon to kuch nahi judta.
- Privacy: facts sirf is phone par hain; brain ko sirf us waqt ke jawab ke liye context milta hai (prompt me), koi alag sync nahi.
- Audit: 2 files brace/paren balance; brain.ask callers grep-verify (chat path memory ke saath, routine paths default).
- Baaki P1: wake tune, Deepgram streaming barge-in; phir P2–P5, S2/S3, R3, EK final deploy.

## v2.22 chunk 6 — Wake tune: user-adjustable sensitivity (2026-10-28 ~17:20 IST, CI PENDING)
- Pehle threshold hardcoded 0.60 tha (sirf shuruaati guess). Ab FloraPreferences me wakeThreshold (default 0.60f, float key) + Settings me Kam (0.70) / Medium (0.60) / Zyada (0.50) buttons.
- WakeWordService har 2s window par fresh threshold padhta hai — Settings badalte hi lagu, restart ki zaroorat nahi; read fail ho to 0.60 fallback.
- Imaandari: ye abhi bhi tuning ka pehla kadam hai — asli phone-speech tune tab hoga jab user "Hey Flora" recordings dega (tab tak false trigger possible hai, ye baat service comment me khuli hai).
- Audit: 3 files brace/paren balance; sensitivity row Settings me wakeStatus ke neeche, enrol button se pehle (grep-verify); declaration order: wakeThreshold state launcher se pehle wale block me.
- Baaki P1: Deepgram streaming barge-in; phir P2–P5, S2/S3, R3, EK final deploy.

## v2.22 chunk 4b CI FAIL — root fix (2026-10-28 ~17:30 IST)
- Wajah (job log, d5aaead6): FloraApp.kt:2203 `Unresolved reference: voice` — chunk 4 me Settings ke persona row me `voice.setPersona(id)` likha tha, par wo row SettingsScreen composable me hai jiske params me `voice` tha hi nahi (voice sirf main FloraApp composable me hai). Pehle ke fail (VoiceMatcher import) ne is error ko chhupaya hua tha — ek error fix hote hi agla dikha.
- Fix: SettingsScreen signature me `voice: VoiceEngine` param + call site par `voice` pass + app-start LaunchedEffect me `voice.setPersona(prefs.persona())` (taaki restart ke baad bhi persona ki awaaz wahi rahe, sirf command/Settings switch par nahi).
- Audit: SettingsScreen ka ek hi call site (grep); VoiceEngine import pehle se tha; SettingsScreen range me `voice.` ka ek hi use; brace/paren balance.

## v2.22 chunk 7 — Deepgram streaming barge-in (2026-10-28 ~17:35 IST, CI PENDING)
- Naya dep: OkHttp 4.12.0 (build.gradle.kts) — WebSocket ke liye. Baaki deps unchanged.
- DeepgramStreamClient: wss /v1/listen (nova-2, hi, interim) + Token auth; sirf FINAL non-blank transcript callback; audio kabhi save nahi.
- BargeInController: AudioRecord 16kHz mono + WS stream; permission/key/mic-init fail par false (chupchaap no-barge-in); 30s auto-stop; stop idempotent.
- Wiring (FloraApp): MainActivity ke onSpeechStart/onSpeechDone ko OVERWRITE nahi — chain kiya (pehle unka kaam, phir barge-in arm/disarm). TTS shuru hote hi, continuous mic OFF ho aur Deepgram key ho to arm; FINAL milte hi TTS roko + transcript ko naya command banao (sendChatHolder se). Stop button par bhi disarm.
- Imaandari: continuous mic ON ho to mic uska hai — barge-in tab kaam nahi karega (mic fight se bachne ke liye). Wake Word ON hone par mic share par nirbhar karega — fail ho to chupchaap no-barge-in. Bina Deepgram key ke ye feature bilkul nahi chalega (REST client ki tarah, koi fake nahi).
- Audit: 3 naye/badli files brace/paren balance; Stop button edit wahi CHAT Stop button hai (grep + context verify); hooks chain (prevStart/prevDone) verify.
- P1 POORA (enrol UI, Guardian voice, Deepgram REST+streaming, job runner, auto-memory, voice-per-persona, wake tune, brain-context). Baaki: P2–P5, S2/S3, R3, EK final deploy.

## M1 server — manifest v2 + voice-encoder LIVE (2026-10-28 ~17:40 IST, khud verify kiya)
- Bucket flora-models me: manifest.json v2 (3 models: face-match + wakeword-template bundled, voice-encoder bundled=false) + conformer_tisid_medium.tflite.
- Khud ka check (browser report par bharosa nahi): manifest public URL se version 2 + voice-encoder entry (sizeBytes 22134696, sha256 7781a316...) mili; conformer URL par HTTP 200 + Content-Length bilkul 22134696.
- Iska matlab: app ka ModelDownloader ab voice-encoder ko bucket se SHA-verify karke download kar sakta hai — enrol/voice-match ka server wala hissa poora. Koi paisa nahi laga.

## v2.22 P2-F10 — Notification listener, READ ONLY (2026-10-28 ~17:50 IST, CI PENDING)
- FloraNotificationListener (NotificationListenerService, manifest me BIND_NOTIFICATION_LISTENER_SERVICE ke saath): aane wali notification ka title+text (max 200 chars) sirf is phone par DataStore me (max 20, purani auto-hat). Flora ki apni + ongoing notifications ignore. Dismiss/reply/action KABHI nahi — read-only hi hai.
- NotificationsScreen (Menu > Notifications): list (app label + time + title + text), access ON/OFF status, Settings kholne ka button (access user khud dega, Flora khud ON nahi kar sakti), Refresh, ek tap par list saaf.
- Privacy: notification me OTP/private message bhi ho sakta hai — isliye list sirf phone par, server par kabhi nahi; auto-announce NAHI (chupchaap bolna private content leak kar sakta hai). Voice se "notification batao" poochhna agle chunk me.
- Audit: 4 files brace/paren balance; enum+title+menu+branch+manifest grep-verify; FloraApp.kt me NOTIFICATIONS ke baaki matches sirf POST_NOTIFICATIONS permission wale hain (alag cheez).

## v2.22 chunk 4c CI FAIL — root fix (2026-10-28 ~18:00 IST)
- Wajah (job log, d7b186e0): VoiceEngine.kt me platform declaration clash — `var persona` ka generated setter aur mera haath ka `fun setPersona()` dono ki JVM signature ek (setPersona(String)V). Ye galti chunk 4 ki thi, pehle do errors ke peeche chhupi thi.
- Fix: fun hataya, 4 call sites ko property assignment (`voice.persona = ...`) par laaya. Sed ke greedy match ne startup wali line ka `catch (e: Exception)` bigaad diya tha — use haath se theek kiya (grep se verify: ab koi `voice.setPersona` call nahi bacha).
- Seekh (AGENTS.md me bhi): Kotlin `var` ke saath usi naam ka setter fun kabhi mat likho; sed me `.*` greedy hota hai — replacement ke baad har line grep se padho.

## Website fix — coordinator audit + parent verify + push (2026-10-28 ~18:00 IST)
- Coordinator (subagent) ne web/ ke 9 pages audit karke fix kiye (working copy only): signup (stale app-link note + confirm dono case), login (auto-redirect account), account (POORA REWRITE — purana licence-verify call device_id "" ke saath hamesha 400 deta tha, ab apni UTR list + logout), pay (link-first warning), pricing/index/download/admin (stale notes/nav/overclaim), admin-draft (redirect-notice, delete nahi).
- Parent verify (code se, guess nahi): licence-verify index.ts me `!device_id → 400` CONFIRMED; admin-approve me `!users.device_id → 400 "user device unknown"` CONFIRMED; admin-phase-a SQL me payment_submissions par user INSERT/SELECT-own + admin SELECT/UPDATE policies CONFIRMED — isliye naye account/admin flows policy-sound hain.
- N4 (coordinator ka app finding, parent ne fix kiya): FloraApp lock text "sirf PIN se" stale tha (chunk 3 voice wiring ke baad PIN + voice) — ek line theek ki.
- Baaki dashboard se: confirm-email ON/OFF, Site URL/redirects, owner is_admin flag (browser audit task chal raha hai, uski report aane par). Function gap (website-only approve 400) ka permanent fix (signup par users row ya approve me website path) abhi pending faisla — short-term copy "pehle app link karo" hai.

## v2.22 chunk 7b CI FAIL — root fix (2026-10-28 ~18:10 IST)
- Wajah (job log, 7200f35e): FloraApp.kt:172 `Unresolved reference: appContext` — chunk 7 me bargeIn ka remember appContext ki declaration (line ~200) se PEHLE likh diya tha. Wahi purana declaration-order trap (v2.13/v2.17 wali galti ka naya roop).
- Fix: bargeIn declaration ko appContext ke turant BAAD le jaya gaya; isse pehle ke koi use nahi hai (hooks LaunchedEffect ~430 ke baad, Stop button ~1576). Balance verify.

## v2.22 P2-F10 poora — notification voice query (2026-10-28 ~18:20 IST, CI PENDING)
- sendChat me "notification batao / notifications batao / koi notification aayi" branch: NotificationStore se aakhri 3 (title warna text, 100 chars) bolkar + chat me likhkar. Khali ho to access ON karne ka raasta batata hai.
- Privacy: sirf user ke poochhne par bolta hai — auto-announce kabhi nahi (private content apne aap bolna leak hota).
- Audit: branch normal-mode block ke baad, call block se pehle (return ke saath, brain tak nahi jaata); brace/paren balance.

## v2.22 P2 audit — F4 PARTIAL, F7/F8 WIRED (2026-10-28 ~18:25 IST, code check only, phone test pending)
- F4 general tap/type/swipe — PARTIAL: FloraAutomationService me primitives poore hain (clickText/clickAnyText, setTextOnFirstEditable/typeIntoFocused, swipeUp/swipeDown via dispatchGesture, scroll, goBack/goHome; manifest me BIND_ACCESSIBILITY_SERVICE registered) — par sendChat me general voice commands ("is par tap karo", "neeche scroll karo", "type karo X") wired NAHI hain. Sirf 2 flows primitives use karte hain: ChatGPT image prompt + Play Store install. Fix path: general commands ko Auto Mode gate + confirm ke saath sendChat me jodna (naya feature, isliye R3 se pehle alag item me banega, chupke se nahi).
- F7/F8 continuous vision — WIRED: LiveScreen me persistent MediaProjection session (v2.20) + continuous loop — max 10 frame/session, 10s gap, har frame VisionClient (Gemini key) ko; session sawaal ke liye zinda rehta hai, dispose par stop. Frame sirf user ke sawaal ke liye jaata hai (server par continuous stream nahi). Phone test (permission dialog + asli vision jawab) abhi pending hai.

## v2.22 P2-F4 poora — general tap/type/swipe voice commands (2026-10-28 ~18:30 IST, CI PENDING)
- sendChat me naye commands: "tap karo X" / "click karo X" / "X par tap karo", "scroll up/down karo" (upar/neeche), "type karo X", "back/wapas jao", "home jao" — sab FloraAutomationService primitives par.
- GATE: Accessibility OFF ho to sirf guidance (execute nahi); Auto Mode OFF ho to execute nahi (sirf "Auto Mode ON karo" batana); har chalaya kaam Auto Log me. Koi chhupa execution nahi.
- Audit: prefix lengths verify (tap 9, click 11, type 10, " par tap karo" 13); sab service calls Boolean; brace/paren balance.

## v2.22 P3-F12 poora — research voice commands (2026-10-28 ~18:35 IST, CI PENDING)
- ToolsScreen me search/study/define/stock pehle se the, awaaz se nahi chalte the. sendChat me jode: "search karo X", "X search karo", "matlab batao X" / "X ka matlab", "stock price X" / "X ka share price", "study karo X" / "X par study" — bolna (500 chars tak) + chat me poora result.
- Sirf padhna hai, isliye Auto Mode gate nahi. Audit: prefix/suffix lengths verify (search 12/12, matlab 13/9, stock 12/14, study 11/10 — " par study" ka 9→10 fix push se pehle pakda gaya); brace/paren balance.
- NOTE (CI): 929f67b8 run wahi purana appContext error (line 172) se fail hua — ye b91ff250 me pehle hi fix ho chuka hai, naya kaam nahi hai. Website deploy (Vercel) isse affected nahi — wo READY hai.

## v2.22 P3-F11 poora — website starter voice (2026-10-28 ~18:40 IST, CI PENDING)
- "website banao X" / "website starter X" → SearchTools.websiteStarter (pehle se ToolsScreen me tha). Chat me poora starter, bolkar sirf summary — HTML sunana bekaar hota.
- Imaandari barkarar: starter local template hai, poori website/file-save nahi — Flora bolkar bhi wahi kehti hai. Prefix lengths verify (banao 14, starter 16); brace/paren balance.
- P3 survey note: F16 whiteboard ka code abhi ZERO hai (naya screen banega — bada item, baad me); F14 docs screen + email intent maujood hain, voice routing survey baaki; F13 image voice maujood hai, post step ka survey baaki.

## v2.22 P3-F14 poora — docs + email voice (2026-10-28 ~18:45 IST, CI PENDING)
- "note save karo X" → DocsStore.add (title = pehle 30 chars, body = poora text; sirf phone par); "documents dikhao" → aakhri 5 titles bolkar; "email bhejo <address>" / "email kholo" → mail app kholta hai (mailto chooser) — Flora kabhi chhupke email nahi bhejti, subject/message/bhejna user khud karta hai.
- Audit: prefix lengths verify (note 15, email 12); first() import pehle se maujood; brace/paren balance.

## v2.22 P3-F15 poora — market overview voice (2026-10-28 ~18:50 IST, CI PENDING)
- "market kaisa hai" / "market dikhao" / "share market kaisa hai" → Nifty 50 (^NSEI), Sensex (^BSESN), Bitcoin (BTC-USD) — wahi stockPrice/Yahoo raasta, har line ka sirf pehla hissa (per-symbol note repeat nahi), aakhir me ek baar live-pushti note.
- Sirf padhna hai, isliye Auto Mode gate nahi. Audit: brace/paren balance.
- P3 baaki: F16 whiteboard (code ZERO — naya screen), F13 image post step (image voice maujood hai; auto-post ka raasta abhi koi nahi — IG auto-post bada + risky item, confirm flow ke saath alag se banega, chupke se nahi).

## v2.22 P3-F16 poora — Whiteboard screen (2026-10-28 ~18:55 IST, CI PENDING)
- Nayi WhiteboardScreen.kt: ungli se draw (drag paths), 4 colors, Undo, Saaf karo, Save → gallery Pictures/Flora (MediaStore, PNG). Drawing sirf phone par banti hai, koi upload nahi. Wiring: Screen.WHITEBOARD + title + Menu entry + branch (Docs ke baad).
- Audit: dono files brace/paren balance; Save IO par, note main par; khaali canvas par Save mana karta hai.
- P3 baaki sirf F13 image post step (image voice maujood; auto-post ka raasta koi nahi — IG auto-post bada + risky, confirm flow ke saath alag se banega).

## v2.22 P3-F13 poora — image share with confirm (2026-10-28 ~19:00 IST, CI PENDING)
- "image share karo" / "aakhri image share karo" → Gallery/Flora ki aakhri image (MediaStore, DATE_ADDED DESC) ko ACTION_SEND share sheet se bhejta hai. App user khud chunta hai — share sheet hi confirm hai; Flora kabhi chhupke post nahi karti. Koi image na ho to pehle "image banao" ka raasta batata hai.
- Audit: MediaStore import joda gaya (pehle nahi tha — push se pehle pakda gaya); brace/paren balance.
- CI GOOD NEWS: b91ff250 (bargeIn fix) + 802199dc (notification query) dono SUCCESS — fail chain toot gayi hai.

## v2.22 P4-F3 poora — WhatsApp message voice (2026-10-28 ~19:05 IST, CI PENDING)
- "whatsapp karo <naam> <message>" → SMS wale raaste par (ContactResolver: seekha/exact/poochho; pending clarification bhi whatsapp action se chalta hai; naya whatsapp command purana pending khatam karta hai) → wa.me chat kholta hai, message bhara hua, Send user dabata hai.
- Imaandari: Flora kabhi chhupke WhatsApp nahi bhejti (koi official API nahi) — yehi raasta hai. 10-digit number par 91 lagta hai. Prefix length verify (whatsapp karo = 14); brace/paren balance.
- P4-F2 note: call voice-answer code pehle se hai (FloraInCallService: dialer role + callTalk pref → answer + speaker + greeting) — phone test hi baaki proof hai, naya code nahi.

## v2.22 P4-F20 poora — teach-by-doing (2026-10-28 ~19:10 IST, CI PENDING)
- "routine sikhao <naam>" → uske baad bole gaye commands execute NAHI hote, steps me darj hote hain ("Step darj: X" bolkar) → "routine khatam" par RoutineStore me save (wahi Routines screen + runner) → "routine chhodo/cancel" par discard. Khaali steps par save nahi. Nayi sikhao purani khatam kiye bina nahi.
- Audit: prefix length verify (routine sikhao = 15); teaching check sendChat ke shuru me (pending se pehle, taaki steps execute na hon); Routine(name, commands) + load/save signatures verify; brace/paren balance.
- P4 baaki: F21 PC link — phone-only code se nahi banega (PC companion + pairing design chahiye), isliye pending rakha hai, jhootha "ho gaya" nahi.
- CI: 3a5cd20b (F4) bhi SUCCESS — green chain jaari.

## v2.22 P5-F9 poora — number-guessing game voice (2026-10-28 ~19:15 IST, CI PENDING)
- "game khelo" → 1-10 number, guess par chhota/bada hint, jeet par tries ke saath badhai, "game band karo" par number batakar band. Poora on-device, koi key/network nahi. Game ke beech baaki commands normal chalte hain (game background me rehta hai); pending clarification se game ko naya command mana jaata hai.
- Audit: brace/paren balance.
- CI: 005ebe52 (F12 research) bhi SUCCESS — green chain jaari.
- P5 survey: Home relay code ZERO, game ZERO tha (ab game hai); dedup/weak-network/learning ke code-level survey baaki.

## v2.22 P5-U1+U3 poora — memory dedup + sudhaar (2026-10-28 ~19:20 IST, CI PENDING)
- AutoMemory.learn ab: exact duplicate skip (U1, pehle se tha) + nayi baat purani ki jagah leti hai (U3 — "mera naam Y hai" aane par "Naam: X" update hokar "Naam: Y" hota hai, dono saath nahi rehte). Key match sirf "Key: value" wale facts par; bina colon wale free notes kabhi overwrite nahi hote.
- Audit: MemoriesStore.update(index, text) signature + pairs order verify; update me index guard hai (stale index par crash nahi). Chhoti seema: learn ke read aur update ke beech user ne memories badli hon to galat index update ho sakta hai — phone par ek user ke liye na ke barabar, aur guard crash rokta hai.
- U2 weak-network verify (koi code change nahi): brain fail par BasicTalk fallback pehle se hai; search fail par seedha non-technical message pehle se hai.

## v2.22 P5-F22 phone half poora — Home screen + honest voice (2026-10-28 ~19:25 IST, CI PENDING)
- HomeScreen: safety text (trained electrician + MCB OFF, 5A/channel, <10A board, green = EARTH, phase nahi), box join code phone par save + hatao, status. Wiring: Screen.HOME + title + Menu + branch.
- Awaaz se ghar ka light/pankha mangne par imaandari wala jawab: box na juda ho to jodne ka raasta; juda ho to bhi "server sync baaki hai, isliye nahi chalega". Koi relay chhupke kabhi nahi chalega.
- Baaki: S4 server sync (box ↔ server) — uske bina control adhoora hai, isliye F22 ka server half pending rakha hai.
- CI: cc4c851a (F14) + bfb5bbcd (F11) bhi SUCCESS — green chain jaari.

## v2.22 S2 poora — Avatar catalog (2026-10-28 ~19:30 IST, CI PENDING)
- AvatarScreen (Menu > Avatar): 5 on-device styles — gulabi (default, pehle wale rang), neela, hara, baingani, sunehra. FloraOrb(busy, avatar) ab rang param leta hai (purani calls default se chalengi); chuna hua rang prefs me + turant Baat orb par lagta hai. Sirf rang badalta hai — awaaz/persona wahi; koi download, koi creator asset nahi.
- Audit: 4 files brace/paren balance; enum + title + Menu + branch wiring grep-verify.

## v2.22 S3 + brain-proxy step 2 — function code ready (2026-10-28 ~19:35 IST, DEPLOY + KEYS PENDING)
- brain-proxy step 2 (repo copy): JWT gate lag gaya — anon client se JWT user (401 agar na mile), body user_id JWT se match nahi to 403. Gemini → Groq order, 4000 cap, 503 jab keys na hon — sab barkarar. JWT pattern send-push/admin-list wala hi hai.
- tts-proxy (nayi function, repo copy): POST {text} + JWT gate → Deepgram TTS audio/mpeg; key unset ho to 503; text cap 1000; text kabhi log nahi.
- PENDING (jhootha "live" nahi): dashboard editor se deploy + GEMINI/GROQ/DEEPGRAM keys set karna (owner ka kaam) + uske baad app wiring. Aaj app proxy se wired nahi hai aur system TTS par hai — isliye ye code kuch todta nahi.

## R3 prep — security scan (2026-10-28 ~19:40 IST, code-only)
- App code me koi hardcoded API key nahi mili (BrainClient URLs $key prefs se aate hain). Web pages me sirf Supabase publishable key hai (public by design; service-role kahin nahi).
- Manifest permissions review: INTERNET, CAMERA, CALL_PHONE, SEND_SMS, RECORD_AUDIO, FOREGROUND_SERVICE(+MIC), READ_PHONE_STATE, READ_CONTACTS, FINE_LOCATION, POST_NOTIFICATIONS, BOOT_COMPLETED, EXACT_ALARM, IGNORE_BATTERY — sab features se justified. DISABLE_KEYGUARD / SYSTEM_ALERT_WINDOW nahi hain (standing exclusions barkarar).
- CI: 03359326 (WhatsApp) bhi SUCCESS — green chain jaari.

## R3 prep — network/privacy scan (2026-10-28 ~19:45 IST, code-only)
- App se jaane wale hosts (16): apna Supabase, Gemini/Groq (brain, user key), Deepgram (STT/TTS), DuckDuckGo/Tavily (search), Open-Meteo+geocoding (weather), Frankfurter, DictionaryAPI, Yahoo Finance, Pollinations (image prompt), Play Store/wa.me/YouTube/Google (user-triggered intents, data upload nahi).
- Koi analytics/crash-reporting third party nahi; screen frame sirf user ke sawaal par uski apni Gemini key se jaata hai (opt-in, continuous stream nahi) — privacy rule barkarar.

## R3 law audit — Privacy text ke 2 stale daave theek kiye (2026-10-28 ~19:50 IST, CI PENDING)
- Point 4 galat tha ("Face screen match nahi karti") — FaceScreen me FaceMatcher enrol/match hai (on-device). Ab sahi hai: chehra sirf phone par match hota hai, photo server par nahi jaati.
- Point 7 galat tha ("koi romantic persona nahi") — v2.18 se romantic tone hai. Ab sahi hai: romantic mode sirf tone hai, explicit content nahi.
- OPEN (owner faisla, jhootha naam nahi likha): IT Rules ke hisaab se grievance officer chahiye — Flora me abhi sirf support email hai (flipify.com@gmail.com). Flow99 me bhi yehi PARKED hai; Flora ke liye bhi owner ka faisla chahiye.
- NOTE (proxy wiring ke baad badalna hoga): Privacy point 1/3 aaj sahi hain (app proxy se wired nahi hai) — jab brain/TTS proxy wiring hogi, tab prompts/text server jaane ka zikr jodna hoga.

## Supabase dashboard audit + Site URL fix (2026-10-28 ~17:41 IST)
- Dashboard audit (read-only): Email provider ON, signup ON, Confirm email ON (signup ke baad email link zaroori), 7 functions live, payment_submissions + admin_audit policies maujood, users 3 rows, 0 admin.
- Site URL fix DONE (dashboard): purana flora-2318h9ew5-webbuilder1.vercel.app -> naya stable https://flora-henna-alpha.vercel.app (save + reload se verify). Stable domain live site dikhata hai (text-fetch verify).
- BAAKI: Redirect URLs me abhi bhi sirf purana deploy URL hai — confirm default Site URL par jaata hai, isliye flow nahi tootega; purani entry ki safai agle dashboard task ke saath hogi.
- Admin abhi bhi 0 — owner signup + confirm + app link ke baad hi is_admin set hoga.

## v2.22 mega-build CI — FULLY GREEN (2026-10-28 ~17:42 IST, API-verified)
- Aakhri run c22dee2e (R3 law fix) bhi SUCCESS. P1–P5, S2, S3, R3-law-fix — sab code runs green; koi fail/in-progress nahi bacha.
- Baaki sab user/dashboard/phone-gated hai (admin, phone tests, S3 deploy+keys, grievance faisla, stale redirect safai) — code-side kuch pending nahi.

## Admin flag — INSERT FAIL (2026-10-28 ~18:15 IST, dashboard SQL)
- Owner auth user maujood hai (17:02 signup, email unconfirmed), par public.users row nahi hai.
- INSERT (id, is_admin) FAIL: users.device_id NOT NULL constraint (23502) — khaali device_id wali row ban hi nahi sakti. Koi fake device_id nahi dala gaya.
- Sahi raasta: email confirm -> app link (asli device_id ke saath row banegi, push-token-sync upsert se) -> phir UPDATE is_admin=true (tab chalega, kyunki device_id non-null hoga).
- Yehi constraint website-only approve ki problem ki jad bhi hai: bina device ke users row ban hi nahi sakti — permanent fix (i)/(ii) ka faisla abhi bhi pending hai.

## Admin flag — email confirm VERIFIED, row abhi bhi nahi (2026-10-28 ~18:17 IST)
- Email confirmed YES (28 Oct 18:15), website sign-in bhi ho gaya (18:16) — login ka raasta khul gaya.
- public.users row abhi bhi NAHI hai (3 doosri rows hain) — isliye UPDATE skip hua, koi flag nahi laga, koi fake device nahi dala.
- Aakhri step sirf app link hai: usse asli device_id wali row banegi, phir UPDATE is_admin=true turant chalega.

## Website redesign — admin + user panel (2026-10-28 ~18:25 IST, CI PENDING)
- Wajah (user sahi tha): admin me na nav, na logout, na refresh, na admin-status — approve fail hone par bhi pata nahi chalta tha kyun. User panel me sirf 2 button, payments auto-load nahi.
- admin.html FULL REWRITE (site style: cream/orange cards): nav header, Logout + Refresh sab, auto session load, NOT-ADMIN banner (approve kyun nahi chal raha + app link raasta), pending count badge, payment cards + inline result (alert ki jagah), users/usage/audit cards, ADMIN badge.
- account.html FULL REWRITE: nav header, auto-load, login card (bina login), Pro steps checklist (signup/confirm/pay status badges), payments auto-load + status badges (pending/approved/rejected), Refresh + Logout.
- Baaki 6 pages (index/login/signup/pricing/pay/download) par wahi nav header joda — har page se har panel ek click par.
- Logic change NAHI: wahi functions, wahi RLS notes, koi secret nahi. Sirf design + UX states.

## Table-count dobara verify — "empty" wali read GALAT thi (2026-10-28 ~18:26 IST)
- Dobara jaanch: public.users me 4 rows hain (khali nahi) — pichhli "0 rows/table empty" wali read galat thi (filter/grid misread). Koi delete nahi hua.
- Owner ki row abhi bhi NAHI hai; Auth users total 10 hain (public 4 se alag list — auto-create na hone ki wajah se expected hai).
- Admin flag abhi bhi app link par atka hai — row banne ke baad UPDATE turant chalega.

## Screen vision + session + continuous — ROOT FIX (2026-10-28 ~18:35 IST, CI PENDING)
- VisionClient BUG MILA (screen na dikhne ki asli wajah): hardcoded gemini-1.5-flash (retired) — har vision call 404 par girti thi. Fix: chat wali auto-discovery chain hi (flash-prefer, 404 par agla) — Maya ka bhi fixed model nahi tha, "latest free chain" tha.
- Session/permission loop ki jad: session composable `remember` me tha + screen chhodte hi DisposableEffect STOP karta tha; mari projection ka purana token dobara kaam nahi karta — isliye har wapasi par naya dialog. Fix: app-level LiveSessionHolder (screen badalne par zinda), asli maut MediaProjection.Callback se, explicit "session band karo" button. Process marne par Android dobara permission maangega hi (OS niyam).
- Continuous 10-frame cap HATA DIYA (user order): ab ROKO dabane tak chalega, har 10 sec ek frame (har frame = user key par 1 Gemini call, isliye gap rakha hai).
- brain-proxy (repo only): Gemini chain flash -> lite (deploy pending).
- NOTE: ye fixes code me hain, phone tak v2.22 final ke saath aayenge (single-deploy order barkarar).

## Model chain + voice compound + echo guard (2026-10-28 ~18:40 IST, CI PENDING)
- MODEL CHECK (user ki di hui key se live, key kahin save nahi): models-list 200 (35 models); gemini-2.5-flash 404 (do baar), 2.5-flash-lite 404, gemini-flash-latest network fail; CHALTE HAIN: gemini-flash-lite-latest (200), gemini-3-flash-preview (200), gemini-3.5-flash (200). "Baat nahi" ki jad: chain sirf 3 models tak jaati thi, working model chautha tha.
- Fix: BrainClient + VisionClient take(3)->take(6), har failure par agla (429 par bhi — quota per-model hota hai), fallback list me flash-lite-latest + 3.5-flash pehle. brain-proxy chain me bhi ye 2 models jude (repo only, deploy pending).
- VOICE DIAGNOSIS: (1) Compound command ("X kholo aur Y karo") pehle sirf pehla hissa chalata tha, baaki drop — ab compound parser: app kholo + 2.5s baad doosra hissa usi sendChat se (call/sms ka confirm niyam doosre hisse par barkarar). (2) Repeat loop ka sabse bada candidate: barge-in Flora ki apni awaaz ko transcript samajh kar dobara bolta tha — ab echo guard (aakhri Flora message se overlap par ignore). (3) Background me command execute NAHI hota by design (ek-confirmation niyam): service Flora ko aage la kar confirm karwati hai; Auto Mode ON ho to confirm bina kholega. STANDBY/TAKEOVER wiring sahi hai (double-listening nahi).
- NOTE: user ki key app ke Settings me user ko khud daalni hogi — assistant ne key kahin save nahi ki, phone par tabhi chalegi jab Settings me wahi working key ho.

## Live screen autonomy + voice-everything + call UI (2026-10-28 ~19:00 IST, CI PENDING)
- PLAN DOC: docs/FLORA_LIVE_SCREEN_AUTONOMY_PLAN.md — audit + Android niyam + flow + class plan + imaandari limits (bina tap ke permission kabhi nahi, secure windows black, key/password typed hi rahenge).
- Screen autonomy: ScreenCapture Session Context-based + openShared(); FloraApp level par screen states + launcher; Chat branches — "screen share chalu/band karo", "screen dekho / is screen par kya hai", "app kholo aur dekho <naam>", "continuous mic chalu/band karo", "mute karo". Permission dialog Flora khud kholegi; Allow ek tap OS ka niyam hai.
- LiveScreen typing HATA DIYA: app-naam TextField hata, mic button se bolo + auto-run (openAppAndLook). Session/permission ab FloraApp se aate hain (apna launcher hata).
- Call UI (pehle koi UI nahi tha — isliye option nahi dikhta tha): CallActivity (Flora se baat karao / Khud baat karo / Kaato) + CallHolder lambdas + InCallService UI-first (auto-answer HATA DIYA) + manifest entry + ConnectScreen text. Dialer role abhi bhi user ko khud set karna hai (system dialog).
- Pending-question auto-resume (Allow dabate hi bina dobara bole jawab) agle chunk me — plan me darj hai, is turn me daava nahi.

## Bina-Allow screen raasta (2026-10-28 ~19:10 IST, CI PENDING)
- User order: Allow tap khatam karo, one-time setup ke baad sab bol kar. Imaandari: MediaProjection Allow Flora khud NAHI daba sakti (OS hard rule, self-grant band hai).
- Raasta: Accessibility takeScreenshot (API 30+) — one-time setup (Settings → Accessibility ON + Android 13+ restricted-settings toggle, user khud, ek baar) ke baad har "screen dekho" bina dialog/tap ke. Sirf bolne par capture, kabhi passive nahi; secure windows black.
- Code: FloraAutomationService.screenshotJpeg() + FloraApp.lookAtScreen/LiveScreen.ask() me pehle try + fallback session path + "screen share chalu" par dialog skip. Service header ka purana "never screenshotted" comment naye rule se badla gaya.

## Full-autonomy chunk 1 (2026-10-28 ~19:25 IST, CI PENDING)
- Encrypted memories: MemoryCrypto (KeyStore AES/GCM) + MemoriesStore value encrypt/decrypt; purana plain data migration-safe (decrypt fail → plain padho, agle save par encrypt). File copy hone par bhi sirf Flora padh sakti hai.
- Background screen-Q&A: BackgroundMicService.handleHeard me screen sawaal (screen/is app/kya karu) par accessibility screenshot + vision + TTS jawab, app aage laye bina. Accessibility OFF/key missing ho to seedha batao. Execution commands abhi bhi bring-forward + confirmation (rule nahi toota).
- Website dropdown: 8 pages (index/signup/login/account/pay/pricing/admin/download) par "Sab pages" dropdown — saare pages ek jagah se. admin-draft ka apna nav nahi (admin par redirect hai).
- Baaki mega-order items (overlay mic button, code-likho branch, website-link hosting faisla, Chrome/GitHub/Vercel flow, journal/docs encryption, hisaab branch) agle chunks + user faislon par — plan doc me darj hoga.

## CI fail fix — screenshotJpeg compile errors (2026-10-28 ~19:30 IST, CI PENDING)
- Run 40698840209 (4fe5742e) job-log se pakde gaye 2 asli errors, guess nahi: (1) kotlinx.coroutines.resume internal hai (is coroutines version me) → cont.resumeWith(Result.success(...)) par laya gaya; (2) ScreenshotResult me bitmap property hoti hi nahi → hardwareBuffer + colorSpace se Bitmap.wrapHardwareBuffer() par laya gaya (baaki errors isi ke cascade the).
- AGENTS lesson: takeScreenshot API ko yaad se mat likho — ScreenshotResult = hardwareBuffer/colorSpace, bitmap nahi; continuation resume = resumeWith(Result.success()).

## G0 audit catch fix (2026-10-28 ~20:30 IST, CI PENDING)
- Maya full audit (MAYA_FULL_AUDIT_20261028.md) ne pakda: Flora ki accessibility config me canTakeScreenshot + canPerformGestures NAHI the — isliye naya screenshot raasta + gesture automation asli phone par fail hote. Dono attributes jod diye (Maya jaisi configuration, code copy nahi).
- Doosra catch: Flora manifest me backup exclusion nahi thi (default backup ON rehta) — android:allowBackup="false" jod diya (Flora ka apna attribute, Maya ki file copy nahi).

## Camera dekhna + CEO plan + G21 detail (2026-10-28 ~21:00 IST, CI PENDING)
- CameraLookScreen (naya): ScanScreen wala CameraX pattern — preview + capture (Pictures/Flora) -> JPEG bytes -> user ki Gemini key se VisionClient.askAboutImage -> jawab bol kar + screen par. Voice branches: "camera me kya dikh raha hai / camera dekho / camera se dekho / camera me kya hai" -> CAMERA_LOOK screen; menu entry bhi. Permission user khud Allow karega.
- Prepush checks (kotlin_prepush_checks): braces/parens balanced dono files; declaration order verify — scope/vision/state launcher se upar, capture AndroidView se upar; nullable guards (savedUri null, bitmap null, stream close); askAboutImage(prompt, jpeg, key) signature + suspend geminiApiKey() coroutine me — sab match. Local gradlew nahi hai (wrapper absent), isliye CI hi build gate hai (Flora practice).
- Voice samples (style selection): your_files/flora-voice-samples me 3 samples (Isabella/Lily/Myrtle, Hindi, speed 85). IMAANDARI: ye Muse TTS samples hain, Flora ki awaaz nahi — Flora me style G1 Deepgram selection se aayegi.
- Docs: FLORA_CEO_FULL_PLAN_20261028.md (G1-G18 order + action system + Maya-se-aage systems + G19 workarounds research) + FLORA_G21_DECISIONS_DETAIL_20261028.md (9 faisle detail + sifarish).

## Voice Sample 2 Lily fixed everywhere (2026-10-28 ~21:05 IST, CI PENDING)
- VoiceEngine.voiceSample (default "lily") + applyPersona: lily -> pitch 0.95/rate 0.88 (gahri+dheemi); isabella 1.05/0.90; myrtle 1.15/0.92; warna persona logic. Sample persona se upar (sample = voice, persona = tone/wording).
- FloraPreferences VOICE_SAMPLE key + voiceSample()/setVoiceSample() (default "lily", persona pattern par).
- FloraApp LaunchedEffect me voiceSample load; SettingsScreen me state (persona ke saath, launcher se pehle) + load + 3-sample selector row.
- Prepush checks: braces/parens balanced teeno files; declaration order — state launcher se pehle, load persona ke saath, voice/scope params upar; nullable nahi hai is change me; local gradlew absent, CI hi build gate.

## Keys UI + links + Deepgram check + live update + approve (ii) + direct APK (2026-10-28 ~21:35 IST, CI PENDING)
- BrainClient: checkKeys() ab Deepgram bhi check karta hai (Token auth, /v1/projects); naya checkKeysFor() bhare hue (bina-save) keys ko probe karta hai — Save user ke haath me, sahi ho ya galat.
- Settings: Gemini ke neeche AI Studio link, Groq ke neeche console link, Deepgram ke neeche console link; "Bhare hue keys check karo (bina save)" button; note me per-user key rule (har user ki apni key, usi ke liye).
- SetupWizard step 3 me keys hint (Settings > Keys, link se banao, check karo, phir Save).
- Settings me Roles buttons: Default assistant (VOICE_INPUT_SETTINGS) + Default dialer (MANAGE_DEFAULT_APPS_SETTINGS) — dono system settings kholte hain, tap user ka.
- Live update voice branch: "live update batao / taaza khabar batao / realtime update batao" -> Open-Meteo weather + DuckDuckGo search, free APIs, bol kar jawab.
- admin-approve option (ii): users row/device_id na ho to payment approve + NULL-device pro licence (website-path); licence-verify NULL-device row ko pehli phone-verify par claim karta hai (15-din wait nahi). Order: pehle licence, tabhi payment approved. Deploy dashboard editor se hoga (GitHub push se nahi).
- Direct APK: CI me Supabase Storage upload step (continue-on-error, build gate kabhi fail nahi); bucket SQL + secrets + page-flip doc FLORA_APK_DIRECT_DOWNLOAD.md; download.html par "aa raha hai" note (SwissTransfer abhi primary, pehli upload ke saath flip).
- Neural voice: plan doc FLORA_NEURAL_VOICE_PLAN_20261028.md (Kokoro primary Apache-2.0, Piper fallback MIT, Sherpa-ONNX glue, bucket-download, system-TTS removal sirf sample approve + phone test ke baad). System TTS is push me NAHI hataya gaya — pehle hatana Flora ko goonga kar dega.
- G19 examples doc + G21 CEO actions appendix likhe gaye.
- Prepush checks: braces/parens balanced (5 files); declaration order — Settings context/scope/key states UI se upar, live branch scope/appContext/searchTools branch se upar; TS me expires/userRow single-def, licErr block-scoped; local gradlew absent, CI hi build gate.

## G19 impossible → possible slices (2026-10-28 ~22:52 IST, CI PENDING)
- Shizuku bridge (naya automation/ShizukuBridge.kt): isReady() = pingBinder, hasPermission() = checkSelfPermission, grantPermission(pkg, perm) = Shizuku.newProcess pm grant + waitFor + exit 0, statusText() (3 haalat), requestPermission(1001). Har call try/catch me — Shizuku na ho to false/status, kabhi crash nahi. Dep: dev.rikka.shizuku:api:13.1.5 (build.gradle.kts). Manifest <queries> me moe.shizuku.privileged.api package (Android 11+ visibility).
- SettingsScreen Shizuku section (roles ke baad): state vars theme ke baad, launcher se pehle (line 2788-2790 vs launcher 2810); load block me statusText/isReady/hasPermission; UI: status text + "Shizuku permission do" + "Status refresh karo" + grant buttons (Notification/Camera/Mic — sirf ready+granted par) + Play Store link + setup guide (wireless debugging, reboot par dobara, user kabhi bhi band kar sakta hai).
- Voice branches (step executor, live-update ke baad): "shizuku status batao" (statusText bol do); "shizuku se permission do" (ready+granted to teeno grant kar ke result bolo, warna setup guide); "whatsapp call uthao" (accessibility ON to swipeUp + best-effort bolo — "agar WhatsApp call screen khuli thi to uth jayegi; har phone ka UI alag hota hai, guarantee nahi." — kabhi guarantee nahi; OFF to accessibility guide).
- Website publish (naya tools/WebsitePublisher.kt): suspend publish(html, slug, anonKey) — HttpURLConnection POST FloraConfig.SUPABASE_URL + /storage/v1/object/flora-sites/<slug>/index.html?upsert=true (apikey + Bearer anonKey, text/html), 2xx par public URL, warna failure reason. Website-banao branch ab result ko try/catch me filesDir/last-website.html me save karti hai. Voice branch "website publish karo": file na mile to "pehle website banao" bolo; anonKey + userId().take(8) se slug site-<8>; success par chat me URL + chhota spoken confirmation (poora URL nahi bola).
- IMAANDARI (ye slices phone par abhi TEST NAHI hue): Shizuku ke liye user ko Shizuku app install + wireless debugging se chalana hoga; WhatsApp swipe har phone/UI par ek jaisa nahi; publish ke liye flora-sites bucket + public-read/upload policy dashboard se lagani baaki hai (na ho to branch wajah bata degi). Call-uplink TTS guarantee aur WhatsApp call API abhi bhi mumkin nahi — speaker-loop + swipe hi imaandari wala raasta hai.
- Prepush checks: braces/parens balanced (5 files: FloraApp, ShizukuBridge, WebsitePublisher, manifest, gradle); declaration order — shizuku states launcher se pehle, load + UI uske baad; nullable guards — instance?.swipeUp, result.getOrNull, file.exists, try/catch har IO/Shizuku call par; return@launch label ke saath; local gradlew absent, CI hi build gate. Creator code/UI/assets copy nahi; koi key/password file me nahi.

## G19 fix (2026-10-28 ~23:00 IST, CI PENDING) — Shizuku newProcess private tha
- Pehla push (7426fa10) CI FAIL: "Cannot access 'newProcess': it is private in 'Shizuku'" (ShizukuBridge.kt:42, compileDebugKotlin).
- Root cause verify: Maven Central se api-13.1.5.aar download kar ke classes.jar ka method table khud parse kiya — Shizuku me koi public newProcess nahi hai (sirf pingBinder/checkSelfPermission/requestPermission/transactRemote/getSystemService raaste public hain).
- Fix: grantPermission ab SystemServiceHelper.getSystemService("package") + getTransactionCode + obtainParcel (interface-token wala Parcel) + binder.transact + reply.readException se grantRuntimePermission chalata hai — wahi kaam jo `adb shell pm grant` karta hai, sirf Shizuku binder ke through. Sab kuch try/catch me; phone test par transact protocol verify hona baaki hai.
- Prepush: braces/parens balanced (ShizukuBridge); baaki files me koi change nahi.

## 2026-10-28 ~23:05 IST — G19 fix2 (ShizukuBridge, main agent)
- Pehla error (7426fa10): `newProcess` private hai Shizuku API 13.1.5 me — public tareeka nahi.
- Doosra error (42565aaf): `getTransactionCode` ko 3 argument diye, function sirf 2 leta hai.
- Fix2: `getTransactionCode("android.content.pm.IPackageManager", "grantRuntimePermission")` — pehla `"package"` argument hataya. AAR method table se verify: `getTransactionCode(String, String)` public hai; `obtainParcel` 3-arg wahi raha (usme koi change nahi).
- CI result fix2 push ke baad verify hoga; green hone tak G19 done nahi mana jayega. Baaki 3 items (Settings UI, voice branches, WebsitePublisher) me is error ke alawa koi compile error nahi mila.

## Phase 1 — Core automation poora (chunk 2) (2026-10-28 ~23:20 IST, CI PENDING)
- Overlay mic service (naya automation/OverlayMicService.kt): foreground service + WindowManager overlay button (TYPE_APPLICATION_OVERLAY, 56dp, TOP|END), tap par MainActivity flora_mic extra ke saath aage aati hai + 500ms baad system recognizer shuru. Notification me "Band karo" action. SYSTEM_ALERT_WINDOW + FOREGROUND_SERVICE_SPECIAL_USE manifest me; specialUse subtype property ke saath. Bina overlay permission ke button nahi dikhega, crash nahi. Voice branches: "overlay mic chalu karo" (permission na ho to system settings kholta hai + guide bolta hai) / "overlay mic band karo".
- MainActivity: startMicRecognition() helper (RecognizerIntent hi-IN) — onMicRequest lambda + onCreate/onNewIntent flora_mic handling dono isi se. Overlay se aane par onCreate me 500ms delay (resume ke baad), onNewIntent me seedha.
- G3 VoiceInteractionService full (naya voice/FloraVoiceInteraction.kt + res/xml/voice_interaction_service.xml): FloraVoiceInteractionService (onReady/onShutdown) + SessionService (onNewSession) + Session (onShow par MainActivity aage laata hai). Manifest me BIND_VOICE_INTERACTION + meta-data. recognitionService jaanboojh kar nahi diya (system recognizer hi use hota hai). Settings ka "Default assistant banao" button pehle se system settings kholta hai — ab service maujood hai.
- "Code likho" branch (sendChat, G15): brain.ask se sirf code -> filesDir/code-<ts>.txt save + chat me dikhao, sirf confirmation boli jaati hai. Khali description par usage batata hai.
- Hisaab branch: "hisaab batao / mera hisaab batao / kya kiya / kya karna hai / ab tak kya hua" -> yaadein (aakhri 5) + routines + jobs (enabled, time ke saath) ka summary chat me + chhota spoken summary. Sirf padhna, koi action nahi.
- Pending-question auto-resume: pendingScreenQuestion state (screenLauncher se PEHLE, line 259 vs launcher 260+); lookAtScreen ke dono dialog-launch spots par pending = "screen dekho" set hota hai; Allow milte hi launcher callback voiceCommand.value = pending karta hai (sendChat dobara chalta hai, permission ab granted hai) — user ko dobara bolna nahi padta. Deny par clear.
- LicenceClient.refreshSession() wiring: prefs.linkedRefreshToken() getter (naya) + LaunchedEffect(Unit) me licence verify ke baad renew (naya token mile to setLink se save, rotation). Fail ho to chupchaap local mode — app-start kabhi nahi rukega.
- Prepush checks: braces/parens balanced (7 files; FloraPreferences ka 1-paren fark sirf block comment ke andar ka `(` tha — block comments strip karne par 197/197); declaration order — pendingScreenQuestion launcher se pehle, refresh wiring licence verify ke baad, mic helper launcher se pehle; nullable guards — pending ?.let pattern, intent?.extra, try/catch har service/intent call par. Local gradlew absent, CI hi build gate.
- Phone test baaki (Phase 8): overlay button dikhna + tap par mic, default assistant invoke par Flora khulna, auto-resume Allow ke baad, refresh rotation.

## Phase 2 — G18 encryption poora (2026-10-28 ~23:30 IST, Phase 1 CI green ke baad push)
- MemoriesStore ka decode/encode pattern (MemoryCrypto AES/GCM KeyStore, fail ho to plain fallback — data kabhi nahi khota) ab 7 aur stores par laga: JournalStore, DocsStore, RoutineStore, JobStore, SosStore, NotificationStore, AutoLogStore. Har store me decode() padhne par (purana plain bhi khulta hai — migration-safe) aur encode() likhne par.
- RoutineStore.load() me try/catch bhi joda (pehle JSONArray/getJSONObject bina guard ke the — corrupt data par crash hota tha; ab corrupt par emptyList milta hai).
- AutoLogStore: items flow + add + pruneOlderThan sab encode/decode; clear() "[]" hi rehta hai (khali marker, user data nahi). NotificationStore clear bhi "[]" hi (khali marker).
- Prepush checks: 7 files braces/parens balanced (strings + dono tarah ke comments strip karke); decode/encode har file me maujood; koi naya permission nahi; koi server/backup change nahi (DataStore sirf phone par, allowBackup=false pehle se).
- Phone test baaki (Phase 8): purana plain data khulna (migration), naya data encrypted dikhna (DataStore file inspect), routines/jobs/journal/docs/SOS/notifications/auto-log sab kaam karna.

## Phase 3 — Voice maturity (2026-10-28 ~23:45 IST, Phase 2 CI green ke baad push)
- (a) Wakeword default template: 6 clips (New_recording_28–33.wav) se prototype_mfcc.py ke mfcc_mean (12-d, DCT 0th skip — Kotlin WakeWordMFCC se step-by-step match) ka mean vector -> asset app/src/main/assets/wakeword/default_template.json (549 bytes, vector dim 12). WakeWordService.listenLoop ab enrolled template na hone par isi default template se chalega (enrolled template hamesha pehle; dono na hon to service chupchaap kuch nahi karti). Honest numbers (Python, phir se compute kiye): LOOCV held-out 0.732–0.896, negatives vs full template 0.232–0.314 — threshold 0.60 starting guess hi hai. IMAANDARI: koi TFLite neural model train NAHI hua (6 clips ship-grade neural model ke liye naakaafi hain); shipped detector MFCC template v0 hai. Kotlin/Python vector-match unit test abhi bhi pending hai; phone test Phase 8 me validate karega. Threshold Settings me user-adjustable hai (pehle se), default template ke saath bhi wahi threshold lagta hai.
- (b) SttLanes (naya voice/SttLanes.kt): hasDeepgramKey + transcribe — key ho to DeepgramClient.transcribe (REST nova-2 Hindi), warna null (caller system STT fallback lega). VoiceEnrolScreen par "Aakhri sample ko text me badlo (STT check)" button — key na ho to "Deepgram key chahiye — Settings me daalo" guide, warna suna hua text. Streaming barge-in lane pehle se FloraApp me wired hai (Flora bolte waqt Deepgram WS, key na ho to start false -> system STT jaari) — dono lanes ka fallback ab code me poora hai, key ka intezar nahi.
- (c) Guardian threshold UI: Settings me matchThreshold state (launcher se PEHLE, line 2940) + LaunchedEffect load + "Sakht (0.60) / Medium (0.50) / Dheela (0.40)" row wake sensitivity row ke neeche. Setter Float leta hai (setVoiceMatchThreshold(value)) — pehle String likhne ki galti pakdi aur sudhari.
- Prepush checks: braces/parens balanced (WakeWordService 27/27, SttLanes 7/7, VoiceEnrolScreen 31/31, FloraApp 1076/1076 — parens 2247/2247); declaration order — matchThreshold launchers se pehle, transcribeNote states block me; nullable guards — loadDefaultTemplate har galti par null, transcribe khali PCM/key par null; koi key file me nahi.
- Phone test baaki (Phase 8): enrol bina wake word chalna, default template par "Hey Flora" trigger, STT check button, threshold buttons ka asar.

## Phase 5 — Maya parity breadth (2026-10-28 ~raat, Phase 3 CI green ke baad push)
- G7 IN-APP BROWSER poora: tools/BrowserController.kt (BrowserNav pending url/action + nonce; normalize site/search; UI-thread-safe load/back/forward/reload/readText/links/clickLink; JS string decode) + ui/BrowserScreen.kt (WebView: JS on, DOM storage on, file access OFF, universal file access OFF, mixed content NEVER, Safe Browsing on; SSL error par kabhi chupchaap proceed nahi — warning dialog + user faisla; download har baar confirm dialog ke baad (DownloadManager, Downloads folder); URL bar, back/forward/reload, progress bar, error page + retry, session history list; password save ka option hi nahi hai). Screen.BROWSER + title + Tools menu + render branch. Voice: "browser kholo", "browser me <site> kholo" (khali site par sirf browser khulta hai), "browser me wapas/aage jao", "browser me reload karo", "ye page padh kar sunao / is page par kya hai" (JS title + body text, 600 chars spoken), "links dikhao", "pehla/doosra/teesra/chautha/panchva link kholo".
- G9 live wallpaper: automation/FloraWallpaperService.kt (dhadakta orb, sirf drawing — koi data/mic/network nahi) + res/xml/wallpaper_service.xml + manifest BIND_WALLPAPER. Voice "live wallpaper lagao" system chooser kholta hai (user khud chunta hai).
- G10 Home server half: tools/HomeSyncClient.kt (POST functions/v1/home-sync, anon key, fail par null) + supabase/functions/home-sync/index.ts (REPO ONLY — deploy Phase 4 dashboard se; home_boxes table SQL comment me; tab tak honest "received, not stored") + HomeScreen "Server se sync karo" button (fail par jhootha "jud gaya" kabhi nahi).
- G12 orchestrator: data/JobChainStore.kt (encrypted, Chain naam + routine naam list) + voice "chain banao <naam>: <r1>, <r2>" / "chain chalao <naam>" (Auto Mode ON par routineQueue me flat ho kar chalti hai, warna Routines screen khulti hai) / "chain dikhao".
- G15 code loop poora: code-likho (Phase 1) + website starter (pehle se) + chat se "website publish karo" branch (routine step ka mirror + chat message).
- G16 voice ops: "chrome kholo / github kholo / vercel kholo" — app ho to app, warna website (AppLauncher.findByName fallback openWebsite). Login user khud ek baar karega; password kabhi bola/store nahi.
- G8 PIN-escapable lock: ui/LockScreen.kt + Screen.LOCK + title + Setup menu + render branch (PIN sahi → chat; Back → chat; PIN set na ho to guidance, lock ka naatak nahi). Render me onUnlock/onBack dono CHAT par jaate hain — user kabhi phasta nahi.
- G11 consent-gated Termux bridge: manifest com.termux.permission.RUN_COMMAND + queries com.termux; voice "termux me <command> chalao" → termuxConfirm dialog (state line 211, confirm states ke saath) — "Haan" par hi com.termux.app.RunCommandService ko RUN_COMMAND intent jata hai; kabhi silent nahi. Termux install/permission na ho to imaandari wala failure message.
- G13 PC link: ui/PcLinkScreen.kt + Screen.PC_LINK + title + Tools menu + render branch; prefs PC_PAIR_CODE (sirf phone par). IMAANDARI: PC client app alag deliverable hai, abhi nahi hai — is screen se PC control ka daava nahi.
- G14 Google link sign-in: LicenceClient.linkWithGoogleIdToken(idToken) + FloraConfig.GOOGLE_WEB_CLIENT_ID (khali, default) + play-services-auth 21.0.0 + SettingsScreen googleLauncher + button GATED (ID khali ho to sirf setup guidance, sign-in shuru nahi). Dashboard step baaki hai: Firebase/Supabase me Web OAuth client banana — tab tak ye feature poora functional NAHI hai (code-ready, gated).
- WhatsApp read: "whatsapp padho" → NotificationStore se com.whatsapp ki aakhri 3 notifications sunao. Bhejna wa.me se hi hai (user Send dabata hai) — yahan sirf padhna.
- ROUTE-PARITY CHECK: Maya ke 45 voice destinations ke saamne Flora branches milaye — generic "<screen> kholo" map se sab screens cover hoti hain (chat, settings, tools, docs, whiteboard, home, avatar, live screen, journal, permissions, licence, auto log, models, awaaz sikhao, notifications, face, connect, privacy, terms, auto, routines, scan, gaadi, sos, lock, pc link) + pehle se maujood branches (yaadein, camera, scan karo, sos, photo lo, chrome/website, email, app kholo, market, search, study, note, documents). DROP (wajah ke saath): about (Flora me alag About screen ka order nahi — Settings me info maujood hai); advanced (Flora me koi chhupa advanced mode nahi — sab Settings me khula hai); marketing (Flora ka product feature nahi, user ka kaam nahi); maya_rules (Maya-specific rules engine — Flora me routines/chains uski jagah hain); settings_secret (chhupi settings ka concept Flora ke khule design ke khilaaf hai); settings_skills (alag skills screen nahi — Tools screen hi skills dikhata hai); vibes-as-separate-screen (Avatar screen hi vibes dikhata hai, alag screen duplicate hoti); settings_awareness (awareness ka alag toggle nahi — Live Screen + camera hi awareness hain); settings_backup (allowBackup=false by design — backup ka toggle dena galat hota); settings_behaviour (persona Settings me hi hai, alag screen duplicate hoti); settings_connectors (har connection apni screen par hai — Connect/Settings); settings_email (email bhejna Docs/Email branch se hota hai, alag settings screen nahi); settings_social (social connect ka order nahi hai); settings_whatsapp_groups (WhatsApp group auto-messaging CEO rule se kabhi silent nahi — isliye iska screen banane ka matlab hi nahi); settings_autoreply (CEO: never silent auto-reply — isliye auto-reply ka screen/feature DROP hai, yehi wajah); settings_subagents (G12 orchestrator = chains, alag sub-agent screen ka order nahi); settings_face_attempts / settings_face_lock (Face screen hi face lock dikhata hai); settings_lock_credentials (PIN Settings + Lock screen me hai); settings_screen_lock (Lock screen hi hai); settings_touch_guard (Touch Guard ka order abhi tak user se nahi mila — Phase 6 me dobara poochha jayega, chupchaap nahi banaya); settings_typing (typing indicator ka alag setting nahi — chat me hi dikhta hai); settings_voice_engine (voice sample + threshold Settings me hi hain); settings_battery / settings_sos (battery/auto-start Settings me, SOS apni screen par); settings_optional / settings_personal / settings_maya / settings_theme / settings_appearance (ye sab Settings ke andar hi hain — alag screens duplicate hoti). Flora-ahead items (call UI, encrypted memory, camera vision, keys UI/checks, live-update) regression check Phase 6 me hoga.
- Prepush checks: braces/parens balanced (FloraApp 1193/1193, parens 2472/2472; baaki 13 files bhi balanced); declaration order — termuxConfirm confirm states ke saath (line 211, dialog se pehle), googleLauncher Settings launchers ke saath (3407, use 3888 se pehle), Screen enum/titles/menu/render ek saath; nullable guards — BrowserController webView null par callback failure, readText/links null par "pehle page kholo" message, chain/termux khali input par usage message, HomeSync null par honest failure. Koi key/password file me nahi; koi creator copy nahi.
- Phone test baaki (Phase 8): browser khulna + SSL warning + download confirm + voice navigation, wallpaper chooser, chain chalana, lock khulna/band hona, Termux consent dialog, PC code dikhna, Google button gated guidance.
- CI FIX (Phase 5, pehli fail ke baad): run 40742816573 me processDebugResources fail hua — AAPT error: wallpaper_service.xml me android:description par literal string nahi chalti, reference chahiye hota hai. Fix: description @string/flora_wallpaper_description par laaya (strings.xml me naya string joda). Ye Phase 5 ki pehli fail thi, isliye raasta badalne ki zaroorat nahi thi — sirf attribute ka type sudhara.

## Phase 6 — R3 final audit, round 2 (2026-10-28 ~raat, Phase 5 CI green ke baad push)
- Audit round 1 (code scan): app code me koi hardcoded key/token/password value nahi (sirf DataStore key NAAM hain, value user ki); koi TODO/FIXME nahi; hosts 21, sab justified (Supabase apna backend, Gemini/Groq/Deepgram AI, DuckDuckGo/Open-Meteo/Yahoo/Tavily/Brave free APIs, wa.me, Play Store Shizuku link, GitHub/Vercel G16, Google search fallback, Pollinations image, Dictionary API, YouTube gaana) — koi anjaan host nahi.
- Permissions: 16 Android + Termux RUN_COMMAND — sab justified (location mausam, call/SMS confirm ke saath, camera, contacts, mic, overlay, exact alarm jobs, battery opt, boot, notifications, phone state, internet, foreground services). BIND_* services par hi hain, uses-permission me nahi — sahi.
- Licence flow (repo code): licence-verify NULL-device claim (lines 51–57) + admin-approve website_path (action payment_approved_website_path) dono repo me maujood hain. Deploy Phase 4 dashboard se baaki hai — live hone ka daava nahi.
- Download page (local): v2.20 SwissTransfer primary + imaandari wala "Direct download (aa raha hai)" note — koi dead link nahi. Phase 7 flip tabhi jab Phase 6 = 0 ho.
- FIX 1 — Privacy text galat tha: purana point 3 kehta tha "iske alawa koi chat/yaad/document server par nahi bheja jaata" — ye jhooth tha (chat sawaal + 20 yaadein Gemini/Groq ko jaate hain, khoj/mausam/market/vision apni services ko jaate hain). LegalScreens.kt dobara likha: 13 points — on-device encrypted stores, PIN KeyStore, licence check, AI chat flow, khoj/mausam/market/vision flow, mic/continuous/wake/Deepgram flow, browser flow, push token flow, camera/face, call/SMS confirm, 30-din retention, bachchon ki suraksha, support. Ab koi blanket "kuch server par nahi jaata" wala daava nahi hai.
- FIX 2 — Terms stale tha: "Live wallpaper aur conversation mode abhi available nahi hain" — galat (dono Phase 5/continuous mic me maujood hain). Terms point 1 sudhara.
- FIX 3 — ModelsScreen me user ko dikhne wala "Maya ke models" naam hataya — ab "kisi aur app ke models kabhi nahi" (benchmark ka naam UI me nahi, copy ka sawaal hi nahi).
- FIX 4 — Live-update chat gap: "live update batao" sirf routine executor me tha, sendChat me nahi — chat/voice se bolne par brain fallback par ja raha tha. sendChat me wahi branch jodi (mausam + khabar, chat message ke saath). Ab dono raaste ek hi kaam karte hain.
- Regression check (code level): CallActivity + FloraInCallService maujood; CameraLookScreen + VisionClient maujood; keys UI + "Bhare hue keys check karo" maujood; live-update dono raaston par; encrypted stores — MemoriesStore + 8 aur stores (journal/docs/routines/jobs/chains/SOS/notifications/auto-log) sab MemoryCrypto use karte hain.
- Audit round 2 (fix ke baad dobara scan): keys/TODO clean (sirf key naam, value nahi), hosts phir 21, braces/parens balanced (FloraApp 1204/1204, parens 2491/2491; LegalScreens 6/6; ModelsScreen 22/22).
- OPEN ISSUES: 0 (code + permissions + hosts + privacy + licence-flow + regression — sab band). Phone test (Phase 8) aur dashboard deploy (Phase 4) is audit ke scope se bahar hain — wo alag phases hain, isliye "0" ka matlab code-audit 0 hai, phone-verified 0 nahi. Ye farak imaandari se likha ja raha hai.
