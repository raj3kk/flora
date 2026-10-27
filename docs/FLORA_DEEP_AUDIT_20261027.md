# Flora — Deep Audit, 2026-10-27 ~23:15 IST (user order: har cheez check karke batao)

Method: har daava ke peeche live evidence ya file evidence hai. Jahan
evidence nahi hai, wahan "UNVERIFIED / PENDING" likha hai — guess par
koi gate PASS nahi kiya gaya hai. Percentages estimate hain, gate nahi.

## 1. Live evidence is audit me (fresh checks)
- Vercel latest production deployment: HTTP 200, Flora landing live.
- Supabase licence-verify via naya custom.supabase-flora connector:
  HTTP 200, {"plan": "free", "status": "free"} (unknown UUID, expected).
- GitHub Actions run 40539723810 (commit 476e5c5, Flora v0.1): success,
  artifact flora-debug-apk 16,770,600 bytes, expired false.
- Supabase dashboard se (23:02-23:08 IST): licence-verify + usage-count
  dono deployed/active, secrets platform se auto-provided.
- Turso flora DB tables usage_daily + sync_queue: 21:05 IST ko platform
  token se verified (is audit me dobara check nahi hua — watcher ka
  stored credential JWT InvalidToken deta hai, ye watcher-level issue hai).

## 2. Features — 178 mapped, v0.1 me kitne bane?
- Total mapped: 124 (V5) + 22 (Maya Home) + 32 (Wake Word) = 178.
  Mapping file: REBUILD_SPEC_178.md. Koi item bina module ke nahi.
- Flora v0.1 app me ASLI me bane (code + build pass):
  Chat screen, Settings screen, Gemini se jawab (BrainClient),
  system TTS se bolna (VoiceEngine), mic se sunna (RecognizerIntent),
  licence check (LicenceClient -> Edge Function), PIN encrypted store
  (PinVault, KeyStore AES/GCM), automation consent flag + gated app
  launch, free/pro config (600 sec/day free, Pro ₹199/month).
  Ye 178 me se roughly 10-12 features ke barabar hai.
- ABHI NAHI BANE (code me nahi hain): Scan/camera screen, Memories/
  Yaadein, Home tabs poore, wake word (koi model nahi — phrases ka
  decision bhi pending hai), Guardian voice-match, Face Lock, SOS,
  Touch Guard, Market/Marketing/Documents/Website-Coding/Study tools,
  WhatsApp/Instagram/social automation, calls uthake baat, driving mode,
  live wallpaper/orb/3D, sub-agents/skills, Groq/OpenRouter fallback,
  search tools, image/video generation, Maya Home app control.
- Maya Home hardware (22 items): alag track, kuch bhi build nahi hua
  (ESP32 firmware, box, safety test sab pending).

## 3. Frontend
- Bana: Chat + Settings, 2 screens, Material3, Hindi/English mix UI.
- Original app ke 54 registered screens me se 52 abhi nahi bane.
- Web landing: live, public, Flora Free + Pro ₹199/month, UPI note,
  privacy note, support flipify.com@gmail.com. Content check pass
  (title + price mile). Phone par scroll/readability abhi unverified.
- Score: ~25% (landing + 2 screens + specs poore).

## 4. Backend
- Bana aur live: Supabase project Flora (keomyweghrmhqglaxmyv,
  ap-south-1, Free, Healthy); tables users, plans, settings, usage,
  licences (RLS on); plans seeds free + pro ₹199/month; Edge Functions
  licence-verify + usage-count deployed aur live-tested.
- Turso flora (Mumbai): usage_daily + sync_queue (edge metering +
  offline sync queue; Supabase hi authoritative hai).
- Vercel project flora: web root, auto-deploy, site public.
- GitHub repo raj3kk/flora (private): 59 tree entries, CI pass.
- NAHI BANA: admin panel (licence approve / UTR verify UI), payment
  webhook + receipt, FCM server sender, control/update server,
  data export/delete endpoints, backup restore test.
- Score: ~80%.

## 5. Database
- Schema live aur Table Editor se verified (21:05 IST).
- licences.user_id UUID hai, device_id text hai — isi wajah se
  pehla dummy test ("test-user") 500 diya tha; asli app UUID bhejta
  hai, isliye wo rasta sahi hai.
- Pending: data retention 30 din ka auto-expire job (rule likha hai,
  job nahi bana), user data export/delete flow, backup restore test.
- Score: ~75%.

