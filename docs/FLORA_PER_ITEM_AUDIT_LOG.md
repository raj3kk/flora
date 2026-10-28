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
