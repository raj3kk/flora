# Flora continuous audit — 2026-10-28 (user order: kuch miss na ho)

Ye audit har baar chalega — jo check hua, jo fix hua, jo
khula hai, teeno likhe hain. Koi cheez "theek hai" sirf
isliye nahi likhi gayi ki wo pehle theek thi.

## Aaj check hua — PASS
- app/ me forbidden strings: flow99 / Com.flora / TODO /
  FIXME / "coming soon" — 0 mile.
- app/google-services.json: project flora-f22a7, package
  com.flora.app — sahi (purana flow99 file kahin nahi).
- Nayi local files me secret pattern: private_key /
  service_account / sbp_ / sk- / hardcoded api key — 0.
- Brace/paren balance: BiometricTemplates.kt 0/0,
  FaceMatcher.kt 0/0 (fix ke baad phir check hua).
- Wake dataset: positive_wav 6, negative_wav 6 — plan ke
  mutabiq.
- Supabase functions: service_role sirf env se aata hai,
  repo me koi key value nahi hai.
- FCM draft: token kabhi log nahi hota (comment + code).

## Aaj mile bug — FIX ho gaya (local, push baaki)
1. FaceMatcher ByteBuffer rewind (2 jagah): model buffer
   aur input buffer put() ke baad end-position par the —
   rewind ke bina Interpreter/input khaali padhta. Dono
   jagah rewind lagaya gaya hai.
2. FCM send-push draft arbitrary content: caller koi bhi
   title/body bhej sakta tha. Ab sirf 2 fixed templates
   (trial_ending, pro_expiry) chalenge, baaki 400. Body
   template se aayegi, caller ke text se nahi.

## Khule hue gaps — chhupaye nahi gaye
- Send-push auth deploy blocker: caller auth check + token
  ko push_tokens table se uthana abhi baaki hai (admin
  Phase A auth ke saath solve hoga). Is haalat me deploy
  NAHI karna.
- Licence 1-phone / 15-day change rule licence-verify me
  nahi hai (full audit se pehle se khula hai).
- usage-count: live row par test nahi hua, caller nahi hai.
- Wake prototype: asli speech negatives se test nahi hua.
- Admin panel: design hai, code nahi likha gaya hai.
- Browser task (publishable key + push_tokens + Firebase
  secret) abhi running hai — result aana baaki hai.
- Phone test, backup restore test, final backup — user ke
  phone / baad ke kadam par pending hain.
- Model output 192-d repo docs se liya gaya hai — runtime
  par embedding size check se verify hona baaki hai (code
  me failure par null return hota hai, crash nahi hoga).

## Local-only vs repo divergence (agle push me jayega)
- app/src/main/assets/MobileFaceNet.tflite (5,233,396 B,
  sha verified)
- app/src/main/java/com/flora/app/core/FaceMatcher.kt
- app/src/main/java/com/flora/app/security/BiometricTemplates.kt
- docs/FLORA_V23_IMPLEMENTATION_PREP.md
- docs/FLORA_USAGE_COUNT_WIRING_PREP.md
- docs/FLORA_ADMIN_PANEL_DESIGN.md
- docs/FLORA_V221_TEST_ENTRY.md
- docs/fcm-pending/push_tokens.sql
- docs/FLORA_AUDIT_20261028_CONTINUOUS.md (ye file)
- docs/fcm-pending/send-push-draft.ts (template fix ke saath)
Inme se koi bhi file abhi repo me nahi hai — agla push
inhi ko lekar jayega, usse pehle phir audit hoga.
