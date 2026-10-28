# Voice-match + Face-match — design (v2.3, one-by-one: FCM ke baad)

## Kya banega (consent ke saath, on-device)
- Voice-match: user apni awaaz ke 3-5 chhote samples dega (consent
  screen par, "ye sirf is phone par rahega" note ke saath) — baad me
  Guardian check ke waqt awaaz milne par hi PIN-store unlock hoga.
  Model: on-device embedding (TFLite, chhota) — abhi model chuna
  nahi gaya hai, ye item shuru hote waqt model ka faisla hoga.
- Face-match: gallery face scan + watchman — photo gallery se chehre
  dhoondhna (on-device ML Kit, koi photo server par nahi) + jab
  koi aur phone chhoo raha ho to watchman alert (Face presence se
  aage, identity match ke saath).
- Touch guard: galat touch pattern par alert — isi item ke saath
  design hoga, implement alag chhota step ho sakta hai.

## Audit points — SAKHT
- Voice/face samples KABHI server/backup/file-share par nahi jayenge
  — sirf encrypted on-device store.
- Enrolment bina saaf consent ke kabhi nahi — default OFF.
- Match fail ho to Flora "pehchana nahi" bolegi, koi guess-unlock nahi.
- Ye item phone test ke bina PASS nahi mana jayega (sirf CI kaafi nahi)
  — kyunki asli awaaz/chehra phone par hi test ho sakta hai.
- Creator ka koi voice/face model ya data copy nahi hoga.
