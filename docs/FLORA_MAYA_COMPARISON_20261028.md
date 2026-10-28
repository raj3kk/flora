# Maya vs Flora — poora comparison (2026-10-28, evidence se)

Scope note: Maya wali baatein decompiled APK + videos + dex/endpoint
evidence se hain (paraphrase, koi creator code/text copy nahi).
Flora wali baatein live code + Supabase + CI-verified builds se hain.
"Behtar" ka faisla har point par alag hai — neeche table me saaf hai.

## 1. Naam, daam, licence

| Point | Maya (original) | Flora (rebuild) |
|---|---|---|
| Naam/package | Maya, com.getmaya.android, v5.2.44 | Flora, com.flora.app, v2.20 |
| Free | 10 min/day, ~15 tools | 10 min/day, saare tools maujood |
| Paid | ₹699 base, ₹798/₹948 bundles, Pro Max ₹99/month, MAYA5 5% off | ₹199/month subscription (FINAL, user order) |
| Licence rule | 1 phone, max 1 change/15 din, sign-out email OTP, key website dashboard me | 1 phone, 15 din rule server par (licence-verify function me), UPI + admin UTR approve |
| Payment | Website checkout page (web link) | UPI deep link + UTR submit + admin approve (koi gateway nahi) |
| Distribution | Sideload APK | Sideload APK (Play Store par nahi) |

## 2. Kya server par tha / hai, kya app par

### Maya — server par
- Model downloads: guardian/STT/turn/addressee manifests + files,
  avatar catalog + posters (getmaya.online).
- Awaaz banana: Hunter TTS server (api.getmaya.cloud/tts/v1).
- Maya Home sync: join / genuine check / WebSocket
  (sync.hunterislive.online).
- Licence khareedna + key dashboard: website (getmaya.online).
- Brain/search/tools: app se SEEDHA provider APIs par jaate the
  (Gemini, Anthropic, Tavily, Brave, Telegram, Todoist, Linear,
  Vercel, Google TTS, Drive) — beech me Maya ka apna central
  chat-history server nahi mila.

### Maya — app par (phone ke andar)
- On-device neural models: wakeword TFLite, guardian models,
  STT/turn/addressee ONNX (onnxruntime), face detection,
  macros, voices — sab phone par chalte the.
- Data: app ki apni koi Room tables NAHI mili (negative
  finding) — memories/chat/licence DataStore/encrypted files
  ya sync se chalte the, poora schema kabhi recover nahi hua.
- 54 screens, 45 voice destinations.

### Flora — server par (Supabase Flora, live)
- Tables: users, licences, plans, settings, usage,
  payment_submissions, push_tokens, admin_audit (8 tables,
  RLS ON; 5 par policies nahi — admin-list function se hi
  admin dekh sakta hai).
- Edge Functions (7, live): licence-verify, usage-count,
  send-push, admin-approve, push-token-sync, admin-list,
  brain-proxy (keys abhi unset — owner ko dashboard me
  daalni hain; app abhi isse judi nahi hai).
- Website (Vercel, live): signup, login, pricing, pay
  (UTR submit), account, admin, download.

### Flora — app par (phone ke andar)
- DataStore: keys, memories, journal, routines, auto log,
  talk seconds, face templates, voice enrolment — sab local.
- On-device: MFCC wakeword template (v0, guess threshold),
  MobileFaceNet face-match (threshold 0.55), system STT/TTS,
  ML Kit scan (Latin + Devanagari), camera.
- Screen content KABHI server par nahi jaata (privacy rule);
  vision frame sirf user ke sawaal par user ki key se jaata hai.

## 3. AI kahan use hota hai — dono me

### Maya me AI
- Brain: Gemini (primary, streaming bhi), Anthropic backup,
  aur bhi LLM lanes (Groq/OpenAI/Mistral/Together/xAI/
  DeepSeek/DeepInfra/NVIDIA domains mile).
- Voice: on-device neural STT (Hunter STT ONNX) + turn
  detector + addressee model + cloud Hunter TTS + Google TTS.
- Vision: screen/frame dekh kar jawab (Gemini vision).
- Tools: search (Tavily/Brave), Telegram bot, Todoist,
  Linear, Vercel deploy, Drive — sab app se seedha.
- Wake word: neural pipeline (dataset 10,000+ clips,
  PyTorch → ONNX → TFLite) — accuracy claim unverified.

### Flora me AI
- Brain: Gemini → Groq (user ki keys se, app se seedha).
  v2.20 se model auto-discovery (key par jo model chale,
  wahi use hoga — naam sadne ka khatra khatam) + brain
  down ho to BasicTalk (local, bina AI ke basic baat).
- Voice: system STT/TTS (free, on-device voices) — koi
  neural STT/TTS abhi nahi; Deepgram/ElevenLabs lanes
  plan me hain, key user dega tabhi.
