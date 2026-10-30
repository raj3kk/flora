# FLORA EVENT TRIGGERS — SPEC (parity batch 2026-10-31, M5)

## Records se kya mila (verified, isse zyada daava nahi)

- Maya records me `event_triggers` sirf ek naam ke roop me mila hai
  ("power/creator areas" list me) — iska vyavhaar (kaunse trigger,
  kya action, kis condition par) kahin recover nahi hua.
- Jo mila hai: Maya me exact-alarm receivers the — ReminderReceiver,
  SocialAlarmReceiver, MarketingAlarmReceiver (alarm par jobs).
- Isliye Maya ke event-trigger semantics **UNVERIFIED** hain aur
  is spec me unhe fact ki tarah nahi likha gaya hai.

## Flora-new v1 design (Maya copy nahi — Flora ka apna design)

Trigger = { name, hour, minute, action, enabled } — sirf TIME
trigger v1 me hai. Har trigger roz ek waqt par fire hota hai.

| Action | Fire par kya hota hai |
|---|---|
| `routine` (routine ka naam) | Routine ka naam due-set me jata hai (wahi `flora_job_due` set jo routine jobs use karte hain). App khulte hi maujooda due-rule chalta hai: Auto Mode ON ho to routine khud chalegi, warna Flora sirf batayegi. Trigger khud koi command nahi chalata. |
| `message` (chhota text) | Sirf notification aata hai. Koi action, koi message/call kabhi nahi chalta. |

## Kabhi nahi (design hard rules)

- Trigger se call, SMS, payment, delete, ya koi silent action kabhi
  nahi hoga — trigger sirf routine-due ya notification tak hai.
- Trigger koi naya alarm infra nahi laata: AlarmManager ka wahi
  pattern hai jo JobScheduler/ReminderScheduler use karte hain
  (exact mile to exact, warna inexact fallback; WorkManager nahi).
- Reboot ke baad alarms BootReceiver dobara lagata hai.

## Future (is cycle me NAHI bana — definition + consent design chahiye)

- Call aane par / notification aane par trigger: iske liye alag
  consent design + phone evidence chahiye (kab trigger ho, kis
  app par, kya action allowed hai). Bina us design ke build nahi
  hoga — guess par trigger banana silent-action risk hai.
- Social/marketing alarm receivers (Maya naam): unka content
  vyavhaar bhi UNVERIFIED hai — isliye v1 me unka koi
  equivalent nahi banaya gaya hai.
