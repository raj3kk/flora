# Spec — Core (foreground service + receivers + schedules)

Source: behaviour only — manifest components, videos (reminders, driving
mode, call announce, scheduled agent). Folder: core/ (ABHI KHAALI HAI).

## Files (honi chahiye)
- core/MayaForegroundService.kt — mic + camera + data-sync foreground
  types ke saath, notification ke saath; battery optimisation off user
  khud karega (app chupke nahi karega).
- core/BootReceiver.kt — restart ke baad service dobara start.
- core/PhoneStateReceiver.kt — call announce (contacts se naam), driving
  mode me preset message (WhatsApp + SMS) sirf user ke chune contacts par.
- core/DrivingOffReceiver.kt — driving mode band hone par normal mode wapas.
- core/MicCaptureReceiver.kt — mic capture trigger (wake word ke saath).
- core/ReminderReceiver.kt — time par reminder.
- core/SocialAlarmReceiver.kt — daily story / social schedule (auto-post
  sirf user-approved + official route se).
- core/MarketingAlarmReceiver.kt — marketing schedule (user-approved hi).
- core/ScheduledAgent.kt — scheduled background research agent + morning
  report jaisa kaam; lamba kaam background me, poora hone par khud batana.

## Behaviour
- Koi bhi receiver bina user ke schedule ke kuch post/send nahi karega.
- Call recording/transcript/summary sirf jahan legal ho + disclosure ke
  saath, kabhi silent nahi.
- Offline/weak-network par saaf batana ("ye nahi hua, isliye nahi hua"),
  guess nahi.

## Acceptance (Verify → Test → Audit)
1. Boot, call, reminder, schedule — har receiver ka phone test pass.
2. Koi crash / hidden exception nahi.
3. Mic off / incognito / delete kaam kare.
4. Koi purani API key code me nahi hai.
5. Test log docs me darj hai.
