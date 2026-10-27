# Flora confirmed-automation audit (v0.7 → v0.8, 2026-10-28)

Rule (user order 2026-10-28): ek baar bolo + ek confirmation, phir poora
execution, bolte hue commentary, beech me ruk kar dobara poochna nahi.
Draft/manual step jahan tha, audit karke upgrade kiya gaya hai — jahan
upgrade possible nahi hai, wajah ke saath neeche likha hai (jhootha
daava nahi).

## Upgraded in v0.8
- Scan: sirf preview tha → ab "Photo lo" = capture + Pictures/Flora me
  save + spoken confirmation. Button dabana hi ek confirmation hai.
- Chat voice navigation: "photo lo" / "scan karo" / "camera kholo" →
  Scan screen; "sos" / "sos bhejo" / "madad" → SOS screen;
  "yaadein kholo" → Yaadein screen. Har command par spoken commentary.
- Call/SMS (v0.7): direct, ek confirmation dialog ke baad. SOS Auto:
  ek confirmation me call + SMS dono.

## Already compliant (ek press = poora kaam, koi beech ka step nahi)
- Chat brain reply (TTS ke saath), Memories add/edit/delete,
  Documents add/edit/delete, Tools (search/weather/currency/dictionary),
  Licence check, Keys save, PIN set/verify, Guardian/Face Lock toggles.

## NOT upgraded — wajah ke saath (audit honestly)
- Share → auto-post: social apps kisi app ko apni taraf se silent post
  allow nahi karti (unka login/SDK chahiye). Share sheet + user ka Post
  press hi rahega.
- Face identity match: Face screen sirf presence check karti hai.
  Asli pehchaan match ke liye on-device embedding model + user ki
  consent enrolment chahiye — alag step, is batch me daava nahi.
- "open <kisi bhi app>" voice command: Android QUERY_ALL_PACKAGES
  restricted hai aur naam se package guess karna galat app khol sakta
  hai. Isliye sirf Flora ki apni screens par navigation di gayi hai.
- Incoming-call announcement / auto-reply: phone-state permission +
  foreground service + call screening chahiye — alag bada step, is
  batch me daava nahi.
- Wake word (khud jagna): model + dataset + phrase decision user se
  pending hai — is batch me daava nahi.
