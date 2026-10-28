# Flora FULL AUDIT — 2026-10-28 ~09:10 IST (user ke sawaal par)

Audit method: repo code grep + file list + GitHub API + live checks
(Vercel 200, Supabase licence-verify 200 free/free). Koi guess nahi —
har gap ke saath evidence hai. Ye doc LOCAL hai; agle code push ke
saath repo me jayega.

## A. Audit ke dauraan hi PASS hua — v2.2 FCM
- Run 40627580432 completed success, job build success, artifact
  flora-debug-apk 54,540,574 bytes, expired: false.
- Per-item audit PASS: pre-push code audit (Settings states 808 <
  launcher 810, scope 132 < token fetch 171, braces/parens balance,
  manifest XML OK, InCallService permission barkarar) + CI success +
  test entry docs/FLORA_FCM_TEST_ENTRY.md.
- Latest PASS APK ab v2.2 hai (v2.1: 54,350,099 bytes).

## B. BROKEN / MISMATCH (evidence ke saath)
1. FloraConfig.WAKE_WORD_DECISION_PENDING = true + comment "Wake
   phrases are NOT decided yet" — STALE. User ne 2026-10-28 ~08:25
   IST "Hey Flora" decide kar diya hai. Flag/comment update baaki
   hai (model abhi bhi nahi bana — ye alag baat hai).
2. Repo audit log (docs/FLORA_PER_ITEM_AUDIT_LOG.md) STALE: v2.1
   entry abhi bhi "CI result pending" kehti hai jabki v2.1 PASS ho
   chuka hai (sirf FLORA_DECISIONS.md me darj hai); line 84 me
   "v2.2 Voice/Face match" likha hai jabki v2.2 ab FCM hai aur
   voice/face v2.3 ho gaya hai — doc version confusion.
3. Terms screen: "FCM push (jab available ho)" — v2.2 PASS ke baad
   ye line stale ho gayi hai (FCM client ab available hai; server
   send abhi bhi baaki hai — wording alag karni hogi).
4. Code me koi flow99 / "Com.flora" / TODO / FIXME / "coming soon"
   NAHI mila (case-sensitive grep, app/ poora) — is mamle me clean.
5. Repo ka app/google-services.json = flora-f22a7 / com.flora.app
   (sahi); purana flow99-f575e app/ me kahin nahi hai.
6. Compile-broken kuch nahi hai (CI v2.1 + v2.2 green). RUNTIME
   broken ka koi saboot nahi hai kyunki phone test aaj tak hua hi
   nahi — runtime claim karna galat hoga.

## C. HALF-IMPLEMENTED (aadha bana, aadha baaki)
1. usage-count: Edge Function + LicenceClient.countUsage() maujood
   hai, lekin app code me countUsage() ko KOI caller nahi hai —
   server usage table me app se kabhi row nahi jayegi. Talk limit
   sirf local DataStore se enforce hota hai (3 jagah: chat,
   attachment, routine) — app data clear karne par limit reset ho
   jayega. usage-count function khud bhi live row par kabhi test
   nahi hua.
2. Licence gating PARTIAL: app-start refresh + talk limit local
   hai; per-action server gating nahi hai; ToolsScreen ki koi plan
   gating nahi hai (koi "licence?.plan" check ExtraScreens me nahi).
3. Licence 1-phone/15-day change rule: licence-verify sirf
   (user_id, device_id) match karke plan/status deta hai — device
   change ki limit ka koi rule function me nahi hai.
4. FCM: client poora (v2.2 PASS) — server send + push_tokens
   table BA AKI hai; send-push draft sirf local hai, deploy nahi;
   Supabase secret set karne ka rasta (Management API dead) suljha
   nahi hai.
5. Tools: study/stock/website-starter DONE — website starter sirf
   text deta hai, file save nahi karta (UI me imaandaari se likha
   hai). Maps key field save-only hai, koi Maps lane nahi.
   Brave/SerpAPI keys save-only hain, lane nahi (UI me likha hai).
6. Pro scope waade vs app: live wallpaper, conversation mode abhi
   nahi bane — Terms me "(jab available ho)" likh kar imaandaari
   rakhi gayi hai, lekin scope poora nahi hai.
7. Data retention 30 din + free "15 tools" limit: constants
   FloraConfig me hain, enforce karne wala code kahin nahi hai
   (koi expiry, koi tool-count gate nahi).
8. Call-talk: InCallService + dialer-role button + toggle DONE —
   phone par default-dialer set + asli call test BA AKI hai, isliye
   remote party ko awaaz sunai degi ya nahi, ye UNPROVEN hai.
9. Backup: aakhri full backup 2026-10-27 22:43 IST ka hai (v2.2,
   FCM, recordings se pehle ka) — STALE; restore test kabhi nahi hua.

