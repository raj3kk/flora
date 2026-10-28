# Flora v2.2.1 push plan (audit stale-fix, koi naya feature nahi)

Base: v2.2 head 7a4b5f1a445eb9f1ad63054bba4c100ca425ee40.
Version: versionCode 24 / 2.2.1-rebuild.

Files:
- app/src/main/java/com/flora/app/core/FloraConfig.kt (wake flag:
  phrase "Hey Flora" decided, model/training pending)
- app/src/main/java/com/flora/app/ui/LegalScreens.kt (Terms: FCM
  line stale "jab available ho" → client taiyaar / server secret baaki)
- docs/FLORA_PER_ITEM_AUDIT_LOG.md (v2.1 PASS + v2.2 FCM PASS entries;
  v2.2 Voice/Face label → v2.3 note)
- docs/FLORA_FULL_AUDIT_20261028_V2.md (full audit, is push se repo me)
- app/build.gradle.kts (version bump)
- docs/FLORA_V221_PUSH_PLAN.md (ye file)

Commit message me [skip ci] NAHI hona chahiye (AGENTS.md lesson).
Push ke baad: CI run verify (completed/success + artifact size),
tabhi per-item audit PASS.
