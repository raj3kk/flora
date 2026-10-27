# Flora — Production-Ready Plan (FULL, deep audit ke saath)
Date: 2026-10-27 ~21:25 IST. Status: **PRODUCTION-READY NAHI HAI ABHI** —
ye document batata hai kya done hai, kya baaki hai, aur release se pehle
har gate kaise pass hoga. Koi gate guess par pass nahi hoga.

## 0. Pehchan (verified, guess nahi)
- App: **Flora** (naya naam, naya app, sideload APK — Play Store par nahi).
- Repo: **raj3kk/flora** (private), 24 blob files (21:25 IST check).
- Website project: Vercel **flora** (rootDirectory=web), deployment READY,
  par **public access abhi protected hai** (Vercel login page dikhta hai) —
  deployment protection band karna baaki hai, tabhi site public hogi.
- Supabase: **Flora**, ref keomyweghrmhqglaxmyv, ap-south-1, Free, Healthy,
  account rajraj3kk@gmail.com's Org (limit wala FormMitra account NAHI).
- Supabase tables verified (SQL Editor + Table Editor): users, plans,
  settings, usage, licences. Seeds: free (Flora Free), pro (Flora Pro ₹199/month).
- Turso: DB flora (Mumbai), tables verified: usage_daily, sync_queue.
  Role: edge/fast usage metering + offline sync queue — main DB Supabase hi hai.
- Licence price FINAL: **₹199/month**. Payment pehle **UPI deep link**,
  full integration baad me (provider + alag approval tab).
- Support: flipify.com@gmail.com. Push: FCM. Retention: **30 din**.
- Icon: Flora app icon drawable/ me hai (res-root webp hataya gaya —
  wo aapt error ka ek pakka candidate tha, dekhiye §7).

## 1. Product scope (178 video features + 21 shorts + website, sab mapped)
- App core: setup wizard (user khud har permission ON karega), Home/Scan/
  Mic/Yaadein/Chat tabs + drawer, bottom nav, free mode 10 min/day ~15 tools.
- Voice: wake word sirf Hey Maya / Hello Maya / Wake Up Maya (Flora naam
  par wake phrases user se confirm karni hain — abhi original 3 hi recorded
  hain, Flora phrase ka dataset abhi nahi hai); STT Google/Deepgram,
  TTS system/Edge/gTTS lane; brain Gemini → Groq → OpenRouter → backup.
- Tools: phone control (Accessibility, consent + visible stop), notifications
  + auto-reply (consent), calls answer + talk, WhatsApp/Business read/reply
  (consent route), music (YouTube/Spotify/Gaana app route), alarms/reminders,
  knowledge/live info, maps, smart home (Maya Home alag hardware track),
  memory (Room + Supabase, 30 din), macros (5 builtin, step format tool+args),
  documents, website/coding + deploy (GitHub access + alag approval),
  study/whiteboard, markets (numbers unverified — kabhi guarantee nahi).
- Security: Voice Guardian (Boss, ≤10 samples, threshold 0.86), Face Lock,
  SOS (last 5 contacts), Touch Guard, PIN unlock (encrypted on-device secure
  store ONLY, kabhi file/server/backup nahi, default OFF, voice+Guardian check).
- Personalities: caring/friendly + professional + Venom-style — **18+/romantic
  content BAHAR hai, hamesha.**
- EXCLUDED (kabhi wapas nahi, jab tak user khud na badle): creator code/UI/
  assets/character/trademark copy; purani keys/signing key/backend data reuse;
  hard God Mode; silent screen viewing; silent call recording; passwords store
  karna; DISABLE_KEYGUARD unlock flow; WhatsApp/Instagram silent auto-post;
  Hermes/Rakazo topic; Play Store update purani app par (nayi signing key = nayi app).

## 2. Architecture
- Android (Kotlin + Compose, compile/target 34, min 26, AGP 8.5.2, Kotlin 1.9.24)
  → Supabase (Postgres + Auth + Storage + Edge Functions: licence-verify,
  usage-count — code repo me hai, deploy baaki) + Turso (usage/sync) +
  Vercel (website + admin/control + API routes) + FCM (push).
- Har protected action par server-authoritative licence check (app ke word
  par bharosa nahi). Free cap (600 sec/day) server-side enforce hoga.
- Long jobs background me chalenge, conversation parallel chalegi, completion
  ki khabar FCM + in-app dono se milegi. Failure hamesha plain reason ke
  saath bataya jayega, guess nahi.

## 3. Database (done + baaki)
- DONE: 5 Supabase tables + RLS + seeds (verified).
- BAAKI: Edge Functions deploy + test (licence-verify, usage-count);
  Turso token server env me (Secure Vault / server env, kabhi repo me nahi);
  backup/restore TEST (backup lena hi kaafi nahi); data delete + export
  (user confirmation ke saath); 30-din expiry job verify.