## D. POORA MISS (abhi bana hi nahi)
1. Wake word "Hey Flora": phrase decided, 6/50 recordings mili
   hain — training shuru nahi hui, app me koi WakeWordService /
   model nahi hai.
2. Voice-match + face-match (enrolment/match): design + test entry
   + model decision note hai — code ZERO hai.
3. Gallery face scan, morning welcome/hello, watchman,
   failed-attempts log, touch guard: kuch nahi bana.
4. Maya Home app-side UI (pairing/control screen): kuch nahi bana
   (hardware/mains kaam waise bhi electrician ka hi rahega).
5. Live camera Q&A (bolte-bolte camera video samajhna): LiveScreen
   MediaProjection SCREEN ka hai, camera ka nahi — ye item miss hai.
6. Time/jagah par routine trigger (scheduler): sirf power/battery
   event announcement hai; WorkManager/AlarmManager code nahi hai.
7. Start on boot, default assistant role: code nahi hai.
8. WhatsApp auto-reply/alert/group report, email read/reply, post
   history: nahi bane (silent post/auto-scroll EXCLUDED hai — ye
   miss nahi, decision hai; neeche section F dekho).
9. Per-persona voice, multi-language list, STT choice (Deepgram),
   TTS choice (ElevenLabs): system TTS (hi-IN/EN) hi hai.
10. Live wallpaper, floating charm, edge glow, earpiece/echo guard,
    media mode, typing mode, 3D: themes/orb DONE hai, ye baaki hain.
11. Sub-agents/skills install, orchestrator router, provider list
    refresh, thinking toggle, image-support on/off toggle: nahi bane.
12. Market/Trading poora, Marketing tools poora, Hosting,
    Whiteboard, drawer menu UI, builtin task list, home widgets,
    PC⇄Phone, battery saver mode, image generation (Pollinations):
    code me Pollinations ka naam tak nahi hai.
13. Admin panel + payment webhook: design DONE hai — build ke liye
    user ki alag money approval chahiye (abhi tak nahi mili).

## E. USER KE HISSE KA KAAM (sirf tum kar sakte ho)
1. Hey Flora ki 44 aur recordings (total 50 ke liye).
2. Phone test: latest PASS APK (ab v2.2) install + poora test plan
   + fail report (FLORA_PHONE_TEST_PLAN_v14 + har item ki test entry).
3. Brain ke liye Gemini/Groq keys app Settings me daalna — bina
   keys ke chat brain jawab nahi dega (ye phone test ka hissa hai).
4. Supabase anon key Settings me daalna — bina iske licence check
   sirf local free mode me chalega.
5. Firebase console se test push bhejna (v2.2 install ke baad) —
   app band ho tab bhi, khuli ho tab bhi notification check.
6. Default-dialer set karna (sirf agar call-talk chahiye) + call-talk
   toggle ka faisla.
7. Supabase dashboard me FIREBASE_SERVICE_ACCOUNT_JSON secret set
   karna (server push ke liye) — Management API dead hai, isliye ye
   dashboard se tumhe hi karna hoga; assistant code de chuka hai.
8. Pro payments ke liye apni UPI VPA Settings me daalna.
9. Admin + payment webhook ke liye alag money approval dena.
10. Custom domain jab khareedna ho tab batana.

## F. JO THEEK HAI (verified)
- 16 screens: CHAT, SETTINGS, MEMORIES, SCAN, TOOLS, SOS, FACE,
  DOCS, CONNECT, PRIVACY, TERMS, AUTO, ROUTINES, LIVE, DRIVING,
  JOURNAL — sab CI-pass builds me maujood.
- Chat + personas (tone) + themes/orb + journal/mood + attachments
  (image vision, text/.md, 5 MB cap, PDF honestly unsupported) +
  confirmed automation (call/SMS, ek confirmation) + SOS (max 3) +
  driving mode + continuous live screen (10 frame cap) + event
  triggers (power/battery) + scan OCR (Latin + Devanagari) +
  routines + keys rotation (Gemini/Groq x2, Tavily lane, DuckDuckGo
  fallback) — sab per-item audit PASS (v2.2 tak).
- Backend live: Vercel site 200; licence-verify 200 free/free.
- Privacy lines barkarar: screen server par nahi, token kabhi log
  nahi, PIN KeyStore-only default OFF, Guardian default DENY,
  payment auto-confirm kabhi nahi.

## G. JO MISS NAHI HAI (jaan-boojh kar excluded — decision)
Creator copy, silent screen dekhna/record karna, silent posting,
silent call recording, hard God-Mode lock, DISABLE_KEYGUARD unlock,
18+/romantic persona, payment auto-confirm, screen-to-server,
purani keys/backend/signing key reuse — ye kabhi nahi banenge.
