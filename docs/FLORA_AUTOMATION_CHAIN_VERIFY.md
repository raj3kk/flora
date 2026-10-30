# FLORA AUTOMATION CHAIN — CODE-PATH VERIFY (production batch 2026-10-31)

"Fully automated" ka daava code paths par check kiya gaya hai.
Phone behaviour Phase 10-gated rahega — ye doc sirf code path
ka verify hai, phone proof nahi.

## 1. Planner verified-step execution — PASS (code path)

- `planner/PlannerEngine.kt` runLoop: step sirf proof par VERIFIED
  hota hai (`verifiedAtMs` ke saath); bina proof ke VERIFIED
  kabhi nahi (confirmMacroStep bina proof ke hataya hua hai —
  BLOCKED hi rehta hai).
- Voice-plan raasta: `ui/FloraApp.kt` executeVoicePlan →
  `automation/VoicePlanRunner.execute` (Host: messages/speak/
  AutoLog). Pacing fix (commit 53bb694a) isi raaste par hai:
  APP_OPEN foreground poll 3s, IN_APP_SEARCH settle poll ~3s.
- PlannerEngine runLoop levels parallel-by-design hain (dependsOn)
  — ye M-spec hai, voice-plan ki step-by-step shikayat se alag
  raasta; chheda nahi gaya.

## 2. Routines — PASS (code path)

- `automation/JobScheduler.kt` schedule/cancel/rescheduleAll;
  `automation/JobAlarmReceiver.kt` fire par: due-flag
  `flora_job_due` me likhta hai + notification + agle din ka
  alarm dobara lagata hai. Receiver khud routine nahi chalata.
- App khulte hi due-jobs block (`ui/FloraApp.kt` ~1894): Auto
  Mode ON ho to routine khud chalta hai, warna user ko sirf
  bataya jata hai. Yahi rule M5 triggers ke `routine` action
  par bhi lagta hai (wahi due-store).
- Boot par `BootReceiver.kt`: JobScheduler.rescheduleAll +
  ReminderScheduler.rescheduleAll + TriggerScheduler.rescheduleAll.

## 3. Macros (teach-by-doing + Auto Log) — PASS (code path)

- `automation/MacroEngine.kt` runMacro: fromStep se resume,
  verified app-open, sensitive tap par confirm (confirm ho chuka
  step dobara confirm nahi maangta — dead-loop fix), password
  field guard. Runner Done par AutoLog me "Macro: <naam> ->
  ho gaya" likhta hai — wahi runner log planner-macro confirm
  ka proof hai.
- Cross-app JSON macros EXCLUDED hi hain (§E) — Flora macros
  user ke teach-by-doing se bante hain, Maya ke 5 built-in
  JSON macros copy nahi kiye gaye.

## 4. Auto Mode ON behaviour — PASS (code path)

- Voice plan: autoMode ON ho to planConfirm dialog nahi —
  executeVoicePlan seedha chalta hai (plan dikhakar, confirm
  upfront-once rule ke mutaabiq).
- Due routines: upar §2 ke mutaabiq Auto Mode ON par khud.
- AutoModeNotifier + Auto Log entry har ON/OFF par.

## 5. Reboot-resume last VERIFIED step se — PASS (code path)

- `planner/PlannerService.kt` PlanResumeWorker: boot/app-close
  ke baad interrupted RUNNING plan → PAUSED + AutoLog entry +
  notification ("last verified step se resume hoga").
- `planner/PlannerEngine.kt` resumePlan → launchRun: runLoop
  VERIFIED steps skip karke pehle non-VERIFIED step se aage
  badhta hai — dobara verified kaam nahi hota.
- Boot par `BootReceiver.kt` PlanResumeScheduler.enqueue.

## Fix is batch me: KOI NAHI

- Upar ke paanchon raaste code par toote hue nahi mile — isliye
  is batch me automation ka koi code fix nahi hua. Ye "koi
  bug nahi mila" ka record hai, "phone par chalta hai" ka
  daava nahi. Phone par jo bhi atke, wo Phase 10 me aayega
  aur tab fix-cycle me jayega.