## 4. Licence + payment (baaki code, flow locked)
- UPI deep link flow (docs/upi-activation.md): link server se banega,
  VPA admin setting se aayega (repo me kabhi hardcode nahi), user UTR dega,
  admin UTR+amount+note match karke approve karega → licences active.
- Device bind: 1 phone, change max 1 / 15 din. Revoke/ban server se.
- Full payment integration: provider abhi undecided, us waqt alag approval
  + webhook verification + receipt tabhi add honge.

## 5. Security + privacy (gates, sab mandatory)
- Permissions: mic hi mandatory; baaki sab user khud ON karega, clear
  consent + visible stop. Accessibility kabhi silently ON nahi hogi;
  ADB option sirf khuli screen par, user ki choice par.
- Screen/camera: sirf user ke kehne par, usi waqt, on-device first,
  visible indicator, recording/transcript sirf user ke kehne + disclosure par.
- Voiceprints/face data: on-device, encrypted; server par kabhi raw nahi.
- Secrets: sirf Secure Vault / server env. Koi key repo/file/chat me nahi.
- Privacy Policy + Terms + consent screens release se pehle mandatory.
- Permanent law list (Table A universal) har build par lagu — DPDP, IT Act,
  Copyright/Trademark, Consumer Protection, GST/Income Tax triggers.

## 6. Testing (release gate — dono zaroori)
- Server-side: DB check (tables, seeds, licence verify, usage count, RLS).
- Phone-side: install, permissions, wake word, Guardian samples, tools,
  calls, background + scheduled job, offline/weak network, battery.
- Phone-side result sirf user report par final NAHI — DB/server check se
  confirm hoga. Abhi phone test **0%** hai.

## 7. Deep audit findings (2026-10-27 ~21:25 IST, fresh evidence)
- Repo: 24 files, workflow file present (a784d0c fix ke baad).
- Actions: **5/5 runs failed**. Pehla fail: Set up Android SDK step.
  Fix ke baad ke runs: **Build debug APK step fail** (log download egress
  timeout se fail hua, exact Gradle error abhi UNVERIFIED hai — guess nahi).
- Pakka candidate fix (abhi kiya gaya): app icon res/ root me tha
  (invalid resource dir, aapt fail kar sakta hai) → drawable/ me move
  kar diya, res-root file delete kar di. Agla run is fix ko test karega.
- VM Gradle: daemon communication failure, 4 attempts fail (memory
  available ~1 GB) — isliye Actions hi build route hai.
- Vercel: deployment READY, par protection on hai — public verify pending.
- Supabase mgmt token: 401 (FormMitra account) — Flora account ke liye
  koi mgmt token nahi hai, dashboard hi rasta hai.
- GitHub API token: workflow scope nahi hai (workflow file web se hi
  add/update hoti hai); GraphQL createCommitOnBranch FORBIDDEN hai.
- Turso stored credential: watcher ko JWT InvalidToken deta hai —
  platform token se table verify ho chuka hai, isliye ye watcher-level
  issue hai, Flora DB ka block nahi hai.
- Schema file me duplicate on-conflict line mili thi — fix karke repo
  + browser dono me corrected version chalaya gaya.

## 8. Release gates (sab pass hone par hi APK + site public)
1. Actions build PASS + APK artifact verified (dex/manifest check).
2. Supabase Edge Functions deployed + licence/usage live test PASS.
3. Vercel site public (protection off) + live content check PASS.
4. App modules implemented + phone test (user phone) + DB check PASS.
5. Privacy/Terms/consent screens + FCM push test PASS.
6. Backup restore test PASS. 7. Final backup snapshot + SHA verify.
Tabhi "production-ready" kaha jayega — usse pehle nahi.

## 9. Rollout order (milestone-based, koi deadline promise nahi)
1. Licence server + Edge Functions deploy → 2. App core + voice →
3. Tools (consent routes) → 4. Security (Guardian/PIN) → 5. Licence+
payment+update → 6. Maya Home hardware track (alag) → 7. Test+audit+
APK+backup. Har step ke baad Verify → Test → Audit, tabhi agla step.

## 10. Open decisions (user ke paas, koi guess nahi)
- Flora wake phrases (original 3 vs naya Flora phrase) — confirm baaki.
- FCM: Firebase project + google-services.json + server key — user ke
  Firebase console se aayega.
- Full payment provider — baad me, alag approval ke saath.
- Custom domain — user jab khareede tab attach hoga (abhi Vercel domain).
- Phone test — user ke phone par, uske time par.
