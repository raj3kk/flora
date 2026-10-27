# Spec — Assistant (voice pipeline: wake word → STT → LLM → TTS)

Source: behaviour only — Wake Word video (3 phrases, guards, metrics),
V5 video (voice engines, STT/TTS choice, languages), manifest services,
native wiring evidence (PocketTTS/HunterSTT JNI names, wiring dobara likhni
hai, native code copy nahi). Folder: assistant/ (ABHI KHAALI HAI).

## Files (honi chahiye)
- assistant/MayaAssistActivity.kt — default assistant UI (power long-press /
  corner swipe / lock screen se khulna); user khud default assistant banaye.
- assistant/MayaVoiceInteractionService.kt — voice interaction service.
- assistant/MayaVoiceInteractionSessionService.kt — session lifecycle.
- assistant/MayaRecognitionService.kt — recognition service.
- assistant/WakeWordDetector.kt — on-device TFLite: melspectrogram +
  embedding + hey_maya + wake_up_maya (+ hello_maya optional); sirf 3
  phrases: Hey Maya, Hello Maya, Wake Up Maya.
- assistant/SpeechPipeline.kt — STT (Google free / Deepgram) → LLM router
  (Gemini → Groq → OpenRouter → backup) → TTS (system / Deepgram /
  ElevenLabs / on-device); streaming jawab, TTS turant shuru, lamba kaam
  background me chalte hue baat jaari, poora hone par khud batana.
- assistant/VoiceGuard.kt — Guardian ke saath judna (Boss, ≤10 samples,
  threshold 0.86, Owner-only + Away mode; non-owner ko refusal).

## Behaviour
- Wake guards: energy gate, minimum 4 frames, last 25 fingerprints = 2 sec.
- Mic 24x7 sirf wake word ke liye sunta hai — privacy disclosure + visible
  indicator + on-device processing mandatory; recording/transcript sirf
  user ke kehne par, disclosure ke saath.
- Metrics apne honge: false-accept / false-reject / per-environment /
  battery — creator ke "89%+" jaise daave copy nahi karne.
- Short command bhi context + memory se samajhna; lamba kaam background
  me, parallel conversation jaari.

## Acceptance (Verify → Test → Audit)
1. Shant kamre me 10 baar wake word: kam se kam 9 baar jage; TV shor me
   false wake note karo (koi single-number accuracy claim nahi).
2. Koi crash / hidden exception nahi.
3. Mic off / incognito / delete kaam kare.
4. Koi purani API key code me nahi hai; nayi keys Secure Vault se hi.
5. Test log docs me darj hai.
