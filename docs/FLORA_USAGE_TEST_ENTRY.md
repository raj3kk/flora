# Flora v2.6 — Usage-count server wiring — phone test entry

Server side (pehle se live + verified):
- usage-count Edge Function fixed (seconds alias, counted + detail,
  user_not_found mapping). push-token-sync se users row banne ke
  baad us user ka count save hoga.

App side (v2.6):
- MainActivity onSpoken: local addTalkSeconds ke SAATH server
  countUsage bhi (ek hi call site — FloraApp ke check sites se
  kabhi nahi, warna triple-count).
- LicenceClient.lastPlan gate: plan pro ho to server call skip
  ("pro unlimited"); verify() har baar lastPlan update karta hai.
- Server fail ho to baat-cheet nahi rukti; local counter hi sach hai.

Phone test steps:
1. v2.6 APK install karo, free mode me 1-2 minute baat karo.
2. Dashboard SQL se check karo (owner karega): usage table me
   aaj ki row me talk_seconds badha ya nahi.
3. Pro licence (jab mile) par dobara test: server count nahi
   badhna chahiye (local counter phir bhi badhega).

Known limits:
- CI PASS sirf build ka saboot hai, server row ka phone +
  dashboard check hi asli saboot hai.
- Jin users ne kabhi token sync nahi kiya (users row nahi),
  unka server count user_not_found dega — local counter tab
  bhi sahi rahega.
