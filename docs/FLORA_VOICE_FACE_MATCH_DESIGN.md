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

## Voice-match validation update (2026-10-28 ~13:05 IST)
- Frontend params ab Lingvo/sidlingvo source se pakke hain
  (hop 10ms, frame 512, per-frame preemph, periodic Hann,
  rfft 1024, magnitude, log(max(1.0, mel)), int16 scale,
  stack repeat-first x3 + stride 3, VAD exp(out[0,0]) > 0.1,
  encoder chunk 2, d-vector = last row).
- Asli conformer encoder par end-to-end test PASS: 6
  same-speaker clips cosine mean 0.671 (0.558-0.770),
  white noise ~0.33, silence VAD-reject, clean-vs-noise
  stability 0.877. Threshold ~0.45-0.50 sirf starting guess
  hai — final threshold doosre speaker ke data + phone test
  ke baad lock hoga.
- Kotlin port me HTK mel formula mat use karo (TF matrix se
  max diff 1.0 hai) — exact TF mel matrix (513x128) asset
  ke roop me ship karo.
- Ye item abhi bhi phone test ke bina PASS nahi mana jayega.
