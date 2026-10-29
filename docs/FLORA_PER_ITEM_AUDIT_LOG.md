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
- CI FIX 2 (Phase 5, doosri fail ke baad — ALAG error, wahi raasta nahi): run 40744006652 me compileDebugKotlin fail hua, sirf BrowserScreen.kt par — (1) SslError galat package me import tha (android.webkit ki jagah sahi package android.net.http hai; SslErrorHandler webkit me hi hai), (2) MIXED_CONTENT_NEVER fully-qualified form me resolve nahi hua, isliye WebSettings import karke short form par laaya. Pehli fail (AAPT/resources) aur ye fail (Kotlin imports) alag root cause hain — isliye ye andha retry nahi, nayi diagnosis ka naya fix hai.

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

## Phase 7 deliverable change — bucket route DEAD, release asset hoga (parent brief update, 2026-10-28 ~23:47 IST verified — LOCAL ONLY, agle code push ke saath jayega)
- flora-apk Supabase bucket route DEAD hai: free plan par file-size limit 50MB fixed hai (per-bucket aur global dono raise karne ki koshish fail; Pro bina user approval ke nahi liya jayega) aur APK ~111MB hai, isliye bucket me flora-latest.apk kabhi upload nahi hoga.
- Phase 7 par download page ko bucket URL par flip MAT karna. Uske bajay Phase 7 ka direct link GitHub Release asset hoga: main agent Phase 7 ke waqt workflow me release-upload step web UI se add karega (builder workflow files ko chhu nahi sakta — token me workflows scope nahi hai, ye builder ka kaam nahi hai) aur ek final release publish hogi; builder ka Phase 7 kaam download page ko us release asset ke link par le jana + live verify karna hoga.
- Single-deploy order barkarar hai: Phase 7 se pehle koi intermediate release publish nahi karni, aur download page tab tak v2.20 par hi rahega.
- Production plan (docs/FLORA_PRODUCTION_READY_PLAN_20261028.md) ka Phase 7 section isi ke saath update kar diya gaya hai (local copy; push agle authorized code push ke saath hoga — abhi docs-only push karne se toote hue tree par ek aur red CI run banega, isliye roka gaya hai).
- Download page ka maujooda "Direct download (aa raha hai)" note (Supabase Storage auto-upload ka zikr) ab stale hai — wo Phase 7 ke waqt release-asset wale sach ke saath badla jayega, usse pehle nahi.

## Phase 5 CI follow-up — Task B (wallpaper AAPT fix) pehle se ho chuka hai, dobara push nahi kiya (verified remote par)
- Parent ke follow-up task me run 40742816573 (head be30b303) ki pehli fail ka fix manga gaya tha: wallpaper_service.xml line 5 par literal Hindi string → strings.xml me string resource + @string reference.
- Remote par verify kiya (GitHub API, 2026-10-29 ~00:02 IST ke baad): wallpaper_service.xml me pehle se `android:description="@string/flora_wallpaper_description"` hai aur strings.xml me `flora_wallpaper_description` string maujood hai — ye fix commit 70e1d3a9 me push ho chuka hai (run 40744006652). Isliye usi fix ko dobara push karna no-op hota — koi push nahi kiya gaya, aur "koi aur file mat chhedo" wali shart ka bhi paalan hua hai (wallpaper ke liye koi aur file nahi chhedi gayi).
- Latest runs abhi bhi wahi teen hain (40745112081 e2f2cc24 failure sabse upar) — uske baad koi naya push nahi hua hai. Current blocker wahi hai jo pichhli report me tha: run 40745112081 me BrowserScreen.kt:203:61 `Unresolved reference: MIXED_CONTENT_NEVER` (SslError wali galtiyan fix ho chuki hain, sirf ye ek symbol baaki hai). Is par aage ka faisla parent ka hai (literal -1 workaround ya line hatana) — builder ne teesri fail ke baad rule ke mutabiq ruk kar report kiya hai, chautha push apni marzi se nahi kiya hai.

## 2026-10-29 ~00:10 IST — Phase 5 fix3 (main agent, builder ke stop ke baad)
- Builder final report: Phases 0–3 green; Phase 5 teen baar fail ke baad rule ke mutabiq STOP + report. Phase 6 fixes local-only (push nahi), Phase 7 nahi hua.
- Teesri fail ka asli root cause: `MIXED_CONTENT_NEVER` naam ka constant Android me HAI HI NAHI — official naam `MIXED_CONTENT_NEVER_ALLOW` hai (value 1, wahi never-allow behavior; Android docs + android.jar strings se verify). Isliye line 203 qualified hone ke bawajood fail ho rahi thi — naam galat tha, qualifier nahi. (Main agent ki builder ko pehli hint "qualify karo" galat thi — line pehle se qualified thi.)
- Fix: BrowserScreen.kt line 203 par `WebSettings.MIXED_CONTENT_NEVER` → `WebSettings.MIXED_CONTENT_NEVER_ALLOW`. Diff vs remote e2f2cc24 = sirf ye ek line; tree me koi aur bare MIXED_CONTENT_NEVER nahi.
- Push: commit 5273fbc2 (sirf BrowserScreen.kt; Phase 6 ke local edits isme nahi hain, wo Phase 5 green ke baad push honge). CI result pending — green hote hi Phase 5 gate pass, phir Phase 6 push + audit, phir Phase 7.

## Phase 6 — R3 audit round 3, continuation (2026-10-29 ~raat, Phase 5 fix3 green ke baad push)
- Push set (remote 5273fbc2 ke saamne blob-diff se verify): LegalScreens.kt (privacy rewrite + Terms fix), ModelsScreen.kt (benchmark naam UI se hataya), FloraApp.kt (sirf live-update sendChat branch, 33 lines), PLAN Phase 7 section rewrite + Maya-gap section, AUDIT_LOG me Phase 7 deliverable change + Task B + fix3 notes. Iske alawa local tree me pehle se maujood diffs (android.yml local comments, purane docs drafts, function copies, watcher-appended ROUND1) Phase 6 ka hissa NAHI hain — is push me nahi liye gaye, taaki scope saaf rahe. android.yml ko API se kabhi nahi chheda gaya (workflows scope nahi hai).
- R3 FIX A — Privacy point 1 overclaim: naya point 1 kehta tha settings/API keys "encrypted DataStore" me hain — FloraPreferences.kt check par keys plain DataStore strings nikli (koi MemoryCrypto nahi). Point 1 dobara likha: stores (yaadein/journal/docs/routines/jobs/chains/SOS/notifications/auto-log) encrypted; settings/keys sirf phone ke private store me, kisi server par save nahi, kisi backup me nahi; key sirf provider ko pehchaan ke liye jaati hai jab user usse baat kare (point 4).
- R3 FIX B — Privacy point 5 gap: "image banao" ka prompt Pollinations (image.pollinations.ai) ko jaata hai, par point 5 me iska zikr nahi tha. Point 5 me joda: prompt Pollinations ko, bani photo sirf gallery me. Brave ka lane nahi bana hai (key sirf save hoti hai, traffic nahi) — isliye Brave ka zikr zaroori nahi.
- PLAN stale lines sudhari: G19 line (CI in_progress → SUCCESS run 40737122451; Deepgram sign-in ruka → secret set 23:21 IST) + Deepgram bullet (browser task chal raha hai → HO GAYA 23:21 IST).
- Prepush checks: FloraApp branch ke saare vars upar declare hain (voice 160, searchTools 166, input 193, appContext 226, scope 251, lower 1393 sendChat me) — branch 2929 ke saath same scope; bare return padosi branches jaisa; nullable guard — weather/news try/catch se "" par girte hain, msg non-null join. Braces/parens balanced (FloraApp 1204/1204, 2491/2491; LegalScreens 6/6, 65/65; ModelsScreen 22/22, 46/46). Koi key/password file me nahi; user-visible UI me benchmark naam nahi (code comments me benchmark reference sirf design note hai, copy nahi).
- R3 full scan: hosts 21, sab justified (Supabase backend, Gemini/Groq/Deepgram APIs + key-link consoles, DuckDuckGo/Open-Meteo/geocoding/Tavily/Yahoo/frankfurter/Dictionary free APIs, Pollinations image, wa.me, Play Store Shizuku link, GitHub/Vercel G16, Google search fallback, YouTube gaana) — koi anjaan host nahi. TODO/FIXME 0; hardcoded key/token 0. Permissions 16 Android + Termux RUN_COMMAND, sab justified; BIND_* services par hi hain.
- Regression (code level): CallActivity + FloraInCallService, CameraLookScreen + VisionClient, keys UI + "Bhare hue keys check karo", live-update dono raaston par (FloraApp me 3 match), encrypted stores (MemoriesStore + 8 stores MemoryCrypto) — sab maujood.
- OPEN ISSUES: 0 (code-audit level; phone test Phase 8 + dashboard deploy Phase 4 is audit ke scope se bahar hain — isliye "0" ka matlab code-audit 0 hai, phone-verified 0 nahi).

