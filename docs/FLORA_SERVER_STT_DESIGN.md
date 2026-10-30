# Flora server STT lane — design (Phase 1, Item C)

Status: DESIGN + REPO CODE ONLY. Function `supabase/functions/stt-proxy/index.ts`
repo me hai, DEPLOY NAHI hua hai. Deploy parent-browser deploy session #2
me Section F ke saath coordinated hoga. Live STT claim tab tak nahi.

## Aaj ki haalat (pre-audit)

- Server par STT ka KOI function nahi hai (21 functions me brain-proxy,
  tts-proxy, licence-verify v2, usage-count, send-push, admin-* (10),
  feature-flags, public-settings, heartbeat, home-sync maujood hain).
- App me STT ke do raaste hain: (1) system SpeechRecognizer
  (ContinuousListener + mic RecognizerIntent, hi-IN) — ye fallback hamesha
  rahega; (2) Deepgram REST batch (voice/DeepgramClient, user ki Settings
  key se seedha api.deepgram.com par) — isi ko server lane badlega.
- Deepgram key PENDING hai (user/parent-gated) — key ke bina stt-proxy
  503 dega, isliye app wiring is round me nahi ki gayi.

## Faisla: batch (REST) v1, streaming baad me

- Batch chuna gaya kyunki app ke dono maujooda STT raaste batch hain
  (poora utterance -> ek transcript). Batch me mic coordination, partial
  transcript handling aur barge-in ka naya state machine nahi chahiye.
- Streaming WebSocket (sachhi barge-in, DeepgramStreamClient ki taraf)
  future kaam hai: usme socket lifecycle, partial/final transcript merge
  aur ContinuousListener ke saath mic ownership tay karna hoga. Phase 1
  me streaming ka daava nahi hai.

## Contract (stt-proxy v1)

- POST `/functions/v1/stt-proxy`, header `Authorization: Bearer <user JWT>`.
- Body: `{ audio_base64, language? }` — audio_base64 = WAV (16k mono PCM,
  app ke DeepgramClient.wavBytes jaisa), language `hi` (default) ya `en`.
- Cap: base64 4,000,000 chars (~3 MB raw, ~90 sec) — usse zyada 400.
- Response success: `{ transcript, language }`. Audio kabhi save nahi
  hota, transcript ke alawa kuch wapas nahi jaata.
- Errors (imaandaar, koi fake transcript nahi): 401 unauthorized (JWT
  nahi/galat), 400 (audio missing/invalid/too large, language galat),
  429 free_limit, 503 stt keys not configured, 502 stt provider failed
  (provider down ya transcript khali).
- JWT gate: anon client se JWT user nikalo; body me user_id nahi hota,
  JWT user hi user hai (brain-proxy ke user_id-match rule ki yahan
  zaroorat nahi kyunki caller apna hi audio bhejta hai).
- Key resolution: env `DEEPGRAM_API_KEY` FIRST, else provider_keys DB row
  ("deepgram") — tts-proxy v2 wala hi pattern; STT/TTS ek hi key share
  karte hain. Key kabhi log/response me nahi.
- Licence gate: tts-proxy v3 wala read-only gate (Pro valid ya Free
  < 600 talk sec/day, nahi to 429). Gate sirf padhta hai, increment
  nahi karta; DB error par fail-open. IMAANDAAR NOTE: aaj usage-count
  sirf boli hui (TTS) seconds ginta hai — STT input seconds us counter
  me nahi judte, isliye STT par ye gate approximate hai; usage-count
  ke STT cover karne tak fail-open hi rahega.

## Fallback (kabhi nahi tootega)

- Server fail/unavailable/503/502 par app SYSTEM STT par girti hai
  (ContinuousListener / RecognizerIntent) — offline planner ki tarah
  mic ka hard dependency server par nahi banega.
- App-side wiring is round me NAHI ki gayi (function deploy + key live
  hone ke baad, alag gate par hogi) — isliye aaj ka mic behavior
  bilkul waisa hi hai jaisa Phase 0 tip me tha.

## Live-gated (user/parent par)

1. Deploy session #2 (parent browser) — function deploy.
2. Deepgram key set (dashboard secret ya provider_keys row).
3. User-JWT probe (parent browser) — asli linked user se success path.
4. Uske baad app-side STT wiring (alag item, is Phase 1 me nahi).
