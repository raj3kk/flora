# Flora v2.4 push plan — Wake Word v0

Base: v2.3 commit a38c69ee (CI PASS, run 40629941547).
Version: versionCode 26 / 2.4.0-rebuild.

## Files
- app/build.gradle.kts (version bump)
- app/src/main/AndroidManifest.xml (WakeWordService entry,
  foregroundServiceType microphone, exported false)
- app/src/main/java/com/flora/app/wakeword/WakeWordMFCC.kt (new;
  Python prototype ka port, logic audit 2026-10-28)
- app/src/main/java/com/flora/app/wakeword/WakeWordRecorder.kt
  (new; 16kHz mono AudioRecord, no sample saved)
- app/src/main/java/com/flora/app/wakeword/WakeWordService.kt
  (new; foreground service, TTS ack, 10s cooldown, own scope)
- app/src/main/java/com/flora/app/security/BiometricTemplates.kt
  (wake template methods add; clearAll me wake file bhi)
- app/src/main/java/com/flora/app/data/FloraPreferences.kt
  (WAKE_ENABLED key + getter/setter, default OFF)
- app/src/main/java/com/flora/app/core/FloraConfig.kt (stale
  6/50 comment sudhara — ab 6-clip v0 likha hai)
- app/src/main/java/com/flora/app/ui/FloraApp.kt (Settings:
  wake states launcher se pehle, mic launcher, switch, enrol)
- docs/FLORA_WAKE_TEST_ENTRY.md (new, 8 steps)
- docs/FLORA_V24_PUSH_PLAN.md (ye file)
- docs/FLORA_PER_ITEM_AUDIT_LOG.md (v2.3 PASS entry pehle se
  local me hai — is push me bhi jayegi)

## Gate (push se pehle check hue)
- Brace/paren/bracket balance sab files me 0/0/0.
- Declaration order: wake states line 818-820 < mic launcher
  < Switch use (line 886). FaceScreen wala order trap dobara
  nahi.
- Forbidden strings app me 0 (flow99 / Com.flora / TODO /
  FIXME / coming soon).
- RECORD_AUDIO + FOREGROUND_SERVICE_MICROPHONE pehle se
  manifest me hain (v2.4 se pehle se).
- Admin SDK file repo me kabhi nahi; koi key value code me
  nahi (publishable anon key v2.3 se repo me hai, design ke
  mutabiq public hai).

## Baad me (is push me NAHI)
- Neural wake model (PyTorch/ONNX/TFLite) — jab framework +
  diverse data ho tab.
- Voice-match v2.4 me nahi aaya — ye push sirf wake word hai.
  Voice-match (mel-frontend prototype) alag item rahega.
- Phone test: FLORA_WAKE_TEST_ENTRY.md ke 8 steps, user ke
  phone par. CI PASS = build PASS, runtime PASS nahi.