## Root-cause fix sweep — 2026-10-29 (user order: garbari jad se + dead/duplicate safai)
- Sweep tareeka: 92 Kotlin files par automated scan (unused imports, zero-caller funs, duplicate fun names, class-level dead check) + haath se claim verification (encryption, retention, boot, manifest, functions tables, web pages, voice branches). Push se pehle har file ka remote-tip diff verify (local == remote, sirf sweep edits push hue).
- BATCH 1 (commit 41d9c65f, CI run 40760429993 SUCCESS): dead code safai — ContactLookup.findNumber hataya (findAll ne supersede kiya, v2.22 chunk 2; zero callers grep proof), BiometricTemplates ke hasFace/storeVoice/loadVoice/hasVoice hataye (face = storeFace/loadFace live, voice = VoiceVault live; zero callers), FloraPreferences.setModelsManifestUrl setter hataya (getter ModelDownloader me live, setter ka zero caller), AutomationGate poori file delete (tree me zero reference; consent rule FloraApp ke direct prefs.automationConsent() check + AutoModeNotifier me zinda hai), 2 unused imports (AppLauncher PackageManager, PermissionsScreen Column). getValue/setValue wale "unused import" hits FALSE POSITIVE the (by-delegation ke liye chahiye) — unhe chhua nahi.
- BATCH 2 (commit 71a0e7e8, CI run 40761391744 SUCCESS): sabse badi garbari — Guardian mode enforcement gap. GuardianGate ke check methods (canUnlock/canUnlockFull/canAccessSensitive/clearSession) ka zero caller tha; guardianMode pref sirf load + toggle hoti thi, kisi sensitive action ko gate nahi karti thi — jabki Switch label kehta tha "ON = sensitive kaam ke liye PIN dobara chahiye". Root cause: chat lock ka PIN + awaaz check FloraApp me inline state (chatUnlocked/pendingVoiceUnlock) se chalta hai, GuardianGate se wired kabhi nahi hua. Fix (scope ke mutabiq, text ko sach par laya; enforcement wiring nahi — wo user faisla maangta hai ki kaunse actions gate honge): Switch label + neeche wala note imaandaar text par, GuardianGate class doc me wiring-pending NOTE. markPinVerified/markVoiceVerified calls zinda hain (chat lock + Settings verify se), check methods API ke taur par rakhe hain (delete nahi — wiring ke waqt chahiye honge).
- BATCH 3 (commit 66d98fd2, CI run 40762275106 SUCCESS): duplicate single-source — 9 stores (AutoLog/Docs/JobChain/Job/Journal/Memories/Notification/Routine/Sos) me identical private encode/decode one-liner (MemoryCrypto delegate) the; ab MemoryCrypto.encodeStored/decodeStored ek jagah, 9 stores wahi call karte hain (call sites: AutoLog 3 decode + 2 encode, Docs 1+1, JobChain 1+1, Job 1+1, Journal 1+1, Memories 2+2, Notification 1+1, Routine 1+1, Sos 1+1). Behaviour identical (wahi delegation).
- Voice parity count (plan doc me darj): FloraApp.kt me 122 unique voice phrases (dedupe) + 26-screen "kholo" map + BasicTalk 13 branches; Screen enum ke 28/28 screens voice se reachable. Maya ke 45 se ginti me zyada — target number se poora; phone behaviour Phase 8 me baaki.
- Check karke SAAF paya (koi fix nahi): MIXED_CONTENT fix intact (NEVER_ALLOW), SslError sahi package, Maya naam sirf comments me, TODO/FIXME 0, PIN/face-template sach me KeyStore AES/GCM encrypted, 30-din prune app-start par chalta hai (FloraApp 354-356, cutoff 30 din), BootReceiver wake-word restart karta hai (wakeEnabled par), manifest ke saare components maujood, functions sirf 8 known tables use karte hain, koi dangerous deprecated API nahi, download.html ke bahar web pages me koi stale claim nahi, plan doc Phase 7 section + refresh plan pehle se update tha (audit miss #1/#5 closeout me band — dobara nahi chheda).
- Duplicate NAHI nikle (check karke chhoda): cosine (FaceMatcher FloatArray/normalized-dot vs WakeWordMFCC DoubleArray/full-norms — alag type/semantics), postJson (BrainClient auth+PostResult vs SearchTools String?/alag timeouts — alag behaviour), stores ke add/save/load/remove (alag classes ke per-store methods).
- JAANBOOJH KAR FIX NAHI KIYA: (1) dead CI flora-apk upload step — android.yml workflows-scope ke bahar hai, web UI se hatana baaki (excluded); (2) G17 domain pinning — Phase 4 scope, code me zero trace, implementation/drop reason Phase 4 me; (3) Guardian enforcement wiring — kaunse actions gate honge, ye user faisla hai, bina spec ke wiring karna behaviour change hota; (4) voice ke do handler paths (early when-block + chat if-chains, kuch commands dono me) — unify bada refactor hai, design decision chahiye; (5) getValue/setValue imports — false positive, chhua nahi.
- OPEN (sweep ke baad): Guardian wiring (user faisla), dead CI step (web UI), G17 (Phase 4), voice handler unify (design). Koi batch double-fail par nahi ruka — teeno code batches pehli baar me CI green.

## Phase 2 voice pipeline — Maya-style (2026-10-29 ~raat, plan FLORA_MAYA_WORKFLOW_PLAN.md Phase 2)
- TTS SANITIZATION (VoiceEngine.kt): speak() ab raw text nahi bolta — sanitizeForSpeech() pehle URL, markdown link ([text](url) → text), markdown chars (# * ` > _ | ~), emoji code points (1F000–1FAFF, 2600–27BF, 2B00–2BFF, arrows, misc technical, ZWJ, variation selectors, keycap) aur bekaar spaces saaf karta hai; saaf hone ke baad khali bache to kuch nahi bola jata aur free-talk limit me bhi nahi gina jata. Saare voice.speak callers isi speak() se guzarte hain, isliye safai ek jagah hi kaafi hai. Regex order ka trap pakda gaya (URL pehle hatane se [link](url) toot kar "[link](" bachta tha) — isliye markdown-link pehle, URL baad me; Python simulation se 6 cases verify (emoji, Devanagari surakshit, markdown, link, khali, plain). Audit wali galti (VoiceEngine raw text bolta tha) isi se band hoti hai.
- BRAIN HISTORY (BrainClient.kt + FloraApp.kt fall-through): ask() me naya param history: List<ChatTurn> (fromUser + text); capHistory() aakhri 12 turns + total ~1500 chars par cap karta hai (purane poore drop, aadhe nahi; sabse recent hamesha rakha jata hai). Gemini path: history turns user/model roles ke saath contents me, mergeGeminiTurns() lagatar ek hi role ko "\n" se jodta hai (Gemini alternation rule); Groq path: system + history (user/assistant) + user prompt. Dono system prompts me ban line judi: kabhi emoji, markdown ya technical baat (code, model naam, key ki baat) nahi — PERMANENT RULE prompt level par bhi. FloraApp fall-through (3039) history banata hai: current sawaal baahar (wo prompt hai), assistant lines ka "Flora: " prefix hat kar; sirf main chat fall-through wired hai — routine brain step (579), file Q&A (2449) aur code-gen (2486) jaanboojh kar nahi (single-shot tasks; code-gen ko history bigaadta).
- STT LANES VERIFY (koi change nahi, verify karke chhoda): DeepgramClient timeouts (15s connect / 60s read), khali key/PCM → null, exception → null, blank transcript → null; SttLanes us par empty-PCM + missing-key + exception guards lagata hai. Caller sirf VoiceEnrolScreen (enrolment samples) hai — main mic path system STT (ContinuousListener) hai, barge-in ke liye Deepgram streaming (BargeInController) Flora ke bolne par chalta hai. Main-path Deepgram REST wiring maujood nahi — ye breakage nahi hai (system fallback hi working path hai), par limit ke roop me darj hai.
- TURN VERIFY (koi change nahi): ContinuousListener system recognizer ke FINAL results par hi bhejta hai (onPartialResults khali — adhoora text kabhi chat me nahi jata); MainActivity error par 600ms me restart, TTS-done par dobara suno, 45s safety restart. Yehi lightweight turn detection hai — trained turn model alag decision hai.
- ADDRESSEE VERIFY (koi change nahi): foreground = listener activity pause par stop hota hai (Flora UI me boli baat hi Flora ke liye hai); background = WakeWordService se gated (MFCC cosine vs enrolled/default template). LIMIT imaandaari se: wake ke baad background mic jo bhi sunta hai use Flora ke liye maan leta hai — trained addressee model is phase me nahi hai (alag decision).
- WAKEWORD THRESHOLD VERIFY (koi change nahi, saaf paya): shipped path WakeWordService.kt:47 const WAKE_THRESHOLD = 0.60 + FloraPreferences.wakeThreshold() pref (default 0.60f) + try/catch fallback — ek hi jagah, user-adjustable, consistent. Draft WakeWordMFCC.kt header me prototype numbers (positives 0.732–0.896, negatives ≤0.314) pehle se darj hain. Koi naya model bundle nahi, koi recording upload nahi.
- Prepush checks: VoiceEngine braces 21/21 (parens 91/92 sirf grep artifact — `[^)]` character class me literal `)` hai, regex valid hai; Python simulation se verify); BrainClient braces 56/56, parens 261/261; FloraApp braces 1270/1270, parens 2752/2752 (edit se pehle 1268/1268, 2741/2741 — delta balanced). Koi naya var launcher/callback ke baad use nahi; nullable par mapNotNull + `?.let` guards; koi fun/property-setter clash nahi; sed use nahi hua. Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi.
- IS BUILD ME NAHI: action engine (Phase 1, parallel builder ka kaam), neural voice / tts-proxy (Phase 8), default-assistant role (Phase 5), browser race (Phase 4), koi naya permission/manifest service.

## Phase 1 action engine — Maya-style verified app-open (user order 2026-10-29 02:35 + 02:39 IST)
- PROBLEM (zero-action audit se): AppLauncher.launch() intent fire + no-exception par true deta tha, foreground kabhi verify nahi hota tha — Flora "khol diya" bol deti thi jabki app saamne aati hi nahi thi. BackgroundMicService.isCommand sirf PREFIX jaanta tha ("kholo youtube"); natural Hindi suffix ("youtube kholo") kabhi command nahi banta tha. Chat router me suffix "X kholo" external-app branch nahi thi. Routine runner + LiveScreen firstOrNull auto-pick karte the.
- BUILD (commit b26195fa + fix 4cbcc1bd):
  - AppLauncher POORI FILE DELETE — uski jagah VerifiedAppLauncher.kt (naya file): openVerified() + openWebsiteVerified(), result sealed class (Opened / AlreadyOpen / NotVisible / NoVerifier / NoLaunchIntent / FiredUnverified-website-only). "khol diya" SIRF Opened / AlreadyOpen par bola jata hai.
  - VERIFICATION (ASLI, dispatch-Boolean nahi): FloraAutomationService me window-state tracker (TYPE_WINDOW_STATE_CHANGED par sirf package naam + waqt — content kabhi store nahi hota) + live window tree (rootInActiveWindow ka package) + waitForForeground() (150ms poll, 3500ms timeout). UsageStats par nahi ruka gaya (alag permission + Settings step chahiye hota; Accessibility Flora me pehle se automation ka raasta hai; config me typeAllMask hai, isliye window events milte hain).
  - Service OFF ho to app fire HI NAHI hoti (NoVerifier) — caller imaandaar failure bolta hai ("Accessibility service ON karna hoga, Settings me"), chupchaap "khol diya" kabhi nahi. Ye behaviour change hai: pehle service OFF par bhi app (unverified) khul jaati thi, ab saaf mana + wajah.
  - Saare "khol diya" callers verified flow par: routine app step, autoExecuteOpen (ab onDone callback ke saath — compound "kholo aur <kaam>" doosra hissa SIRF verified open ke baad chalata hai, 1000ms settle), kholo-aur-dekho branch, G16 (app + website fallback), confirm dialog, AutoScreen button, LiveScreen openAppAndLook. ChatGPT image flow (open → delay → prompt fill → Send) aur install flows AppAutomation.launch (ab suspend, OpenResult) par — Maya macro pattern (open → verify → tree se fill/tap) ab verified open par tika hai.
  - Chat router me suffix "X kholo" branch (screen-map ke turant baad): prefix branch jaisa behaviour (ek match → confirm/auto, zyada → ambiguity, zero → not-found). Flora ki apni screens upar wale map me pakdi jaati hain; G16 (chrome/github/vercel) + "link" wale naam is branch se baahar rakhe gaye hain (wo neeche/pehle wale branches sambhalte hain).
  - BackgroundMicService.isCommand me " kholo" suffix juda — queue/drain chain se wahi verified flow par jaata hai. "khol do" JAANBOOJH KAR NAHI joda (router me uska branch nahi hai — jodne se naya router hole banta).
  - Ambiguity MANDATORY: koi firstOrNull / found.first() auto-pick nahi bacha (grep 0). Interactive raaston par options bol kar poora naam manga jaata hai; routine chain me (jo reply ke liye ruk nahi sakti) options bol kar step skip hota hai — auto-pick kabhi nahi.
  - Dekhna (02:39 direction): lookAtScreen ka order pehle se sahi tha (Accessibility screenshot PEHLE, MediaProjection fallback) — wahi rakha; accessibility path me tree context juda (visibleTexts(25) + foreground package vision prompt ke aage — list kahin save nahi hoti). LiveScreen ask() bhi accessibility-first hai, chheda nahi.
  - Website: openWebsiteVerified() — default browser resolve (MATCH_DEFAULT_ONLY) + foreground verify; chooser case ya service OFF par FiredUnverified (intent fire hua) — caller is par sirf progress bolega ("khol raha hun"), "khol diya" nahi.
- CI: pehla run 40770789002 (b26195fa) FAILURE — "Build debug APK" step, 12 errors, sab ek hi kaaran: VerifiedAppLauncher.kt ke andar sealed-subclass naam bina qualification ke likhe the (NoVerifier/Opened/...), jo Kotlin me outer object ke scope me nahi aate. Fix commit 4cbcc1bd (sirf OpenResult.X qualification, koi behaviour change nahi) — run 40771549153 completed SUCCESS, job build success, CI artifact flora-debug-apk 60,234,705 bytes (ZIP size — APK size nahi).
- COLLISION NOTE: Phase 2 builder (commit 4e824c8e) is build ke beech me push hua (tip dd4d47a1 → 4e824c8e). Fetch ke waqt files pehle se 4e824c8e wali thi, isliye Phase 2 ka FloraApp history change (+13/-1) REVERT NAHI hua — remote par dobara verify kiya gaya (ChatTurn history + verified-open dono maujood, AppLauncher.kt deleted, AppLauncher.launch 0).
- Prepush checks: braces/parens balanced (FloraApp 1324/1324, 2804/2804; VerifiedAppLauncher 21/21, 62/62; FloraAutomationService 55/55, 175/175; AppAutomation 22/22, 51/51; AutoScreen 56/56, 122/122; LiveScreen 80/80, 180/180; BackgroundMicService 51/51, 165/165). Declaration order: AutoScreen scope (dialog se pehle), FloraApp scope (251, dialog 3451 se pehle), suffix branch ke vars (autoMode/voiceAppConfirm upar declare) — sab grep se verify. Nullable par `?.let` / try guards; koi fun/property-setter clash nahi; sed use nahi hua. Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission/manifest service nahi.
- IS BUILD ME NAHI (jaanboojh kar): in-app browser WebView load race, neural voice / tts-proxy deploy, trained wakeword, default-assistant role, Termux/macros engine, WhatsApp/Mail/Share draft claims ("khol diya hai" — draft-open, user Send dabata hai), Play Store intents (progress-speech only, koi success claim nahi).
- PHONE TEST BAAKI (Phase 8): zero-action audit ke 7 tests (foreground-vs-background, Auto Log "ho gaya" vs dikha-nahi, dialog, ambiguity, background-vs-foreground bolna, wizard check, screen session check) + service-OFF par imaandaar failure ka test.

## Phase 2B long-lasting on-device memory (user order 2026-10-29 02:59 IST, plan FLORA_MAYA_WORKFLOW_PLAN.md)
- PROBLEM: Phase 2 ki brain history sirf in-session thi (restart par khatam). User ka order: Flora ko poori memory ho — kya kar rahi hai, aage kya karna hai — bole, yaad rahe, EXECUTE bhi kare; sab kuch user ke mobile par hi save ho; "pehle kya bole the / aage kya karna hai / kya bole the / kya karna chahiye" par samajh kar jawab de; memory LONG LASTING ho.
- BUILD (2 naye files + FloraApp.kt):
  - ChatMemoryStore.kt (naya, data package): DataStore Preferences "flora_chat_memory", ek JSON key, MemoryCrypto.encodeStored/decodeStored (KeyStore AES/GCM — wahi single-sourced delegation). StoredTurn (fromUser/text/ts). loadAll/addTurn/clearAll + turns Flow. Is store par 30-din prune NAHI hai (long-lasting order) — bloat se bachne ke liye sirf aakhri 500 turns rakhe jate hain (cap hai, time-prune nahi). Corrupt JSON → khali, crash nahi. Greeting kabhi save nahi hoti.
  - PendingTaskStore.kt (naya, data package): DataStore "flora_pending_tasks", "yaad rakhna" wale kaam (text/ts), MemoryCrypto encrypted. loadAll/add/removeAt/clearAll. Is par bhi 30-din prune nahi (pending kaam waqt se nahi, poora hone se khatam hote hain).
  - FloraApp.kt: stores + states (chatMemoryStore/pendingTaskStore/memoryReady/persistedCount/executeAfterReply) autoLogStore ke baad declare (scope se pehle, par effects me scope use nahi hota — isliye order safe hai); restore effect (LaunchedEffect(Unit): aakhri 60 turns chat me wapas dikhao, persistedCount + memoryReady set); persist effect (LaunchedEffect(messages): naya judne par encrypted save, "Flora: " prefix hat kar, greeting/khali skip, count tabhi badhe jab save kamyab ho); execute effect (LaunchedEffect(busy, executeAfterReply): busy khatam hote hi task sendChat se wahi verified raaste par — Auto Mode rules router me lagte hain).
  - Recall branches (sendChat me, `lower` ke turant baad, early — user turn append se pehle): "pehle kya bole the" family (aakhri 3 user turns), "tumne kya bola tha" family (aakhri 2 Flora turns), "aage kya karna hai" family (pending list ya imaandaar "koi pending kaam nahi"), "yaad rakhna ..." (task save), "agla kaam karo" (pehla task list se hata kar executeAfterReply par — ek baar hi chalega). Jawab store se aate hain, brain ke andaze par nahi; khali ho to imaandaar khali-jawab, banaya hua nahi.
  - Brain history ab persisted store se: fall-through me storedTurns.loadAll() → ChatTurn list; current sawaal aakhri turn ho to hata do (persist effect use pehle hi save kar chuka ho to). Cap ab bhi BrainClient.capHistory me (12 turns / ~1500 chars). Purana in-memory prior.takeLast(12) block hataya gaya.
  - AutoMemory ke 7 facts patterns chhede nahi — persisted chat store unke saath chalta hai, unki jagah nahi leta. App-start prune (memories/journal/autolog, 30 din) wahi hai — naye stores usme nahi aate.
- Prepush checks: braces/parens balanced (FloraApp 1390/1390, 2910/2910; ChatMemoryStore 17/17, 55/55; PendingTaskStore 16/16, 50/50). Declaration order grep se verify (stores 240-244, effects 245+, sendChat uses 1532+; `lower` block ke turant upar; scope 251 sendChat se pehle). Nullable guards (loadAll emptyList on exception, tasks[0]/mine[0] isEmpty/size checks ke baad). Koi fun/property-setter clash nahi (turns/tasks val Flow hain, usi naam ka fun nahi). Sed use nahi hua. Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission/manifest service nahi; server/Supabase change nahi.
- LIMITS (imaandaari se): history/brain me store ke turns aate hain — persist effect ke lag se ek turn kabhi-kabhi agle turn me dikhega (degradation: history thodi stale, kabhi galat nahi). "Yaad rakhna" wale task ka execute ek baar hota hai (list se hat jata hai) — agar action khud fail ho to Flora apna imaandaar failure bolegi, task wapas nahi aayega (dobara "yaad rakhna" bolna hoga). Chat memory clear karne ka UI abhi wired nahi hai (store me clearAll maujood hai, future me user confirmation ke saath judenge — delete bina confirmation ke nahi). Phone behaviour Phase 10 checklist me test hoga (restart ke baad history dikhna, recall jawab, pending execute).
- IS BUILD ME NAHI: Phase 3 screen, Phase 6 macros/form-fill/image-gen, neural voice, server changes, koi naya permission.

## Phase 3 screen + live vision, Maya-style Accessibility-first (user decision 2026-10-29 03:03 IST, plan FLORA_MAYA_WORKFLOW_PLAN.md)
- DECISION: live screen ka method = ACCESSIBILITY-FIRST HYBRID. Primary: Accessibility takeScreenshot (service ON ho to bina dialog, free, on-device, durable) + node tree (structure: kya hai, kahan hai, kya clickable hai). Fallback: MediaProjection (sirf jab accessibility screenshot na mile, ya sach me continuous video frames chahiye hon). MediaProjection-only NAHI (session process death par khatam, dialog, indicator, battery). Tree-only NAHI (image nahi dekh sakta). Server streaming NAHI (standing rule se excluded).
- PROBLEM: screen-look ke sirf 4 exact trigger strings the ("screen pe kya dikh raha hai" / "...batao" brain par gir jaate the); session null par re-ask loop (dialog baar-baar); pending question busy par chupchaap drop (voiceCommand effect me busy check ke baad value clear); live dekhna tha hi nahi (sirf one-shot look).
- BUILD (FloraApp.kt + BackgroundMicService.kt):
  - Fuzzy screen triggers (sendChat, share branches ke baad): "screen dekho" / "screen dekho ..." / contains("screen") + (kya/dikhao/batao/"chal raha") — "pe"/"par" dono, trailing "batao"/"dikhao" ignore. Purane 4 exact strings isme sama gaye (purana branch hataya gaya).
  - Live branch (fuzzy se PEHLE): "live screen dekho" / "screen live dekho" / "live screen dikhao" → lookAtScreenLive(): accessibility se 3 frames (~1.2s gap) + tree, aakhri frame + tree diff vision ko. Sachha continuous video nahi hai (one-shot frames hain) — ye comment + report me saaf hai, user ko "video" kabhi nahi bolna.
  - Voice-command queue (busy-drop fix, Phase 2B execute-effect pattern jaisa): LaunchedEffect(voiceCommand.value) ab busy par value clear NAHI karta; LaunchedEffect(busy) busy khatam hote hi pending value chalata hai. Dono effects Main par bina suspension ke consume karte hain, isliye double-invoke nahi hoga. Purana stale comment ("busy ho to chhod do") bhi sudhara gaya.
  - Session re-ask guard (screenReaskCount, composable state ~line 330, saare uses uske baad): session null ya capture fail par ek baar dialog dobara kholna theek hai; uske baad bhi na bane to dialog loop NAHI — imaandaar failure ("Accessibility service ON karo — usse bina dialog ke dekh lungi"). Success par count reset; share-off aur deny par bhi reset. MediaProjection token process ke saath khatam ho jaata hai (Android limit) — ye message me saaf hai, chhupaya nahi.
  - Key check PEHLE: lookAtScreen/lookAtScreenLive me Gemini key check capture se pehle (bina key ke screenshot lena bekaar hai); key na ho to imaandaar message, kabhi technical baat TTS me nahi.
  - Accessibility-first order banaye rakha (lookAtScreen me wahi hai): accessibility screenshot → tree context (foregroundPackage + visibleTexts(25), kahin save nahi hota) → MediaProjection fallback. BackgroundMicService ke screen path me bhi wahi tree context judaa (richQuestion).
  - Live pending: MediaProjection dialog ke baad pending "live screen dekho" wapas live branch par jaata hai (purana "screen dekho" pending sirf normal look ke liye).
- Prepush checks: braces/parens balanced (FloraApp 1418/1418, 2983/2983; BackgroundMicService 58/58, 173/173). Declaration order grep se verify (screenReaskCount 330, uses 349+; lookAtScreenLive 2478, branch use 2621; screenLook val branch se pehle). Nullable guards (screenshot/tree try-catch null, isNullOrEmpty checks, firstTree/lastTree null checks). Koi fun/property-setter clash nahi. Sed use nahi hua (python exact-once replacements, har anchor 1x verify). Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission/manifest service nahi; server/Supabase change nahi.
- LIMITS (imaandaari se): accessibility frames one-shot hain — "live" ka matlab refresh kiye hue frames hai, video-stream nahi (continuous video sirf MediaProjection fallback se). MediaProjection token process death ke baad dobara Allow maangega (OS niyam, koi app isse bach nahi sakti). Vision samajhne ke liye user ki free Gemini key chahiye (dekhna free hai, samajhna key se). Phone behaviour Phase 10 checklist me test hoga (fuzzy triggers, live frames, re-ask guard, busy-queue, background tree).
- FIX (CI fail 40775751163, Build debug APK): lookAtScreenLive me `lastJpeg` (mutable var) par smart-cast fail — if-block ke andar assignment ke baad pehle wali null-check cast khatam ho jaati hai (AGENTS.md nullable lesson). Fix: `val frame = lastJpeg` + explicit null-check (null ho to imaandaar message + return), phir `frame` use. Koi behaviour change nahi, sirf type fix.
- IS BUILD ME NAHI: Phase 6 macros/form-fill/image-gen, neural voice, server changes, koi naya permission.

## Phase 4 browser + calls + messages (plan FLORA_MAYA_WORKFLOW_PLAN.md Phase 4)
- PROBLEM (audit-verified): BrowserScreen effect pendingUrl ko turant null kar deta tha, aur BrowserController.load() WebView null par silent no-op tha — WebView factory abhi chali hi nahi ho to URL hamesha ke liye kho jaata tha, blank browser, koi error nahi. "Browser kholo" par bhi blank (koi default page nahi); screen chhodne par state khatam.
- BUILD (BrowserController.kt + BrowserScreen.kt; FloraApp.kt ke voice branches chhede nahi):
  - load() kabhi URL nahi khota: WebView null ho to URL queuedUrl me jata hai (onResult(false)); post ke dauraan WebView hat jaye to bhi dobara queue hota hai. Factory me BrowserController.webView set hote hi flushQueued() queued URL khol deta hai. lastUrl har load par set hota hai.
  - Default homepage: factory me queued null + pending voice URL null ho to aakhri page restore karo (lastUrl), warna pehli baar HOMEPAGE (https://www.google.com) — blank kabhi nahi. Pending voice URL ho to factory skip karta hai (effect use kholega) — homepage requested URL ko clobber nahi karta.
  - State save: onPageStarted + onRelease par saveState(currentUrl) — screen chhodne par aakhri URL yaad rehta hai, wapas aane par restore hota hai (process lifetime ke andar; process death par in-memory state khatam — OS niyam).
  - Visible error/notice: pageError box (main-frame onReceivedError) pehle se tha, banaye rakha; naya queuedNotice banner ("Page khul raha hai — browser taiyaar hote hi dikh jayega.") jab load queue me jaye; onPageStarted par notice hat jata hai. Load-fail par retry button wala error box wahi hai.
  - Read-after-load guard: "read" action me WebView ready na ho to galat page padhne ki jagah imaandaar message ("Page abhi khul raha hai, khulne ke baad dobara bolo.") — queue wale page ka galat text kabhi nahi padha jayega.
  - Voice branches UNCHANGED: "browser kholo" / "browser me X kholo" / wapas / aage / reload / padho / links / link:N — sirf load path sudhra hai. "Jao" button + history click bhi wahi load() (default onResult) par chalte hain.
- CALL/SMS VERIFY (koi change nahi, verify karke chhoda): direct paths autoExecuteCall/autoExecuteSms (Auto Mode + CALL_PHONE/SEND_SMS permission, AutoActions Boolean + Auto Log "ho gaya"/"fail"); non-auto path me in-app confirm dialog ("Call lagau?" / "SMS bheju?") + permission launcher; routine steps permission na ho to imaandaari se skip ("permission ya number nahi mila, step chhod diya"). Koi silent send nahi — har direct call/SMS ya Auto Mode consent ya in-app confirm se guzarta hai. ACTION_DIAL fallback code me nahi hai — by design (AutoActions header: user order — ek confirmation ke baad poora execute, dialer draft nahi).
- WHATSAPP VERIFY (koi change nahi): sirf wa.me chat kholo, message bhara hua — Send user dabata hai (runContactAction + "whatsapp karo" dono wahi path); "whatsapp padho" notification store se padhta hai. Silent send kahin nahi (CEO rule).
- NOTIFICATION VERIFY (koi change nahi): FloraNotificationListener READ ONLY — title+text sirf is phone par save, koi dismiss/reply/action nahi; "notification batao" branch store se padh kar bolti hai. Auto-reply is phase me nahi joda (user decision pending).
- Prepush checks: braces/parens balanced (BrowserController 42/42, 109/109; BrowserScreen 103/103, 174/174). Declaration order grep se verify (queuedNotice state effect se pehle; queued/restore factory me declare-then-use; webViewReady effect me declare-then-use). Nullable guards (val w = webView copy + post ke andar val current = webView explicit null-check — Phase 3 smart-cast lesson; q ?: return null; restore != null check; saveState isNullOrEmpty guard). Koi fun/property-setter clash nahi (load ka ek hi signature, onResult default null — purane 3 callers bina change ke compile hote hain). Sed use nahi hua. Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission/manifest service nahi; server/Supabase change nahi.
- LIMITS (imaandaari se): lastUrl/queuedUrl in-memory hain — process death par khatam (screen-exit state process lifetime tak hai). reload()/clickLink() WebView null par abhi bhi silent no-op hain (screen par button hone se webView maujood hota hai; voice reload screen ke bahar aaye to request screen kholne par effect se chalta hai). Phone behaviour Phase 10 checklist me test hoga (queue flush, homepage, restore, queued notice, load-fail error, read guard).
- IS BUILD ME NAHI: Phase 6 macros/form-fill/image-gen, neural voice, server changes, koi naya permission.

## Phase 5 system roles + survival (plan FLORA_MAYA_WORKFLOW_PLAN.md Phase 5; OVERLAY MIC isme nahi — numbered user decision open hai)
- PEHLE SE THA (verify karke chhoda, dobara nahi banaya): default assistant ka service + session (FloraVoiceInteractionService/SessionService, manifest-declared, v2.22 G3) + Settings me "Default assistant banao" button (ACTION_VOICE_INPUT_SETTINGS); battery-unrestricted + Vivo/iQOO auto-start guidance Settings screen + PermissionsScreen dono par; BackgroundMicService/WakeWordService START_STICKY + foreground notification + BootReceiver (wake/continuous-mic + routine alarms reboot par wapas).
- BUILD (naya + poora):
  - FloraAssistActivity.kt (naya, voice package): default-assistant trio ka teesra hissa — ACTION_ASSIST + CATEGORY_DEFAULT, exported. Assist trigger par MainActivity aage laati hai (NEW_TASK|CLEAR_TOP|SINGLE_TOP) aur khud finish — koi UI nahi, koi command execute nahi, koi unlock/posting nahi.
  - FloraRecognitionService.kt (naya, voice package): system RecognitionService, Flora ki apni Deepgram lane (nova-2, hi) se. AudioRecord (VOICE_RECOGNITION, MIC fallback; 16kHz mono PCM16) → DeepgramStreamClient.sendPcm → pehla FINAL transcript onResults, phir session khatam (one-shot). Busy par ERROR_RECOGNIZER_BUSY; mic permission na ho to ERROR_INSUFFICIENT_PERMISSIONS; key na ho to ERROR_CLIENT (chupchaap system STT par nahi gira); recorder fail par ERROR_AUDIO; stream fail par ERROR_NETWORK; stop par kuch pehchana na ho to ERROR_NO_MATCH. sessionId guard: stop/cancel ke baad shuru ho rahi start leak nahi karegi. Audio sirf stream, kabhi save nahi. System STT lanes (ContinuousListener, barge-in) chhede nahi.
  - Manifest: AssistActivity + RecognitionService (BIND_RECOGNITION_SERVICE, android.speech.RecognitionService filter) entries; voice_interaction_service.xml me recognitionService attribute joda + purana comment sudhara (pehle jaanboojh kar nahi diya tha — ab apni lane ban gayi hai, isliye joda gaya).
  - FGS hardening: BackgroundMicService.onTaskRemoved (sirf wanted=true par restart ki koshish) + WakeWordService.onTaskRemoved (service chal rahi hai to wake ON hi hai — BootReceiver sirf ON par start karta hai) — dono try/catch (Android 12+ / Vivo par block ho sakta hai, crash nahi).
  - Guidance poora: SettingsScreen + PermissionsScreen dono par ON_RESUME auto-refresh (DisposableEffect + LifecycleEventObserver; states ke BAAD declare — order trap se bacha). Settings se wapas aate hi battery/notification status khud update hoga; manual "Status refresh karo" button phir bhi hai. Koi setting app khud silent change nahi karti — sab user ke tap se khulta hai.
- Prepush checks: braces/parens balanced (AssistActivity 5/5, 11/11; RecognitionService 57/57, 110/110; BackgroundMicService 62/62, 183/183; WakeWordService 31/31, 85/85; FloraApp 1425/1425, 2998/2998; PermissionsScreen 73/73, 145/145). Declaration order grep se verify (Settings: states 3968-69 → effect 3974-75; Permissions: refreshTick 54 → effect 58-59; context/scope states se pehle). Nullable guards (val copy + explicit null-check — Phase 3 smart-cast lesson; recorder/stream null checks; minBuf <= 0 guard). Koi fun/property-setter clash nahi (stream/recorder/delivered/sessionId sirf var hain, usi naam ka fun nahi). Sed use nahi hua. Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission nahi (role/service ke liye zaroori manifest entries hi — BIND_RECOGNITION_SERVICE permission entry + ASSIST activity, report me likha hai); server/Supabase change nahi.
- LIMITS (imaandaari se): role grant user ka kaam hai (app sirf settings khol sakti hai — default assistant/recognizer chunna user karega); recognizer Hindi-only + one-shot (pehla final hi milta hai, interim nahi, lagatar dictation nahi); recognizer ko user ki Deepgram key chahiye (bina key ke ERROR_CLIENT); onTaskRemoved restart Android 12+ FGS-start restrictions / Vivo aggressive kill par fail ho sakta hai — tab battery-unrestricted + auto-start hi bachaav hai (guidance app me hai, par setting user ko ON karni hogi); process death ke baad MediaProjection token wali limit is phase ki nahi (Phase 3 me darj hai). Phone behaviour Phase 10 checklist me test hoga (home long-press se Flora khulna, recognizer select + ek utterance, task swipe ke baad service wapas, resume par status update).
- IS BUILD ME NAHI: overlay mic (numbered user decision open hai — is phase me nahi banaya); Phase 6 macros/form-fill/image-gen; neural voice; server changes; koi naya permission jo scope se bahar ho.

- FIX (CI fail 40779875179, Build debug APK, pehle Phase 5 push a9d3fae1 par): FloraRecognitionService.kt me 7 "Unresolved reference" errors — meri galti: service-side Callback ke methods ke naam client-side RecognitionListener wale likh diye the (onError/onResults/onReadyForSpeech/onBeginningOfSpeech). Asli service-side Callback ke naam bare verbs hain: error()/results()/readyForSpeech()/beginningOfSpeech() (AOSP RecognitionService.java se verify). Fix: 7 call sites wahi naam par laaye gaye (Deepgram listener ka apna onError() override chheda nahi — wo alag interface hai); comment me bhi (results) likha gaya. Koi behaviour change nahi, sirf naam sudhaar.

## Guardian enforcement build (user order 2026-10-29 ~04:03 IST: "Guardian enforcement pura karo sahi se sab kuch"; Phase 5 ke baad, Phase 6 se pehle)
- PROBLEM (sweep batch 2 se khula tha): GuardianGate check methods ke ZERO callers the, jabki Settings Switch label PIN re-ask ka daava karta tha (jhootha daava). Sweep ne sirf TEXT-to-truth fix kiya tha; enforcement wiring kabhi nahi hui. Ab poori wiring hui hai — label bhi sach par laaya gaya hai.
- DESIGN (GuardianGate.canAccessSensitive: !guardianMode || (pinEnabled && pinVerifiedThisSession)): Guardian OFF ho to sab seedha chalega; ON ho to is session me ek baar PIN verify hona chahiye — uske baad baaki gated kaam seedha chalenge (har kaam par dobara PIN nahi). PIN verify ke raaste: chat lock ka PIN, Settings ka "PIN verify karo" button, ya Guardian PIN dialog khud. Chat lock ka PIN + awaaz check (canUnlockFull) alag se pehle se wired tha, chheda nahi.
- BUILD (sirf FloraApp.kt + GuardianGate.kt comment; koi naya permission/manifest service nahi):
  - FloraApp me runGuardedVoice helper (scope ke baad declare, saare uses uske baad): gate pass ho to action seedha; fail ho to pending action + PIN dialog (AlertDialog, PasswordVisualTransformation, pinVault.verify) + imaandaar bolna ("Guardian mode ON hai — pehle PIN verify karo, phir ye kaam hoga.") + Auto Log entry ("Guardian ne roka..."). Sahi PIN par pending ek baar chalega, galat/cancel par kuch nahi chalega. Ek waqt me ek hi pending rehta hai (naya request purane ko badal deta hai — fail-safe: bina PIN ke kuch nahi chalta).
  - GATED VOICE/AUTO (12 call sites): autoExecuteCall, autoExecuteSms, autoExecuteOpen (isliye compound "kholo aur <kaam>" + saare auto branches bhi gate me aa gaye); routine call step, routine SMS step, routine app-open step, routine website step; "kholo aur dekho"; G16 chrome/github/vercel branch; confirm dialogs ke teen buttons (Call lagau? / SMS bheju? / App kholu?). Termux confirm dialog gate me NAHI hai (uska apna consent dialog hai; Termux removal alag user order, alag build).
  - Settings me runGuardedSettings helper (note state ke baad declare, saare uses uske baad) + PIN dialog (note block se pehle): GATED SETTINGS (8 call sites): Keys save, PIN set/change (pehli baar set Guardian OFF hone se seedha guzarta hai — gate tabhi lagta hai jab PIN pehle se ho; change par purana PIN verify hoga), Guardian mode OFF (anti-bypass: bina PIN ke Guardian band nahi hoga), Face Lock OFF (anti-bypass: bina PIN ke chat lock band nahi hoga), website account link, website account unlink, Admin UPI VPA save, UTR save.
  - GATE ME NAHI (jaanboojh kar, read-only ya non-sensitive): Keys check (sirf validity batata hai, value kabhi screen par nahi aati), Licence check, theme/persona/wake toggles, Auto Log dekhna, background screen sawaal (sirf dekhna + bolna, execution nahi — BackgroundMicService commands waise bhi background se execute nahi hote, app aage laakar confirm hote hain, isliye FloraApp gate hi kaafi hai; BackgroundMicService chheda nahi).
  - LABEL = SACH: Guardian switch label ("...PIN dobara maanga jayega") + explainer (gated kaam ki poori list) — purane jhoothe daave ("lagu nahi hai", "wiring baaki hai") hata diye gaye. GuardianGate.kt ka sweep NOTE bhi wiring-done par laaya gaya.
  - AUTO MODE TRANSPARENCY BANI HAI: ongoing notification + "auto mode band" stop chhede nahi; roka gaya har kaam Auto Log me likha jata hai; PIN dialog user ke saamne saaf dikhta hai (chhupke kuch nahi).
- Prepush checks: braces/parens balanced (FloraApp edited 1507/1507, 3082/3082). Declaration order grep se verify (runGuardedVoice decl 330, uses 522+; runGuardedSettings decl 4066, uses 4398+; guardianPinPending decls 329/4065, dialog uses 3950/4953 — sab uses decl ke baad; scope/messages/autoLogStore/params helper se pehle). Nullable guards (prefs reads try/catch false default; pinVault.verify Boolean; pending null-check let se). Koi fun/property-setter clash nahi (runGuardedVoice/runGuardedSettings sirf fun hain, usi naam ka var nahi). Sed use nahi hua (python exact-once replacements, har anchor 1x verify — E6 anchor pehle indent galat tha, grep se sahi karke dobara lagaya). Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission nahi; server/Supabase change nahi.
- LIMITS (imaandaari se): ek waqt me ek hi pending PIN action rehta hai — do gated kaam ek saath aaye to pehla pending badal jayega (fail-safe, par pehla dobara bolna padega); routine executor gated step par rukta nahi — baaki steps aage chalenge, pending step PIN verify par chalega (order badal sakta hai); PIN bhoolne par Guardian OFF nahi ho sakta bina PIN ke (by design — anti-bypass; naya PIN set karne ke liye bhi purana PIN chahiye jab Guardian ON ho); phone behaviour Phase 10 checklist me test hoga (Guardian ON par pehla gated kaam PIN maange, verify ke baad baaki seedha chale, Guardian OFF par koi dialog na aaye, galat PIN par kuch na chale, Auto Log me roka gaya kaam dikhe).
- IS BUILD ME NAHI: Phase 6 macros/form-fill/image-gen/auto-reply, overlay mic (Phase 6 scope me jayega), neural voice, server changes, koi naya permission.
- FIX (CI fail 40782339642, Build debug APK, pehle Guardian push 77482785 par): 2 errors — FloraApp.kt:523:51 aur 546:47 "Suspension functions can be called only within coroutine body". Meri galti: runGuardedVoice ka parameter `() -> Unit` (non-suspend lambda type) tha, par routine app-open step (E7) aur routine website step (E8) ke andar `VerifiedAppLauncher.openVerified` SUSPEND call hota hai — non-suspend lambda type me suspend call compile nahi hota, chahe runtime par scope.launch ke andar hi chale. Baaki 10 voice call sites fail nahi hui (ya to non-suspend calls the, ya andar scope.launch tha). Fix: dono helpers + pending var ka type `suspend () -> Unit` par laaya gaya, aur dono PIN dialogs se pending ko `scope.launch { pending() }` me chalaya gaya (dialog onClick non-suspend hai, isliye seedha pending() nahi chal sakta). Koi behaviour change nahi, sirf type fix — wahi Phase 3 smart-cast lesson ki tarah, compiler ke type niyam ka mamla hai.

## Phase 6 macros + form-fill + image-gen + carryover + auto-reply + overlay verify (plan FLORA_MAYA_WORKFLOW_PLAN.md Phase 6 + user orders 02:53/02:59/04:03 IST)
- PEHLE SE THA (verify karke chhoda, dobara nahi banaya): Overlay mic — OverlayMicService (floating mic button, foreground service + "Band karo" notification) + manifest declaration + SYSTEM_ALERT_WINDOW + voice branches ("overlay mic chalu/band karo", permission guidance) + MainActivity flora_mic extra → startMicRecognition (onCreate + onNewIntent dono). User order 04:03 "kaam ka hai, rakhna hai" — isliye rakha gaya, naya nahi banaya. Image-gen via ChatGPT bhi pehle se tha (v2.16 runChatGptImage: verified open → prompt bharo → Send → 20s wait → "app me dekh lo") + Pollinations fallback + "image share karo" (Gallery/Flora) — purana "image banao" raasta chheda nahi hai.
- BUILD (naya):
  - MacroEngine.kt (naya, automation): JSON macro (open/tap/type/wait/waitText/scroll/back) + MacroJson parse/toJson (kharab JSON → null, kabhi crash nahi; wait 0-30s clamp, waitText timeout 1-30s clamp). Runner: open = VerifiedAppLauncher (ambiguity → fail "ek se zyada app mili", NoVerifier/NoLaunchIntent/NotVisible alag messages — not-found aur launch-fail ek line kabhi nahi); tap = clickText (fail → "label nahi mila"); type = setTextOnFirstEditable/typeIntoFocused; waitText = visibleTexts poll. Sensitive guard: password field wali screen par pay/submit/login jaisa tap ya type → NeedsConfirm (caller dialog dikhata hai; Haan par wahi step se resume, Ruko par ruk jayega). Password field me macro kabhi type nahi karta. MacroStore.kt (naya, data): DataStore "flora_macros", MemoryCrypto encrypted, sirf on-device.
  - FormFiller.kt (naya, automation): findFields — label = hintText, warna sibling text, warna parent text (nodes sirf usi turn me kaam aate hain, save kabhi nahi). fillField: label contains-match; password field → PasswordRefused (Flora kabhi khud nahi likhegi); na mile → NotFound; set fail → SetFailed. clickSubmit labels: Submit/Continue/Aage/Save/Done/Bhejo (Login/Sign in jaanboojh kar bahar — sensitive tap macro guard se hoga).
  - InAppSearch.kt (naya, automation) + ForegroundAppContext: verified-open par noteOpened(pkg, label). searchInApp: Search/Khoj click → 800ms → setText/typeIntoFocused → Search/Go/Khoj click → Done; text set par submit na ho to TypedOnly ("likh diya hai, enter dabao"); box na mile to NoSearchBox. Carryover rule: plain "X search karo" sirf tab in-app jab saamne wali app sach me wahi ho jo Flora se kholi gayi thi (warna research branch, behaviour same); explicit "is app me / is me / yahan X search karo" me target saamne na ho to use dobara verified khol kar search hota hai.
  - ImageGenFlow.kt (naya, automation): saveJpegToGallery (MediaStore Pictures/Flora, API 29+ RELATIVE_PATH) + generateWithApp (verified open → prompt box labels → send labels → 20s wait → screenshot → gallery save → Saved/AppOnly; har fail ka alag reason). Gemini: AppAutomation me GEMINI_PKG (com.google.android.apps.bard) + KNOWN_APPS entries ("gemini", "google gemini"). Naya branch "image generate karo <prompt>, Gemini se / ChatGPT pe" + prefix form "gemini se image generate karo <prompt>"; app na batai ho to clarification (guess nahi); Gemini installed na ho to Play Store khulega (install user khud karega); login user khud ek baar karega (password Flora kabhi nahi mangegi). ChatGPT path me runChatGptImage ke baad screenshot bring-back joda gaya hai (purana branch chheda nahi — sirf success message me gallery-save line judti hai jab save ho).
  - ReplyDraft.kt (naya, automation): buildDraft — NotificationStore se us app ke aakhri 6 (purane se naye tak; 20-item cap ki wajah se 6 se kam mile to jo mile wahi context, 6 ka daava nahi) → brain.ask se draft (sirf jawab, koi vyakhya nahi). FloraApp me proposeReply + replyPending dialog: draft sunao + chat me likho; bhejna sirf Haan par — WhatsApp par ContactResolver se number → wa.me prefilled (aakhri Send user khud dabata hai); baaki apps me sirf draft (Flora direct reply nahi bhej sakti — imaandaari se bata kar). Silent auto-reply abhi bhi refused hai — ye design usse alag hai (context + confirm).
  - FloraApp.kt branches (sendChat, research branch se PEHLE — research behaviour same): carryover search (explicit + plain-matched), "macros batao", "macro chalao <naam>", "form bharo" (fields list), "field <naam> me <value> likho", "form submit karo" (password field ho to confirm dialog, warna seedha click), "image generate karo ..." (Gemini/ChatGPT), "is notification ka jawab batao" / "<app> ka jawab batao". States (voiceAppConfirm ke baad declare, saare uses uske baad): macroPending, macroNeedsConfirm, formSubmitPending, replyPending. Helpers (Clarification comment se pehle): runMacroFrom (step bol kar + Auto Log; Done/Failed/NeedsConfirm) + proposeReply. Dialogs (voiceCallConfirm dialog se pehle): macro sensitive-step, form submit, reply confirm. autoExecuteOpen ke onDone me ForegroundAppContext.noteOpened (verified open par hi). RoutinesScreen call site me onRunMacro joda gaya hai.
  - RoutinesScreen.kt: signature me onRunMacro param (ek hi call site — FloraApp; repo-wide search me aur call site nahi mila) + Macros section (LazyColumn ke andar: naam + JSON fields, parse-validate ke baad hi save; list + Chalao button → confirm dialog → onRunMacro).
  - BackgroundMicService.kt isCommand me Phase 6 commands jode gaye hain (macro chalao, macros batao, form bharo, field...likho, form submit karo, image generate karo, ...jawab batao, is app me...search karo) — background se ye command bus par Flora aage laakar confirm honge (rule kabhi nahi toota: background se execute nahi). Plain "X search karo" background se abhi bhi brain-talk raasta par hai (research behaviour chheda nahi); explicit "is app me" form background se bhi carryover branch par jayega (Flora aage aane ke baad target app dobara verified khol kar).
  - AppAutomation.kt: GEMINI_PKG + map entries (install flow ke liye bhi kaam aayega).
- Prepush checks: braces/parens balanced (FloraApp 1610/1610, 3100/3100; RoutinesScreen 79/79, 165/165; BackgroundMicService 61/61, 166/166; AppAutomation 22/22, 46/46; MacroEngine 60/60, 169/169; FormFiller 23/23, 80/80; InAppSearch 6/6, 20/20; ImageGenFlow 16/16, 47/47; ReplyDraft 11/11, 21/21; MacroStore 10/10, 28/28). Declaration order grep se verify (states 220-225 → helpers 1371/1417 → sendChat 1512 → uses 2138+/4299+; RoutinesScreen states 57/60 → uses uske baad). Nullable guards (val copy + explicit null-check — Phase 3 smart-cast lesson; service null par imaandaar message, kabhi crash nahi). Koi fun/property-setter clash nahi. Sed use nahi hua (python exact-once replacements, har anchor 1x verify). Apna crash pakda + fix kiya: "is app me search karo" (bina query) par substring(10, len-12) me start > end hota — coerceAtLeast se safe kiya (khali query par usage message). Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; overlay ke liye koi naya permission nahi (SYSTEM_ALERT_WINDOW pehle se tha); server/Supabase change nahi.
- LIMITS (imaandaari se): macro/form/in-app search sirf tab jab Accessibility service ON ho (warna imaandaar message); tree labels app ke UI par depend karte hain (label badla to step fail hoga — guess-click kabhi nahi); in-app search ka submit button har app me ek jaisa nahi (na mile to TypedOnly — text likha hua milega, enter user dabayega); image generation ka waqt app par depend karta hai (20s wait ke baad screenshot — image adhoori bani ho to screenshot wahi dikhayega, jhootha "ban gayi" nahi bola jayega, message me "ban rahi hai" hi hai); reply draft brain key par depend karta hai (key na ho to draft blank → imaandaar message); NotificationStore cap 20 ki wajah se purani thread ka poora context nahi mil sakta; phone behaviour Phase 10 checklist me test hoga (macro poora chale, form field + password-refusal + submit confirm, YouTube me carryover search, Gemini image + gallery save, WhatsApp reply draft + confirm + wa.me, overlay mic har screen par).
- IS BUILD ME NAHI: Termux (plan se REMOVED — koi naya kaam nahi; code me abhi bhi termuxConfirm branch + manifest permission/queries maujood hain, chhede nahi — dead-audit me alag se dekha jayega); neural voice; server changes; koi naya permission jo scope se bahar ho.

- FIX (CI fail 40785361366, Build debug APK, pehle Phase 6 push cb147d2f par): 4 alag root causes, sab meri galtiyan — (1) MacroStore ka package galat likha tha (automation; asli data package hai) — sendChat ke 2 sites par, isliye MacroStore + cascading `it` errors; (2) plain carryover branch me smart-cast Boolean ke through nahi chalta (inApp flag me null-check chhup gaya tha) — launch block me `val svc = fgService ?: return@launch` + explicit null-check se fix (Phase 3 lesson hi hai); (3) RoutinesScreen call site par trailing lambda + named last param ek saath nahi chal sakte (Kotlin: too many arguments + onRunRoutine missing) — dono lambdas named params par laaye gaye (onRunMacro + onRunRoutine, text order wahi); (4) runMacroFrom/proposeReply nested block (16-space indent, chat helpers wale block) me declare ho gaye the, isliye dialogs/call-site se unresolved the — body level (runGuardedVoice ke baad, 4-space) par laaye gaye, taaki sendChat + dialogs + call site sab dekh sakein; reply dialog ka runContactAction call (wo bhi nested helper hai) hata kar wa.me intent inline kar diya gaya hai (existing call dialog ke pattern jaisa — wa.me prefilled, aakhri Send user khud dabata hai). Koi behaviour change nahi, sirf scope/package/type fixes.

## Termux removal (user order 2026-10-29 ~04:03 IST: "Termux remove" — plan se REMOVED, ab code se bhi hatao)
- PROBLEM: plan se Termux hat chuka tha, par code me poora command path zinda tha — voice branch "termux me <cmd> chalao" + consent dialog + RUN_COMMAND intent + manifest permission/queries. Phase 6 entry me bhi darj tha ki ye alag build me hatega.
- TREE GREP (fresh tip 31f7e152 par, 109 code files API se fetch karke): "termux" sirf 2 files me mila — FloraApp.kt + AndroidManifest.xml. BackgroundMicService, AppAutomation, KNOWN_APPS, strings.xml me koi reference nahi. (GitHub code search ne 0 diya tha — private repo par search index par bharosa nahi kiya gaya, poora tree khud grep kiya gaya.)
- BUILD (sirf FloraApp.kt + AndroidManifest.xml):
  - FloraApp.kt (57 lines): state termuxConfirm + comment (213-215); voice branch (3938-3953) — trigger, consent speech, return; confirm dialog (4526-4563) — dialog + RUN_COMMAND intent send + success/fail messages.
  - AndroidManifest.xml (6 lines): Termux permission comment + line (22-24); queries me com.termux package comment + line (32-34).
  - Aas-paas chheda nahi: WhatsApp read branch, voiceAppConfirm dialog, guardianPinPending dialogs (2, dono intact), voiceCallConfirm/voiceSmsConfirm states, Shizuku queries package, MAIN/LAUNCHER intent, queries block — sab grep se verify.
  - Docs me historical zikr chheda nahi (sirf code + manifest).
- Prepush checks: braces/parens balanced (FloraApp 1699/1699, 3382/3382; Manifest 0/0, 6/6). Declaration order: koi naya declaration nahi (sirf removal); termuxConfirm ke saare uses usi ke saath gaye — post-edit tree grep 0. Koi fun/property-setter clash nahi. Sed use nahi hua (python slice removals, har anchor verify — pehle attempt me guardianPinPending end anchor 2 baar mila tha, isliye dialog slice ko start-index se next-anchor par kiya gaya; us attempt me koi partial write nahi hua tha — freshness dobara verify karke hi edits lagaye gaye). Koi key/password file me nahi; web/download.html nahi chheda; workflow file nahi chhedi; koi naya permission nahi (ek permission HATA hai).
- LIMITS (imaandaari se): "termux me ..." bolne par ab koi Termux branch nahi — command research/brain raaste par jayegi (koi special message nahi joda gaya — scope sirf removal tha); Termux app khud phone par rahe to bhi Flora usse command nahi bhejegi. Phone behaviour Phase 10 checklist me test hoga.
- IS BUILD ME NAHI: Phase 7/8/9, neural voice, server changes, overlay mic (alag scope), koi naya permission.

## Admin panel FULL build — 2026-10-29 (user order ~09:22 IST)
- Wajah: admin panel me approve nahi hota tha (admin flag nahi — users row nahi, app-link
  pending → admin-approve 403; ye code bug nahi hai, isliye approve ka code chheda nahi
  gaya), payments list RLS se adhoori thi (sirf apni rows), details (email, push presence,
  totals) nahi dikhte the, UPI sirf app-local tha, keys ka koi panel option nahi tha.
- web/admin.html (rewrite): Users & Licences (search + per-user card: email, device short,
  ADMIN badge, licence plan/status/expiry, push haan/naa, usage total); Payments (ALL
  statuses + filter buttons + detail: amount, UTR, note, user email, dates, reviewed_by,
  reject_reason; approve/reject wahi admin-approve call); App connection table (device +
  push presence + token updated_at; "last active" nahi likha); Usage totals + per-user
  table; Push (templates-only: trial_ending, pro_expiry; user select all/ek; all par sirf
  token wale, skip count ke saath); Keys (present/missing booleans + set form + confirm
  delete; value kabhi display/log nahi); UPI (current + edit + save; pay page ke liye);
  Audit (unchanged); Plans (view-only, owner-gated note); Secrets status text nayi
  reality par update. JS syntax node --check se OK; saare section ids ek-ek baar mile.
- supabase/functions/admin-list v2: + auth emails (listUsers id-match) + push-token
  presence (value kabhi nahi, sirf haan/naa + updated_at) + usage totals (mili rows ka
  jod). admin-payments NEW: admin JWT + is_admin check, full list status filter ke
  saath, limit 200, user email map; approve/reject isme nahi. admin-keys NEW: allowlist
  gemini/groq/deepgram, status/set/delete, value kabhi response/log me nahi. admin-
  settings NEW: allowlist sirf upi_vpa, get/set, basic VPA shape check. public-settings
  NEW: no auth, sirf { upi_vpa } (null jab unset); pay page anon-key Bearer bhejta hai.
- supabase/functions/brain-proxy v3 + tts-proxy v2: key resolution = env FIRST, else
  provider_keys DB row; koi key na mile to pehla wala 503; JWT gate/model chain/caps
  bilkul wahi.
- supabase/sql/admin_panel_tables.sql NEW: provider_keys + admin_settings (RLS ON, koi
  policy nahi — service-role-only by design) + honest comment (at-rest encrypted nahi;
  dashboard secrets zyada secure + pehle jeetenge).
- web/pay.html: public-settings se UPI ID dikhata hai; unset ho to imaandaar fallback
  ("UPI ID abhi set nahi hai — app ke Pro screen par dekho"); jhootha UPI kabhi nahi.
- DEPLOY (dashboard, alag browser step — is commit se deploy NAHI hota): order: (1) SQL
  file chalao, (2) functions deploy karo: admin-list (replace), admin-payments (new),
  admin-keys (new), admin-settings (new), public-settings (new), brain-proxy (replace),
  tts-proxy (replace), send-push (REDEPLOY — local copy se, repo me nahi hai; isme
  admin check + templates wala version live aana chahiye). admin-approve/licence-verify
  dobara deploy karne ki zaroorat nahi (pehle wale deploy ho chuke hain).
- LIVE TEST ke liye chahiye: admin unblock (user ka app-link phone step + is_admin=true)
  — bina iske admin functions 403 denge, ye expected hai, bug nahi.
- IMAANDAAR LIMITS: DB keys at-rest encrypted nahi (RLS + service-role-only hi
  protection); env precedence (dashboard secret hamesha jeetega); push templates-only
  (freeform title/body function reject karta hai; "all" sirf token walon ko jata hai);
  price edit owner-gated hi rahega; app-side UPI sync IS BUILD ME NAHI — panel UPI
  website pay page ke liye hai, app apna local VPA tab tak dikhata rahega jab tak app
  build server UPI na padhne lage (follow-up); usage totals mili hui rows (limit 1000)
  ka jod hain; emails 1000 users tak (listUsers paging cap).
- OLD DESIGN CHANGE (user order se supersede): 2026-10-28 design kehta tha keys sirf
  Supabase dashboard se set hongi, panel par kabhi nahi. User ne panel me key option
  maanga → panel-set keys ban gayi hain, par VALUES panel par kabhi display nahi hongi
  (sirf present/missing + set form); dashboard secrets zyada secure raasta bana rahega.

## Admin panel PHASE 2 build — 2026-10-29 (user order ~09:29 IST, CEO perspective)
- Wajah: user ne panel ko CEO perspective se bada + poora maanga — har cheez ka
  control, feature on/off, Flora ko globally train karna, pura server + live overview
  ek jagah, kuch nahi bache. Blueprint: docs/FLORA_FULL_APP_AUDIT_FOR_ADMIN.md
  (read-only audit; phase 1 files tree me the, deploy baaki tha).
- CONTRACT (Phase 8 app builder ke saath shared — EXACT yehi rakha gaya):
  POST functions/v1/feature-flags, anon-key Bearer, body {user_id?} optional →
  {flags: {talk, calls, sms, whatsapp, browser, macros, image_gen, screen_vision,
  camera, overlay_mic, wakeword, home, routines, form_fill}} — sab boolean.
  Logic: scope='global' rows base + scope=user_id override; jo key na mile → true
  (fail-open). Table read service-role se (RLS no-policy).
- supabase/sql/admin_panel_phase2.sql NEW: feature_flags (key, enabled, scope,
  PRIMARY KEY (key, scope)); global_instructions (id=1, instructions, knowledge);
  push_log (id uuid, user_id, template, sent_at, ok); heartbeats (user_id PK,
  last_seen). Sab par RLS ON, koi policy nahi (service-role-only by design).
- supabase/functions/feature-flags NEW: contract ke mutabiq; NO user-JWT check
  (global config, secret nahi); table na ho to bhi sab true (catch → defaults).
- supabase/functions/admin-flags NEW: admin JWT + is_admin check (admin-approve
  pattern); list/set/delete; key allowlist = contract keys; scope 'global' ya user_id
  (user scope par users row verify, warna 400); enabled boolean check.
- supabase/functions/admin-instructions NEW: admin check; get (text admin ko hi
  milta hai — instructions secret nahi hain); set (merge + upsert id=1, har field
  max 2000 chars); set response me sirf confirm + lengths (poora text wapas nahi).
- supabase/functions/brain-proxy v4 (v3 se): v3 logic (env→DB key fallback, JWT
  gate + user_id match, model chain, 4000 prompt reject) BARKARAAR + global_
  instructions prepend (STYLE ke saath; global pehle, combined global+prompt ≤ 4000
  par prompt truncate; global > 4000 ho to 4000 par cut). Table/row na ho to global
  khaali (fail-open, v3 jaisa jawab). Sirf proxy lane par asar — app-direct lanes
  apne prompts use karte hain.
- supabase/functions/heartbeat NEW: user JWT check + body user_id == JWT user
  (warna 403); heartbeats upsert (last_seen=now). Koi aur data nahi liya jata.
- supabase/functions/admin-overview NEW: admin check + aggregates (users total /
  linked / admins, Free/Pro split, 7-din expiry, approved revenue sum, pending
  count + rakam, pending actions = pending + unclaimed licences, aaj ka usage,
  recent payments 5 / audit 5 / push 10, heartbeats map + 24h count, warnings
  array jo section fail ho). Token/key value kabhi nahi. Function-to-function
  live probe nahi (counts hi kaafi hain).
- supabase/functions/send-push LOCAL EDIT ONLY (repo me nahi hai — git push me
  NAHI dala gaya): send attempt ke baad push_log insert (user_id + template + ok;
  token kabhi log nahi); table na ho to insert fail par send response wahi
  (best-effort catch). Header comment me local-only note joda gaya.
- web/admin.html: CEO Overview section SABSE UPAR (counts cards + recent
  payments/audit/push + warnings); Feature flags section (14 global toggles +
  per-user override form: user select + key select + ON/OFF/delete, confirm ke
  saath); Global brain section (instructions + knowledge textareas + save);
  Push history section (overview se); Users table (App connection) me Last seen
  column (heartbeats map se; "online" kabhi nahi likha). Naye functions deploy na
  hue hon to phase 1 jaisa imaandaar "deploy nahi hua" message (callFn 404
  pattern) — crash nahi. JS syntax node --check se OK; naye section ids ek-ek
  baar mile; phase 1 sections/logic chheda nahi (sirf renderAppTable me column
  + fillPushUsers me fillFlagUsers hook + loadAll me 3 loaders).
- web/admin-draft.html DELETE: audit me dead darj tha ("Purana draft" title);
  kisi page se koi link nahi (web/*.html grep 0) — push_flora_delete.py se
  isi commit me delete kiya gaya.
- DEPLOY (dashboard, alag browser step — is commit se deploy NAHI hota).
  ORDER: (1) supabase/sql/admin_panel_tables.sql (phase 1 wala — agar abhi tak
  nahi chala), (2) supabase/sql/admin_panel_phase2.sql, (3) functions deploy
  karo: phase 1 wale (admin-list replace, admin-payments new, admin-keys new,
  admin-settings new, public-settings new, brain-proxy replace → v4 wali file,
  tts-proxy replace) + phase 2 wale (feature-flags new, admin-flags new,
  admin-instructions new, heartbeat new, admin-overview new) + send-push
  REDEPLOY — LOCAL file se (repo me nahi hai; isme admin check + templates
  wala version + push_log insert live aana chahiye). home-sync function + table
  is commit me nahi hain (Phase 8 builder ke paas hain — deploy checklist me
  uske baad jodna). Dashboard verify notes (code mat chhedna): plans + settings
  tables ke columns dashboard se verify karo (schema unknown hai — panel me
  inke columns nahi dikhaye gaye); Google link web client (GOOGLE_WEB_CLIENT_ID)
  dashboard step hai.
- LIVE TEST ke liye chahiye: admin unblock (app-link + is_admin=true) — bina
  iske admin functions 403 denge, ye expected hai, bug nahi.
- IMAANDAAR LIMITS: flags ka on-device asar Phase 8 app client se aayega —
  phase 2 sirf store + UI + server config hai (brain-proxy me flag gating is
  build me nahi ki gayi); presence = last_seen only ("online" claim nahi);
  global instructions sirf proxy lane par asar karenge (app-direct lanes ke
  liye app build me instructions fetch chahiye — alag app change); app-link
  ke bina heartbeats khaali rahenge (heartbeat ke liye user JWT chahiye);
  overview limits: lists limit 1000 (payments/usage), listUsers 1000 users tak.

## Phase 8 — App build (2026-10-29, CI pending is entry ke waqt)
- SCOPE: app/ tree + home-sync function/SQL ONLY. web/ aur panel/LFMS
  functions (admin-list, admin-payments, admin-keys, admin-settings,
  public-settings, brain-proxy, tts-proxy, licence-verify, admin-licences,
  admin-plan-features, admin.html) CHHEDE NAHI — wo doosre builders ke
  paas hain. Coordination file docs/FLORA_BUILD_COORDINATION.md tip par
  mili NAHI (contents 404 + tree search 0) — padhne ki koshish ki gayi,
  banayi nahi (kisi aur builder ka map hai); scope overlap isliye nahi hua.
- (e) APP-LINK FIX — root cause (code se confirm, 3 hisse):
  R1: link button sync ka result ignore karta tha + linked UI sync se
  PEHLE set ho jati thi (token khali ho to sync hota hi nahi tha) — isliye
  server row bane bina "Linked" dikhta tha (user ka 2026-10-29 ~09:24 wala
  case: app me UNLINK dikha, server par row 0).
  R2: koi confirmation + koi retry nahi tha (app-start sync tha par uska
  result bhi ignore hota tha; restart na ho to retry kabhi nahi).
  R3: FloraMessagingService.onNewToken userId() (random UUID) bhejta tha,
  effectiveUserId() nahi — token refresh linked row ko kabhi repair nahi
  karta tha.
  FIX: prefs.linkServerConfirmed (naya pref, default false; clearLink ise
  bhi saaf karta hai). Link button: setLink ke baad confirmed=false, sync
  ka result dekha jata hai — ok:true par hi confirmed=true + "link ho gaya";
  token khali ya sync fail par imaandaar pending note (koi jhootha "Linked"
  nahi). Settings me "Link pending" + "Server sync dobara karo" button +
  Settings kholte hi ek auto-attempt (token ho to). App-start sync ka result
  ab dekha jata hai (linked + ok:true → confirmed=true). onNewToken ab
  effectiveUserId() bhejta hai + success par confirmed=true. Device_id kabhi
  invent nahi — sirf prefs.deviceId(ANDROID_ID). push-token-sync function
  CHHEDA NAHI (scope bahar); token-less row creation zaroori ho to server
  follow-up hai (parent ko report me diya gaya).
  RETRO-REPAIR: is build se pehle link kar chuke users ka local state
  linked=true + confirmed=false (naya pref default) hoga — agle app-start
  par sync ok:true aate hi confirmed=true + users row ban jayegi (phir
  admin flag lag sakta hai). User ko app restart karna hoga.
- (d) HOME-SYNC: HomeSyncClient rewrite — anon key CHHODI, user JWT
  (refreshAccessToken se, memory me hi, kabhi save nahi) + body
  {user_id (linked uid), device_id (ANDROID_ID path), box_code};
  unlinked ho to sync hota hi nahi (null). Function home-sync rewrite:
  JWT gate (brain-proxy step 2 pattern: anon client getUser → 401 nahi
  mile to; body user_id != JWT user → 403) + service-role upsert
  home_boxes (device_id PK, user_id, box_code, updated_at).
  SQL nayi file supabase/sql/home_sync_table.sql (create + RLS ON, koi
  policy nahi — service-role-only by design). HomeScreen: home flag gate
  + naya sync signature (prefs, boxCode, context) + imaandaar notes.
  DEPLOY dashboard se (checklist parent ko di gayi) — GitHub push se
  function deploy NAHI hota.
- (c) G17 PINNING — implement (OkHttp CertificatePinner NAHI, wajah:
  grep se OkHttp sirf DeepgramStreamClient me hai; Flora-domain traffic
  HttpURLConnection par hai, CertificatePinner use cover hi nahi karta).
  core/PinnedTrust.kt: TLS layer, default chain validation PEHLE, phir
  leaf-ya-intermediate SPKI pin check; fail = CertificateException, kabhi
  silent unpinned fallback nahi. Pins LIVE certs se (2026-10-29):
  supabase leaf B9zUXavotYYIs217h2OZnCqCTp2vB/2pNC1zDijcIAo= (expiry
  22 Jan 2027), vercel leaf xRUwi70J41GFIbtkDY38VXIAoWtbsGGG+LWfenCZh7k=
  (expiry 27 Nov 2026), shared Google intermediate
  47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=. Swap: LicenceClient,
  BrainClient proxy calls, WebsitePublisher, ModelDownloader, HomeSyncClient,
  FeatureFlags, HeartbeatClient, KokoroModelManager. NAHI chheda: direct
  Google calls (BrainClient discovery, VisionClient), SearchTools,
  ImageTools, DeepgramClient (Flora domain nahi). Rotation plan
  docs/FLORA_G17_PINNING.md me (leaf rotation intermediate pin se cover;
  intermediate badle to har release se pehle pins refresh + app update).
- (b) FEATURE FLAGS + HEARTBEAT — contract FINAL (panel phase 2 se match):
  POST functions/v1/feature-flags, anon Bearer, body {user_id?} optional,
  response {flags:{talk,calls,sms,whatsapp,browser,macros,image_gen,
  screen_vision,camera,overlay_mic,wakeword,home,routines,form_fill}}.
  Client core/FeatureFlags.kt: missing key / fetch fail → fail-open true;
  aakhri flags prefs me cache (offline). Enforcement: gateFeature()
  (FloraApp) + seedha isEnabled() (Settings/HomeScreen) — calls, sms,
  whatsapp, browser, macros, image_gen (dono branches), screen_vision
  (lookAtScreen + Live), camera (chat + routine steps), overlay_mic (ON
  hi; OFF kabhi gate nahi), wakeword (Switch + mic launcher service-start;
  band karna hamesha chalega), home (HomeScreen + client andar bhi),
  routines (poori queue), form_fill (3 branches), talk (main fall-through
  brain + routine brain step). OFF → "Ye feature abhi band hai." + koi
  action nahi. Settings screen kabhi gate nahi (recovery path). File Q&A
  aur code-likho talk flag se gate nahi (explicit feature commands).
  Heartbeat tools/HeartbeatClient.kt: foreground + licence check ke baad,
  linked hi, JWT header, body {user_id}; 404/koi fail → silent skip;
  sirf last_seen, koi content/location/screen data nahi. Doc:
  docs/FLORA_FEATURE_FLAGS.md.
- (a) NEURAL VOICE — Kokoro hf_alpha (user-final). Model download poora:
  models/KokoroModelManager.kt (PUBLIC HF URLs hexgrad/Kokoro-82M:
  kokoro-v1.0.onnx 325,532,387 bytes + voices-v1.0.bin 28,214,398 bytes,
  SHA-256 verify, .part + .ok gating, progress + 3 attempts, Settings UI
  section + Wi-Fi note; APK me bundle NAHI). Routing poora:
  assistant/NeuralSpeaker.kt (engine hook) — VoiceEngine.speak() +
  Call/Event announcement receivers isi se (announcements me free-talk
  counting kabhi nahi). Sanitizer single-source (NeuralSpeaker.sanitize;
  VoiceEngine delegate). SYSTEM TTS IS BUILD ME NAHI HATAYA (gate:
  hatana sirf phone test pass ke baad; Lily profile barkaraar).
  NATIVE PLAYBACK ENGINE BLOCKED WITH EVIDENCE (koi guess nahi, koi
  cloud TTS fallback nahi): official docs me NDK source build hi hai,
  koi verified Maven AAR nahi; ek blog ka JitPack coordinate
  (com.github.k2-fsa:sherpa-onnx-android:1.10.0) single claim, official
  source se unverified + us version me Kokoro support unverified; NDK
  step Flora CI (JDK-only) me nahi chalega aur workflow API se chhooi
  nahi ja sakti; raw ORT route ko espeak-ng + unverified model I/O
  chahiye. Model ready hone par bhi aaj system TTS hi bolega; engine
  judte hi NeuralSpeaker.engine set karna hi kaafi hoga. Doc:
  docs/FLORA_NEURAL_VOICE.md (unblock raasta + limits: no resume ~354MB,
  phone RTF unknown, chunking Phase 10 phone test ke baad).
- LICENCE-VERIFY v2 CLIENT (coordination note se align, server LFMS
  builder ka hai — app ne function NAHI chheda): LicenceStatus me
  valid/bound_device_short/reason/features/expires_at + honestMessage().
  verify(): Pro sirf valid+active+plan==pro par (revoked/expired/
  superseded/bound_mismatch par plan free — revoke ke baad Pro chalna
  bug hota). Purani shape backward compatible (valid na ho to
  valid = status=="active"). Messages: revoked → "licence revoke ho
  gayi hai"; bound_mismatch → "ye licence kisi aur device par lagi
  hai (device: short) — rebind sirf admin panel se, apne aap nahi";
  expired/superseded waisa hi; active/local_free/error par koi message
  nahi. Surfaces: app-start chat message (voice nahi, greeting se
  takrao na ho), header licence line, LicenceScreen (status line +
  expiry + refresh note). Device binding rule: first claim bind, auto-
  rebind kabhi nahi — app rebind ki koshish kabhi nahi karti. Purana
  "15 din transfer" text LicenceScreen se hataya (v2 rule se takrata
  tha). Doc: docs/FLORA_LICENCE_V2_CLIENT.md.
- PREPUSH CHECKS: 21 kt files brace/paren/bracket balance 0; naye states
  (neuralStatus/neuralProgress/linkConfirmedDisplay/gateFeature) ek-ek
  baar declare, use se pehle; nullable ?: return / null-check pattern;
  koi setter clash nahi; koi greedy sed nahi (sirf muse.edit); koi
  workflow file nahi; koi [skip ci] nahi; koi secret/key/password
  read/push nahi (pins = public cert hashes, HF URLs = public).
- IMAANDAAR LIMITS: phone behaviour Phase 10 me test hoga (is build ka
  koi phone test nahi hua); flags fail-open chalenge jab tak panel phase
  2 server functions dashboard se deploy na hon; v2.22 APK flags ko
  ignore karega (asar sirf is build ke baad wale APK se); home-sync +
  heartbeat tabhi chalenge jab linked account ho (JWT chahiye);
  G17: CA badle to app update ke saath pin refresh zaroori (fail-closed);
  Kokoro download me resume nahi.

- LFMS SERVER BUILD (licence + features management, Wave A server, 2026-10-29):
  Base tip 6e36b72d se shuru, Phase 8 push (5ca6c4e8, sirf app/ + home-sync +
  docs) se takrao nahi — compare se verify kiya gaya; audit log push se
  pehle tip 0aa656b8 se fresh liya gaya (Phase 8 entries barkaraar).
  Audit: docs/FLORA_LICENCE_SYSTEM_AUDIT.md (sections A–H) + parent
  correction 09:59 (15-day rule barkaraar) + F8 plaintext-with-controls.
- CONTRACT (server shapes, Wave B isi par banega):
  Key FLORA-XXXX-XXXX-XXXX (A-Z + 2-9, no 0/O/1/I; server generate;
  unique index; collision par retry). Key ADDITIVE hai — keyless
  payment rows bina key ke waise hi chalengi. valid=false SIRF tab
  jab blocked/invalid ho (revoked/expired/superseded/device_locked/
  invalid_key); free tier (koi pro nahi) valid:true + plan 'free'
  hai (error nahi). Pro sirf valid+active+plan==pro par.
  15-DAY RULE (correction se, audit ke bound_mismatch sujhav ko
  override karta hai): mismatch par 15 din na hue hon -> plan
  free + status free + device_locked + device_change_available_at
  (purani device par chalti rehti hai); 15 din poore -> TRANSFER
  (device_id + activated_at update, device_changed:true). NULL-claim
  par koi wait nahi. Admin rebind = wait ka bypass (device NULL +
  audit). Ek user ki ek active licence (nayi purani ko superseded
  karti hai, superseded_by set hota hai). Renewal stacking default
  (admin-approve): nayi expiry = max(ab, pehli expiry) + 30 din.
  plan_features hi plans ka single source hai (plans table ko koi
  code read nahi karta). Masked default (last 4) + click reveal +
  copy (panel + user apni key apne JWT se).
- FILES: supabase/sql/licence_system.sql (licence_key unique index,
  revoked_reason/by/at, superseded_by, status check me superseded,
  (user_id,device_id) unique index, plan_features + seed free=[talk]
  / pro=14 flags, RLS ON no policy; device_id nullability NAHI
  chhedi — deploy-check); licence-verify v2 (JWT gate + user match,
  key path, v1 logic preserve, features[], bound short, key kabhi
  response me nahi); brain-proxy v5 + tts-proxy v3 (licence gate:
  Pro valid ya Free usage_today<600, nahi to 429 free_limit; gate
  sirf padhta hai, increment nahi; DB error par fail-open; device
  binding yahan check nahi hoti — binding licence-verify par);
  admin-licences (create ek submit=ek licence + full key EK BAAR,
  list masked, revoke reason+audit, rebind, reveal+audit);
  admin-plan-features (get/set, 14-key allowlist); licence-key
  (user apni key apne JWT se: list masked + reveal + audit);
  admin-approve MINIMAL (website-path hi: key generation + supersede
  + stacking; app-path byte-identical; approve/reject/audit flow
  waisa hi); admin-list v3 (licences select me id/device/
  activated_at/masked last4 — full key kabhi nahi); usage-count v2
  (JWT gate: linked=JWT+match, unlinked=device-resolve-or-counted:
  false; seconds/talk_seconds alias fix — app `seconds` bhejta tha,
  v1 sirf talk_seconds padhta tha isliye reports 0 add karti thi;
  response me counted+detail); push-token-sync v2 (linked=JWT+match
  tabhi users row claim; unlinked=device-resolve, row claim nahi —
  fresh unlinked ka token link ke baad hi judega, ye security
  trade hai, regression nahi); web/admin.html (Licences section:
  create + list + revoke/rebind + masked/show/copy; Plan features
  editor; Users section E1 fix: saari licences dikhti hain, sirf
  aakhri nahi); web/pay.html C2 text (website-path ii se match).
  home-sync + app/ bilkul nahi chhua gaya.
- PREPUSH CHECKS: 10 function files brace/paren/bracket balance 0;
  admin.html JS node --check PASS; naye element IDs ek-ek baar;
  licence-verify ke kisi response me licence_key nahi (grep verify);
  koi workflow file nahi; koi [skip ci] nahi; koi secret/key/
  password read/push nahi.
- DEPLOY CHECKLIST (ORDER — dashboard, is commit se nahi hota):
  licence_system.sql PEHLE (uske bina naye columns nahi honge;
  admin-list v3 tab tak licences khaali dikhayega, crash nahi) ->
  panel tables phase1/2 (agar baaki) -> functions: licence-verify
  v2, brain-proxy v5, tts-proxy v3, admin-licences,
  admin-plan-features, licence-key, admin-approve minimal,
  admin-list v3, usage-count v2, push-token-sync v2, baaki phase1/2
  set, send-push local redeploy, home-sync Phase 8 se -> schema
  checks (dashboard se, code se assume nahi): licences.device_id
  nullable? (NULL-claim ke liye chahiye), (user_id,device_id)
  unique (SQL me index hai, ban gaya ya dupes ki wajah se fail
  hua?), payment_submissions INSERT policy, core tables RLS policy
  list, har function ki verify_jwt setting, plans/settings columns.
- IMAANDAAR LIMITS + COORDINATION FLAGS (Wave B / parent ke liye):
  (1) Phase 8 client (FLORA_LICENCE_V2_CLIENT.md) bound_mismatch +
  "15-day text hataya" par bana hai — server correction ke mutabiq
  device_locked/device_changed bhejta hai, bound_mismatch kabhi
  nahi. Wave B me client parsing + 15-day text wapas laana hoga,
  nahi to locked users ko "kab khulegi" nahi dikhega.
  (2) Unlinked (random-UUID, bina login) app verify v2 JWT gate par
  401 degi — ye parent decision khula hai (audit D1): linked users
  JWT bhejen (Wave B must-do), unlinked ke liye key-only path ya
  link-required — server me koi bypass NAHI lagaya gaya hai
  (correction ka letter mana gaya hai).
  (3) Unlinked usage reporting tab tak counted:false rahegi jab tak
  app countUsage me device_id na bhejne lage (Wave B ek line ka
  change); linked path JWT se turant chalega.
  (4) On-device feature enforcement Wave B app build se aayega —
  server brain/tts gate is build me hai; D8 (app-direct lane) ka
  faisla user se pending hai, server kaam us par ruka nahi hai.
  (5) Free-cap day-boundary: server UTC day (usage-count) vs app
  local day me ghanton ka farq ho sakta hai (audit D6) — gate
  server UTC day par chalta hai.
  (6) Live admin test admin unblock (app-link + is_admin) ke baad
  hi hoga; usse pehle admin functions 403 denge by design.
  (7) DB-leak caveat: licence_key plaintext hai (F8 user order ki
  wajah se) — service-role-only + masked UI hi protection hai,
  panel DB keys wali caveat jaisi hi.

- LFMS v2.1 ADDITION (licence-verify, website account card ke liye):
  device_id ab OPTIONAL hai. device_id na mile to STATUS-ONLY view
  milta hai (plan/status/expiry/features, sirf JWT user ka apna
  status) — bina binding decision, bina claim, bina transfer ke
  (koi write nahi). Website account.html ka licence card isi par
  chalega (website par device_id hota hi nahi). device_id mile to
  poora v2 logic (binding/claim/transfer) waisa hi chalega. JWT
  gate + user match dono raaston par lagta hai. Key + no-device
  par bhi status-only (claim nahi hoti).

## Website user-side build — 2026-10-29 (user order ~10:06 IST, audit docs/FLORA_USER_SIDE_AUDIT.md items B5/B6/B7/B9/D2/D3/B3/B8)
- Scope: web/ user pages ONLY (account/login/signup/index/pricing/download + nayi privacy/terms/reset-password). pay.html/admin.html/app/supabase/functions MAT chhue (LFMS server builder ke paas hain) — koi takrao nahi, koi duplicate kaam nahi.
- login.html: "Password bhool gaye?" link → reset-password.html; confirmation resend button (auth.resend signup); auth errors Hindi-friendly mapping (invalid credentials / not confirmed / too many requests / already registered / password length / invalid email / expired link).
- signup.html: wahi resend + Hindi mapping; D2 stale text theek ("bina link ke bhi approval hota hai; Pro dikhane ke liye link karo"); confirm hint me resend ka zikr.
- reset-password.html (NAYI): request flow (resetPasswordForEmail, redirectTo reset page) + set flow (session maujood ho to updateUser se naya password); session-fallback note (link home page par khule to login session ke saath wapas aao).
- account.html: licence card (licence-verify v2.1 status-only view: user JWT + {user_id} only, device_id kabhi invent nahi; plan/status/expiry render, key kabhi nahi; v2.1 deploy se pehle imaandaar "jald dikhega" note, crash nahi); payments list me reject_reason bhi; D2 stale note theek; refresh button licence bhi reload karta hai.
- privacy.html (NAYI): app LegalScreens text se adapt — retention SAHI: chat memory long-lasting (time-prune nahi) + journal/dated/Auto Log 30 din + licence/payment record alag; website account data + support.
- terms.html (NAYI): verified facts only — plans (Free 600s/day, Pro ₹199/mahina), 1 user = 1 licence = 1 device + 15-day transfer + admin rebind, UPI + UTR human-verify approval (auto-approve kabhi nahi), sideload + SHA note, support; refund/grievance policy invent NAHI ki ("support se sampark karein").
- index.html: privacy text D3 fix (30-din auto-expire claim hataya, sahi retention likhi); "conversation mode" vague phrase hataya (live wallpaper note barkaraar).
- pricing.html: "conversation mode" hataya; D2 note theek (bina link ke bhi approval hota hai).
- download.html: SHA-256 verify guide card joda (content ONLY — version/link/expiry/SHA value chheda nahi, wo Phase 10 ka kaam hai).
- Nav: privacy/terms links sirf in pages par jode (index/pricing/account/login/signup/download/privacy/terms/reset-password); admin.html/pay.html ki nav MAT chhedi.
- Verify: 4 inline scripts node --check PASS; har page par IDs unique; stale texts (conversation mode / auto-expire / approval rukega) meri pages par 0; secrets scan: sirf pehle se maujood publishable anon key, koi nayi key/password value nahi.
- Deploy note: web/ push se Vercel auto-deploy hoga. Dashboard step: Supabase Redirect URLs me https://flora-henna-alpha.vercel.app/reset-password.html add karni hai (list abhi empty → sirf Site URL; add na ho to link Site URL par khulega aur session-fallback path se kaam chalega). Account licence card LFMS licence-verify v2.1 status-only view par chalta hai — deploy ke baad live, usse pehle imaandaar note.
- Imaandaar limits: reset flow + licence card ka live test deploy + admin unblock ke baad hoga (koi live account create/submit test is build me nahi hua); D4 (Free "saare tools" vs "basic tools") + D5 (documents/email line) + pay.html ka D2 text is build me nahi chhue (pay.html LFMS builder ke paas hai; D4/D5 follow-up me).

- Wave B LFMS APP BUILD (2026-10-29, user orders 09:57 LFMS + 10:09 D8 dono-gate + user-side audit scope addition):
  Scope: app/ + docs only (functions/web LFMS server builder ke paas, chhua nahi).
  (1) Licence fill screen (LicenceScreen): key field + verifyWithKey (FLORA-XXXX format) + saved-key masked display (last 4) + Dikhao/Chhupao + Copy (clipboard + Toast). Key prefs me save hoti hai (sirf verify success par); unlink par clear nahi hoti (key account link se alag hai).
  (2) Honest messages (Phase 8 v2 client par): revoked / bound_mismatch / expired / superseded + device_locked (server flag + v1 free+locked shape dono parse; transfer date dikhti hai) — app-start chat (no voice), header licence line, LicenceScreen teeno par.
  (3) 15-day transfer display RESTORE: Phase 8 ne LicenceScreen se transfer text hataya tha (v2 ko "no transfer" samajh kar) — wapas likha gaya (15 din poore par transfer, pehle device_locked, admin rebind turant). verify() ka stale comment bhi transfer wale sahi text se badla gaya.
  (4) Plan-features + flags: PlanFeatures object (licence-verify features[]; empty/never = fail-open) + gateFeature + routine stepBlocked + HomeSyncClient(home) par dono-layer check; Settings screen kabhi gate nahi hoti (recovery path).
  (5) D8 app-side gate (LicenceGate): BrainClient.ask se PEHLE licence check (15-min cache; Pro valid → Allowed; revoked/expired/superseded/bound_mismatch/device_locked → Blocked + message, NO provider call; free/unknown → local 600s/day counter). Deepgram streaming start par bhi wahi gate (BargeIn arm site + FloraRecognitionService; blocked par stream shuru hi nahi). User keys ka model NAHI badla (koi server-keys routing force nahi).
  (6) Link-pending UX (Phase 8) barkaraar — chheda nahi (Link pending + retry + Settings-open auto-attempt + app-start retry).
  (7) JWT for linked users (server builder must-do): verify + countUsage par linked refresh token se user JWT (SessionTokenCache, memory-only, 55-min reuse, Mutex single-flight, rotation save) — anon-only v2 par 401 dega. v1 ko nahi todta (v1 header padhta hi nahi). Unlinked anon hi bhejega (v2 401 → error → local mode). Key path par JWT NAHI (key khud credential hai — OPEN QUESTION parent/server builder ko: agar server key path par bhi JWT maangta hai to unlinked key users tootenge).
  (8) countUsage: device_id body me (stored prefs se — MainActivity call site waisa hi) + linked JWT (server builder note: unlinked reporting counted:false tab tak jab tak device_id na aaye).
  (9) Scope addition (user-side audit): A6 LicenceScreen par "Website se UTR submit karo" button (in-app browser me /pay) + step text imaandaar (app UTR sirf LOCAL note, asli submit website se); A3 free-limit "kitna bacha hai" display (chat header + Settings + LicenceScreen — gate wala hi counter, LicenceGate.freeRemainingSeconds, do sach nahi); D1 Pro screen UPI server public-settings se EK source (unset → "UPI abhi set nahi hai", local VPA kabhi nahi); D9/D10 stale comments sahi kiye (transfer text + cap proxy/app gates me hai, usage-count sirf report karta hai).
  Limits (imaandaar): phone test Phase 10 me hi hoga; client-side gate ko hacked app bypass kar sakta hai (server gate proxy lane par doosra hissa hai); flags fail-open barkaraar; server UTC-day vs app local-day ka farq aadhi raat ke aas-paas ho sakta hai (app "aaj" = local day, user ki nazar se sahi); key-verify + public-settings + JWT-gated verify live server v2 deploy ke baad hi poori tarah chalenge (abhi v1 par anon path + local fallback).

- APP TEAM CYCLE 1 FIX (2026-10-29, app team worker; scope sirf ye 11 — reserved files chhue nahi: LicenceScreen, LicenceGate.kt, usage/limit display, Pro UPI + comments, supabase/functions/, web/, coordination doc, home-sync function, admin actions, APP-C1-012 GuardianGate timeout HELD):
  Push 1 (commit 81801d531fe2c839041a32ea4a8fdf09c5296ffc; base tip move note: fetch tip 616d023c66 tha, push waqt tip dfd91e2743 tha — wo server-team commit tha, 15 files sab functions/web/docs, meri 6 app files se ZERO overlap, isliye koi overwrite nahi):
  (001) SetupWizard.kt — mic deny par bhi aage: step 2 par "Mic ke bina aage badho (sirf likh kar)" TextButton; isGranted ab sirf system check (pehle grants map ka purana denial false system check ko daba deta tha — Settings se grant karke wapas aane par bhi "Aage badho" stale-disabled rehta tha, har launch gate atak jata tha); intro + mic row wording sach par (bina mic sirf likh kar baat). FloraApp gate read (515) / wizard call (1009-1015) chheda nahi — skip path step 3 → "Ho gaya" → gate done, isliye atakna band.
  (002) voice/BargeInController.kt — start() ab suspend (ekmatra caller FloraApp scope.launch me hai) + stream khulne se pehle wahi LicenceGate.check jo FloraRecognitionService me hai (Blocked → false, koi Deepgram call/kharcha nahi). FloraApp call-site gate barkaraar (double-check, 15-min cache se sasta).
  (003) voice/BackgroundMicService.kt — background vision path (isScreenQuestion) par screen_vision flag (FeatureFlags + PlanFeatures, fail-open waisa hi) + LicenceGate (blocked/free-cap → wahi message, vision call nahi); background image branch par image_gen flag + LicenceGate. isCommand routing (foreground + confirmation) CHHUA NAHI — image branch aaj isCommand se shadowed hai (foreground queue pehle pakadta hai), gate isliye hai taaki routing badalne par bhi path kabhi ungated na chale.
  (005) data/FloraPreferences.kt — 12 secrets ab KeyStore AES/GCM (MemoryCrypto pattern) se encrypted: Gemini/Groq (+2), Deepgram, Tavily, Brave, SerpAPI, Maps, linked refresh token (setLink me bhi encrypted), licence key, FCM token. Migration-safe: purana plaintext ek baar padh kar encrypted save, phir plaintext mit jata hai (readSecret me ek baar ka re-save); saare readers/writers prefs methods se hi jaate hain (grep: DataStore keys ko koi aur file seedha nahi chhuti); non-sensitive prefs plaintext waisi hi; Supabase anon key publishable by design isliye encrypt nahi (Phase 6 audit ka "encrypted DataStore" overclaim ab sach hai — sirf in 12 keys par). KeyStore-bound limit: naye phone/restore par purana ciphertext nahi khulega (PIN vault jaisa niyam) — tab key dobara daalni hogi. Koi key value is entry me nahi.
  (010) ui/PcLinkScreen.kt — title "PC Link (taiyaari)" + intro/code/naya-code notes par imaandaar label: PC client abhi nahi hai, code abhi kahin daala nahi ja sakta. PC CLIENT NAHI BANAYA (out of scope).
  (011) assistant/VoiceEngine.kt — free-cap counting ab asli bola hua time: onStart (sachhi shuruaat) se onDone/onError/stop (interrupt) tak ka elapsed, seq-guard ke saath (purani utterance ka late callback nayi ko count nahi karega; bina onStart ke kuch count nahi — bina bole count nahi, jo sahi hai). Speak-start wala ~13 chars/sec andaza hataya gaya; cap ka matlab (600s/day) waisa hi. Neural + system TTS dono paths par.
  Push 2 (commit 8653a54528e740dd00597ef496c6fa2d6b9f3066; tip Push 1 wala hi tha, koi move nahi):
  (004) ui/FloraApp.kt menu — Camera dekho → gateFeature("camera"), Live Screen → gateFeature("screen_vision"), Browser → gateFeature("browser") (chat commands wali hi keys: 745/752, 3032/3155, 3750/3757). Blocked ho to menu khula rehta hai, gateFeature wajah bol + likh deta hai. LiveScreen/CameraLookScreen/BrowserScreen files me alag gate NAHI lagaya — saare entry routes (menu + chat) ab gated hain; ek jaanboojh kar exception: LicenceScreen se /pay kholne wala Browser route (Wave B A6 UTR submit) — wo browser feature entry nahi, payment submit path hai, isliye us par gate nahi (gate lagta to pay toot jata).
  (006) ui/FloraApp.kt:828/4133 (dono publish branches) — raw exception TTS se hataya, warm Hindi line ("Arre, website publish nahi ho payi — thodi der baad dobara koshish karo."); raw exception chat me bhi nahi daala gaya.
  (007) ui/FloraApp.kt:4907/5402 — Settings notes se raw server error hataya: Google link → "Google se link nahi ho paya, dobara koshish karo."; website link → "Account link nahi ho paya — email aur password dobara check karke koshish karo."
  (008) ui/FloraApp.kt header licence line — raw enum ("free (error)" / "free (local_free)") hataya: ab "Licence: Pro/Free — server se verified / local mode"; khaas status ka warm message honestMessage() se neeche waisa hi aata hai.
  (009) ui/FloraApp.kt home voice + ui/HomeScreen.kt — homeServerSynced flag (prefs, Push 1 me joda): HomeScreen "Server se sync karo" safal → true, naya code save / box hatao → false, fail par flag waisa hi (purana safal sync server row ko nahi mitata). Voice ab flag check karke bolta hai: code nahi → jodo + sync karo; code + synced → "sync bhi ho gaya hai, par relay control ka server hissa abhi baaki hai" (koi jhootha "sync baaki" nahi); code + unsynced → "sync baaki hai" + Home se sync karo. Control ka na hona teeno me sahi rakha hai.
  Pre-push checks: braces/parens balance 0 (saari files), declaration order grep (helpers/vars use se pehle), koi greedy sed nahi, koi [skip ci] nahi, workflow files chhue nahi. CI GREEN (API se verify): Push 1 run 40821141548 completed/success; Push 2 run 40821370338 completed/success. Saare 11 fix (001-011) isi cycle me code-complete + CI-green; phone test alag phase ka kaam hai, is cycle me claim nahi.
