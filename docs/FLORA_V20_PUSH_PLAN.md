# v2.0 Tools push plan (local note — themes audit PASS ke baad push hoga)

Gate rule: themes (v1.9) ka audit PASS hone ke baad hi v2.0 push hoga.
v2.1 keys uske baad, alag push me — dono ek saath nahi.

## v2.0 push files
- app/build.gradle.kts: versionCode 21, versionName 2.0.0-rebuild
- app/src/main/java/com/flora/app/tools/SearchTools.kt (study, stockPrice, websiteStarter)
- app/src/main/java/com/flora/app/ui/ExtraScreens.kt (ToolsScreen 3 naye sections)
- docs/FLORA_TOOLS_TEST_ENTRY.md
- docs/FLORA_PER_ITEM_AUDIT_LOG.md (v2.0 section: code audit done, test entry, CI pending)
- docs/FLORA_V20_PUSH_PLAN.md (ye file, push ke saath chali jayegi)

## v2.0 push me NAHI jayega (v2.1 ke liye alag rakha hai)
- FloraPreferences keys (GEMINI_API_KEY2, GROQ_API_KEY2, TAVILY, BRAVE, SERPAPI, MAPS)
- BrainClient rotation
- SearchTools Tavily lane + postJson
- MainActivity SearchTools(prefs)
- Settings optional key fields
- docs/FLORA_KEYS_TEST_ENTRY.md

## v2.0 audit log entry (push ke baad CI result se poori hogi)
- Code audit: study (DuckDuckGo, 5 sources + Source note), stockPrice
  (Yahoo Finance + stale-price note), websiteStarter (sirf local text,
  file save nahi); braces 0; koi key use nahi.
- Test-plan entry: docs/FLORA_TOOLS_TEST_ENTRY.md (6 steps).
- Build test: CI result push ke baad.
