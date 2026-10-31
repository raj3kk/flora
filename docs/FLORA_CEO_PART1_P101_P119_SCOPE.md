# CEO P1-01 / P1-19 Scope Confirm (Part 1, 2026-10-31)

**PLAN DOC MAT CHHEDA GAYA HAI.** Plan ka amendment section
(parent ke paas, pehle se darj) waisa hi hai. Ye doc sirf code
scope confirm karta hai — evidence ke saath. Verify base: live
tip (neeche lines D-11 push ke baad ke code ki hain).

## P1-01 — 1 phrase "Hey Flora" (scope confirm)

- **Shipped scope = 1 phrase.** `core/FloraConfig.kt:103`:
  "Wake phrase DECIDED hai: \"Hey Flora\" (user decision
  2026-10-28)". `wakeword/WakeWordService.kt:26` header: "Hey
  Flora wake-word listener — v2.4 v0, default OFF".
- **Enrol ki "3 baar" = ek phrase ki 3 repetitions.**
  `ui/FloraApp.kt:6533` button: "Enrol: 3 baar 'Hey Flora' bolo"
  — enrol loop me 3 samples (har ek "Hey Flora" ka MFCC mean)
  liye jate hain aur unka average template
  `BiometricTemplates.storeWake` se save hota hai. Ye 3 alag
  phrases NAHI hain — ek hi phrase ki 3 repetitions hain.
- **Galatfehmi se bachne ke liye saaf:** `ui/VoiceEnrolScreen.kt`
  wali "3 baar" enrol ALAG cheez hai — wo speaker-voice-match
  enrolment hai (user kuch bhi bole, jaise "Namaste, main
  Priyadarshi hun"), wake phrase enrolment nahi hai. P1-01 ka
  "3 baar" FloraApp wale wake enrol (upar) se hai.
- **Aage ka flow phone-pending:** wake+command ka aage ka flow
  aur multi-phrase + sleep/wake NAYA feature hai (CEO amendment
  ke mutabiq) — is cycle me koi naya phrase, koi sleep/wake
  code nahi banaya gaya hai.

## P1-19 — consented MediaProjection fallback (scope confirm)

- **Code DELETE nahi hua hai.** `automation/ScreenCapture.kt`
  tip par maujood hai (is cycle me chhua hi nahi gaya hai).
- **Sirf caller-supplied resultCode/data:**
  `automation/ScreenCapture.kt:74` —
  `getMediaProjection(resultCode, data)` — `openShared` ko jo
  resultCode/data caller deta hai wahi istemal hota hai; ye
  class khud kabhi permission create nahi karti.
- **Grant sirf system dialog se user Allow par:**
  `ui/LiveScreen.kt:182-183` aur `:288-289` —
  `createScreenCaptureIntent()` launcher (system dialog) se
  grant milta hai. Bina Allow ke ek pixel bhi nahi milta
  (ScreenCapture header me bhi darj hai).
- **Stored grant sirf in-memory:** `automation/ScreenCapture.kt:47-52`
  `LiveSessionHolder` (app-level var, koi persistence nahi) —
  process marne par Android dobara permission maangega (OS ka
  niyam, header me imaandaari se darj hai).
- **Clear paths paired (har open ka band raasta hai):**
  `LiveSessionHolder.clear()` (`ScreenCapture.kt:49-52`,
  `session?.stop()` + null) call sites: `ui/LiveScreen.kt:179,293`,
  `ui/FloraApp.kt:848,4044,4150,4221,5735`; `Session.stop()`
  (`ScreenCapture.kt:133-135`, `projection?.stop()`); projection
  marne par `MediaProjection.Callback.onStop` (`:83`) session ko
  dead mark karta hai.
- **Criterion (CEO amendment ke mutabiq):** "0 silent/app-wide
  uses" — MediaProjection ke saare call sites upar wale do
  consented surfaces (ScreenCapture fallback + LiveScreen /
  FloraApp live-screen launchers) par hain; koi background /
  silent / app-wide capture path grep me nahi mila hai. Koi
  silent use is cycle me joda nahi gaya hai.
