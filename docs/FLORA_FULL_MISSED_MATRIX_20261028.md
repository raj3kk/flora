# Full missed matrix — video 124 + screenshots + shorts/website vs Flora (2026-10-28)

Source: video RE_FEATURES.md (124), SCREENSHOT_AUDIT.md (11 screenshots),
SHORTS_AND_WEBSITE_RESEARCH.md, REBUILD_SPEC_178.md. Status: DONE /
PARTIAL / MISSING (plan me add, banega) / EXCLUDED (decision, miss nahi).

## Video sections
- Setup (#1–10): DONE (keys Settings me, permissions wizard jaisa
  manual flow, battery note) — PARTIAL: default assistant, start on
  boot, Play Protect note (sideload guide phone test ke saath).
- Core chat/Home (#11–20): DONE chat, memories, voice+text —
  PARTIAL: scan vision v1.1 me, incognito/auto-memory (Flora chat
  persist nahi karta, isliye N/A jaisa) — MISSING: wake word (#18),
  start on boot (#19), proactive mode (#20, creator khud band
  rakhne ko kehta hai — Flora me default OFF hi rahega).
- Persona/Voice/Language (#21–32): PARTIAL system TTS hi-IN/EN,
  Gemini/Groq brain — MISSING: Flora ke apne personas (creator ke
  naam kabhi nahi), per-persona voice, multi-language list, favourite
  song (v1.1 me "gaana bajao" command DONE), STT choice Deepgram.
- Modules (#33–40): PARTIAL Docs, weather/currency/dictionary —
  MISSING: Maya Home UI, Market/Trading, Marketing, Website/Coding
  agent, Hosting, Study/Whiteboard.
- Communication (#41–52): PARTIAL call/SMS confirm ke saath,
  share sheet — MISSING: WhatsApp auto-reply/alert/group report
  (Accessibility se WhatsApp chalana ban-risk, isliye sirf intents),
  email read/reply, social auto-post/daily story (EXCLUDED silent
  post), Instagram auto-scroll (EXCLUDED), post history.
- Appearance (#53–64): MISSING themes/orb/3D/live wallpaper/
  floating charm/edge glow/earpiece/echo guard/media mode/typing
  mode — look-feel, functionality ke baad (v1.3).
- Call/Driving/Events (#65–68): v1.1 me call announce (number,
  toggle) DONE — MISSING: bol kar receive/reject (CallScreening
  role, v1.2), driving mode, event triggers (charger/WiFi/battery).
- Skills/Sub-agents/Multi-key (#69–72): MISSING skills install,
  sub-agents (routines v1.1 iska pehla kadam hain), multi-key rotation.
- Security (#73–95): PARTIAL PIN gate, SOS (3 contacts, manual
  confirm + auto SOS), Face presence-only — MISSING: voice-match
  (samples+model), face enrolment/match, gallery face scan, morning
  hello, watchman, failed-attempts log, touch guard (+options,
  stealth, charger trigger) — EXCLUDED: hard God-Mode lock, screen
  lock voice unlock (DISABLE_KEYGUARD), romantic/18+ modes.
- Battery/Optional (#96–99): PARTIAL DuckDuckGo/Open-Meteo no-key,
  Pollinations note — MISSING: Maps Places key field, battery saver
  mode, image generation key field.
- Agentic (#100–109): PARTIAL Gemini→Groq fallback, retry nahi —
  MISSING: orchestrator router, provider list refresh, thinking
  toggle, image-support toggle (vision opt-in ke saath aayega),
  STT/TTS choice screen.
- Live demos (#110–119): WhatsApp demo = share/intent level DONE;
  music = gaana command DONE; camera demo = vision v1.1 DONE
  (text-read); Instagram demo = app kholo + click level PARTIAL;
  Termux demo = EXCLUDED (Hermes topic dropped, user order).
- Monetization (#120–124): Flora rule alag hai — ₹199/month
  FINAL (video ke ₹699/798/948 superseded), UPI+UTR flow PARTIAL
  (server webhook baaki).

## Screenshots (11) se extra
- Home widgets (Gaane/Padhai/Journal/Mausam/Mood/Energy) — PARTIAL
  (Tools me mausam, gaana command); journal/mood MISSING (v1.2).
- Chat attachment (image/PDF/.md) — MISSING (v1.2).
- Memories builtin tasks (gemini/chatgpt poochho, video/image
  prompts, 8–17 steps) — PARTIAL (routines v1.1 iska Flora version
  hai; builtin task list MISSING).
- Drawer menu (Maya Home, Markets, Marketing, Documents, Photos,
  Website/Coding, Browser, Accessibility, PC⇄Phone, Rules) —
  PARTIAL (screens hain, drawer UI MISSING); PC⇄Phone MISSING.
- Key setup flow (free key steps, paste, encrypted note) — DONE
  (Settings keys + guide jaisa text).
- Optional keys (Maps, Tavily/Brave/SerpAPI) — MISSING fields
  (v1.2, sirf user ki keys, Secure Vault style on-device).

## Shorts + website se
- 21 shorts ke features video list me hi cover hain; website prices
  Flora ₹199 se superseded (record only). Website login server =
  Supabase (Flora ka apna) DONE; licence 1-phone/15-day change =
  Flora licences table PARTIAL (server check DONE, change-limit
  rule MISSING — v1.2).

## v1.1 me is push me kya aa raha hai
Vision text-read (Scan), routines auto-run, call announce toggle,
gaana bajao command, Auto screen + Accessibility foundation (v1.0),
heap fix 4096m. Baaki MISSING items v1.2/v1.3 me ek-ek karke —
koi item plan se hata nahi hai.
