# Flora v2.2 push plan (FCM wiring)

Base: v2.1 head 659c83934e9f68cdf4a64aa9ad9a3b1231584354.
Version: versionCode 23 / 2.2.0-rebuild.

Files (10):
- build.gradle.kts (google-services plugin 4.4.2, apply false)
- app/build.gradle.kts (plugin + firebase-messaging 24.0.0 + version)
- app/google-services.json (NAYA file: project flora-f22a7, package
  com.flora.app — user ki bheji hui google-services__4-2.json ki copy;
  PURANA flow99-f575e file kahin use nahi hui)
- app/src/main/java/com/flora/app/data/FloraPreferences.kt (FCM_TOKEN)
- app/src/main/java/com/flora/app/push/FloraMessagingService.kt (naya)
- app/src/main/AndroidManifest.xml (FCM service entry; InCallService
  block push se pehle wapas verify — permission line barkarar)
- app/src/main/java/com/flora/app/ui/FloraApp.kt (token fetch +
  Settings FCM status line; token value kabhi display nahi)
- docs/FLORA_FCM_TEST_ENTRY.md (naya)
- docs/FLORA_V22_PUSH_PLAN.md (ye file)
- docs/FLORA_VOICE_FACE_MATCH_DESIGN.md (v2.2 → v2.3, kyunki FCM
  ne v2.2 le liya hai)

Admin SDK file (flora-f22a7 service_account) REPO ME NAHI JAYEGI —
sirf user ke files me rahegi; server secret route alag step me.

Push ke baad: CI run verify (completed/success + artifact size),
tabhi per-item audit PASS.
