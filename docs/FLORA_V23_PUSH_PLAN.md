# Flora v2.3 push plan (face-match, photo enrolment)

Base: v2.2.1 head a7686dae.
Version: versionCode 25 / 2.3.0-rebuild.

SCOPE CHANGE (saaf likha hai): v2.3 me sirf FACE-match
hai. Voice-match v2.4 me jayega, kyunki uska mel-frontend
prototype abhi baaki hai aur usko face ke saath zabardasti
jodna audit kharab karta. Voice design + test entry pehle
se maujood hain, koi kaam delete nahi hua hai.

Files:
- app/build.gradle.kts (version bump)
- app/src/main/java/com/flora/app/core/FloraConfig.kt
  (DEFAULT_SUPABASE_ANON_KEY — publishable key, public by
  design; service_role kabhi nahi)
- app/src/main/java/com/flora/app/data/FloraPreferences.kt
  (supabaseAnonKey: Settings key pehle, warna default)
- app/src/main/java/com/flora/app/core/FaceMatcher.kt (naya)
- app/src/main/java/com/flora/app/security/BiometricTemplates.kt (naya)
- app/src/main/java/com/flora/app/ui/FaceScreen.kt
  (enrol/match/delete UI + preview weight fix taaki buttons
  dikhen; states launcher se pehle, braces 0)
- app/src/main/assets/MobileFaceNet.tflite (naya, 5,233,396 B,
  git blob sha verified 3249c511be86...)
- docs/FLORA_FACE_TEST_ENTRY.md (naya, 7 steps)
- docs/FLORA_V23_PUSH_PLAN.md (ye file)
- docs/FLORA_PER_ITEM_AUDIT_LOG.md (v2.2.1 PASS + v2.3 entry)
- docs/FLORA_AUDIT_20261028_CONTINUOUS.md (audit fixes)
- docs/FLORA_V23_IMPLEMENTATION_PREP.md (pehle se local tha)
- docs/FLORA_USAGE_COUNT_WIRING_PREP.md (pehle se local tha)
- docs/FLORA_ADMIN_PANEL_DESIGN.md (pehle se local tha)
- docs/FLORA_V221_TEST_ENTRY.md (pehle se local tha)
- docs/fcm-pending/push_tokens.sql (pehle se local tha)
- docs/fcm-pending/send-push-draft.ts (template fix)

Admin SDK file repo me NAHI jayegi. Koi key value (service
key, private key) repo me nahi hai — sirf publishable key
hai, jo public by design hai.

Push ke baad: CI run verify (completed/success + artifact
size), tabhi per-item audit PASS. Phone test ke bina audit
poora nahi mana jayega (test entry upar hai).
