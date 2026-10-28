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

## v1.9 Themes + Orb — push hua, CI result pending
- Code audit: THEME key + MaterialTheme wrap + warm/dark/light
  schemes (sirf colors); orb Canvas-only, busy par pulse, idle par
  static; theme states launcher se pehle (FloraApp 99, Settings 784
  < launcher 786); brace/paren balance 0.
- Test-plan entry: docs/FLORA_THEMES_TEST_ENTRY.md (7 steps).