- Vision: LiveScreen frame → Gemini vision (user key se).
- Tools: Tavily/DuckDuckGo search, Yahoo Finance,
  Open-Meteo mausam, Pollinations images (no-key),
  ChatGPT automation (image), Play Store install.
- Wake word: MFCC template v0 (neural nahi — training
  ke liye user ki recordings + FCM files ka intezar hai;
  voice-match validation PASS hai, Kotlin port pending).
- Brain-proxy: server par taiyaar hai par keys unset +
  app se wiring pending (owner ke faisle par).

### Kya baat karne ke liye AI lagana padega?
- Haan — asli baat-cheet (gehre sawaal, samajh, jawab)
  ke liye AI brain chahiye hi (Gemini/Groq). Bina AI ke
  Flora sirf BasicTalk kar sakti hai: greeting, haal-chaal,
  naam, time/date, shukriya, alvida, ek joke — isse zyada
  nahi. Isliye: basic baat = local (v2.20 me hai);
  poori baat = AI key chahiye (free Gemini key kaafi hai).

## 4. Kya gadbad hai — Maya jaisa theek hona chahiye

| Gadbad (aaj) | Maya me kaisa tha | Flora me fix |
|---|---|---|
| Brain 404 bol kar technical error sunata tha | Maya seedha jawab deta tha, error nahi sunata tha | v2.20: chat me technical baat kabhi nahi; warm fallback + BasicTalk; asli kaaran sirf Keys check me |
| Model naam sad jaate the (1.5-flash, phir 2.5-flash par bhi 404) | Maya ke models server se list aati thi | v2.20: auto-discovery — key par jo chale wahi use hoga |
| Screen share: dobara "frame nahi mila", koi app nahi khulti thi | Maya me screen + automation chalta tha | v2.20: persistent session (stop nahi hoga), 3 retry, "App kholo aur dekho" flow |
| Awaaz me garmahat nahi, sirf "key gadbad" bolta tha | Maya ki awaaz pyari + character wali thi | v2.20: warm prompt (muskaan + pyar se) + TTS pitch/rate narm + BasicTalk tone |
| Bahut kuch automatic nahi hai | Maya me macros + automation zyada the | Auto Mode default ON (v2.18) — call/SMS/open/routine auto; tap/type/swipe P2 me pending hai |

## 5. Abhi bhi kya kami hai (imaandari se)

- Sachhi barge-in (bolte hue rokna) nahi — Deepgram key
  user dega tabhi (F1).
- WhatsApp automation nahi (F3) — confirm + Auto Mode rule
  par banega (P4).
- Tap/type/swipe general automation nahi (F4) — sirf
  ChatGPT flow me hai (P2).
- PC link, Home dashboard, game commentary, docs+email,
  whiteboard, watchlist, code-writer (stub hai), research
  loop — sab P2–P5 me pending hain (screenshot plan doc me).
- Voice Guardian Kotlin port pending (validation PASS hai).
- Neural wakeword pending (user ki recordings ka intezar).
- Brain-proxy keys unset + app wiring pending (owner).
- Admin user abhi bana nahi (pehla website signup pending).
- Phone tests pending: wake/face/FCM-sync/usage/app-link/
  licence block-transfer (asli Pro licence ke baad hi).

## 6. Kaun kis point par behtar — seedha faisla

| Point | Behtar | Wajah |
|---|---|---|
| Baat-cheet ki samajh (brain) | Maya (tab), Flora (ab fix ke baad barabar) | Maya ke zyada lanes the; Flora ke 2 lanes + discovery + fallback ab kaafi hain |
| Awaaz (STT/TTS) | Maya | On-device neural STT/TTS + character voices; Flora system voices par hai |
| Wake word | Maya | Neural model; Flora v0 template hai |
| Licence/payment system | Flora | Server-enforced 1-phone/15-din + UTR approve, admin audit — Maya se zyada saaf |
| Privacy | Flora | Screen kabhi server par nahi; data local + 30-din retention; Maya ka schema hi nahi mila tha |
| Auto Mode | Flora | Default ON + Auto Log + notification — Maya me ye khula system nahi tha |
| Automation (tap/type/swipe) | Maya | Flora me abhi sirf ChatGPT flow + open/call/SMS hai |
| Home/IoT | Maya | Flora me Home abhi P5 me pending hai |
| Price | Flora | ₹199/month vs ₹699+ bundles |
| Tools/connectors | Maya | Telegram/Todoist/Linear/Vercel/Drive — Flora me abhi nahi hain |
| Naya banana/audit | Flora | Har item audit gate + CI verify + test entry — Maya rebuild isi liye ho raha hai |

Aakhri line: aaj Flora Maya se har cheez me behtar nahi hai —
awaaz, wake word aur automation me Maya aage tha. Licence,
privacy, Auto Mode aur price me Flora aage hai. Baaki ka
farq P1–P5 poora hone par mitega — kaam rukega nahi.
