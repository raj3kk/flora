# v2.1 Keys push plan (local note — v2.0 audit PASS ke baad push hoga)

Gate rule: v2.0 tools ka audit PASS hone ke baad hi v2.1 push hoga.
v2.0 push me Tavily lane nahi gaya hai — wo isi push me poora aayega.

## v2.1 push files (local me sab taiyaar hai)
- app/build.gradle.kts: versionCode 22, versionName 2.1.0-rebuild
  (NOTE: bump push ke waqt karna hai, abhi local 21 hi rakha hai —
  v2.0 ka local tree abhi 21 par hai, isliye pehle bump karne se
  v2.0/v2.1 ke beech confusion hoti)
- app/src/main/java/com/flora/app/data/FloraPreferences.kt (second keys + optional keys)
- app/src/main/java/com/flora/app/assistant/BrainClient.kt (rotation Gemini 1, 2, Groq 1, 2)
- app/src/main/java/com/flora/app/tools/SearchTools.kt (POORA local file — Tavily lane + postJson ke saath)
- app/src/main/java/com/flora/app/ui/FloraApp.kt (Settings optional key fields + save)
- app/src/main/java/com/flora/app/MainActivity.kt (SearchTools(prefs))
- docs/FLORA_KEYS_TEST_ENTRY.md
- docs/FLORA_PER_ITEM_AUDIT_LOG.md (v2.1 section: code audit done, test entry, CI pending)
- docs/FLORA_V21_PUSH_PLAN.md (ye file, push ke saath chali jayegi)

## v2.1 audit notes (code audit already done)
- Rotation order fixed: Gemini 1, Gemini 2, Groq 1, Groq 2; khali key
  skip hoti hai (geminiAsk/groqAsk empty par null lautate hain).
- Key values kabhi log/print nahi hoti — sirf lane try hota hai.
- Tavily lane: key ho to pehle, fail ho to DuckDuckGo fallback.
- Brave/SerpAPI/Maps keys sirf save hoti hain, lane nahi bana hai —
  ye baat Settings UI me saaf likhi hai.
- States launcher se pehle (775 < 793); braces 0 sab files me.
- Test-plan entry: docs/FLORA_KEYS_TEST_ENTRY.md (6 steps).