## 6. Security / Privacy
- Bana: server-authoritative licence (app ki baat par licence nahi
  milta), PIN sirf KeyStore me (file/server/backup me kabhi nahi),
  automation consent gate (default OFF, visible stop ka rule),
  screen/camera/call sirf user ke kehne par (rule + UI note),
  repo/code me koi secret nahi, owner personal details kahin public nahi,
  purani keys/domains ka reuse nahi, creator code/UI/asset copy nahi.
- Pending: app ke andar Privacy + Terms screens, data export/delete,
  Guardian voice-match (samples abhi collect nahi hue), Face Lock,
  secret rotation policy ka test, permanent law list Table A ka
  app-level checklist sign-off.
- Score: ~60%.

## 7. Maya bolti hai kya? (sawaal ka seedha jawab)
- Code me: HAAN — VoiceEngine system TTS se jawab bolta hai, aur
  mic button se RecognizerIntent se sunta hai. BrainClient Gemini se
  jawab laata hai (jab user apni Gemini key Settings me daalega).
- Phone par: ABHI UNVERIFIED — v0.1 APK phone par install karke
  test nahi hua hai. Isliye "Flora bolti hai" ka pakka daava phone
  test ke baad hi hoga. Wake word se khud jagna ABHI NAHI hota
  (koi model, koi recording, koi phrase decision nahi hai).

## 8. Maya working hai ya nahi? (seedha jawab)
- Server side: HAAN — licence check end-to-end live pass hai.
- App side: AADHA — v0.1 build pass hai, Chat/Settings/licence/PIN/
  voice ka code hai, par phone test nahi hua aur 178 me se zyadatar
  features abhi code me nahi hain. Poori Maya (original jaisi) abhi
  working NAHI hai — ye kehna jhooth hoga.

## 9. API keys — kaun si, kab, status
- MUST (v0.1 chalne ke liye):
  - Gemini key — har jawab ke liye. User apni key app Settings me
    daalega (free tier, aistudio.google.com). ABHI NAHI DALI GAYI.
  - Supabase publishable key — licence/usage ke liye. Vault me
    SAVE HO GAYI (connector verified, live test pass). App Settings
    me daalna abhi baaki hai (user dashboard se copy karke).
- FREE / BINA KEY: system STT/TTS, DuckDuckGo search, Open-Meteo
  mausam, Yahoo Finance price, Pollinations image (free tier).
- BAAD ME / OPTIONAL: Groq, OpenRouter, Deepgram, ElevenLabs,
  Tavily, Brave, SerpAPI, Google Places, Notion/Todoist/Linear,
  YouTube Data API — sab free tier wale, zaroorat par user ki key se.
- FCM: Firebase project + google-services.json + server key —
  user ke Firebase console se aayega. Code DRAFT hai
  (docs/fcm-pending/), wiring pending hai.
- PAYMENT: koi provider key nahi — abhi UPI deep link + admin
  UTR verify (manual). Full provider baad me, alag approval se.
- RULE (hamesha): key ki value kabhi file/repo/chat me nahi.

## 10. Production-ready gates (FLORA_PRODUCTION_READY_PLAN ke 7)
1. Actions PASS + APK verified — PASS (v0.1, 16.8 MB).
2. Edge Functions deployed + live test — PASS (dono, 200 free/free).
3. Vercel public + content check — PASS (HTTP 200, title + ₹199).
4. Modules + phone test + DB check — FAIL (modules adhoore, phone
   test pending; DB check pass hai).
5. Privacy/Terms/FCM test — FAIL (screens + FCM pending).
6. Backup restore test — FAIL (backup bana hai 22:43 IST, 42,493
   files, original APK sha match; restore test nahi hua).
7. Final backup + SHA — PARTLY (fresh backup hai, final tab hoga
   jab sab kuch complete ho).
- Natija: Flora PRODUCTION-READY NAHI HAI. 7 me se 3 gate pass hain.

## 11. Overall percentage (estimate, gate nahi)
- Research/docs: 100% | Backend: 80% | App features: ~10%
- Build/CI: 95% | Security: 60% | Testing: 20% | Docs/site: 90%
- OVERALL: ~52% (pehle ~50% tha; build pass + functions live hone se
  thoda badha hai, par app features hi sabse bada baaki kaam hai).

## 12. Baaki kaam, order me
1. Phone test: v0.1 APK install, mic + licence + PIN + chat bolna.
2. App modules, MASTER_PLAN order me: core poora, voice (wake
   word decision + recordings), tools, social/calls, security
   (Guardian/Face Lock/SOS), licence+payment+update.
3. FCM wiring (Firebase project user se) + live push test.
4. Admin panel + UPI verify UI + payment webhook/receipt.
5. Privacy/Terms screens + data export/delete + retention job.
6. Maya Home hardware track (alag).
7. Final gates 4-7 + final backup + APK delivery.
