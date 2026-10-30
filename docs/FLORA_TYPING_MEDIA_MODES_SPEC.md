# FLORA TYPING / MEDIA MODES — SPEC (parity batch 2026-10-31, M10)

## Records se kya mila (verified, isse zyada daava nahi)

- Maya records me "typing / media modes" ki koi definition nahi
  mili — sirf gap-list entry M10. Isliye ye modes Flora-new
  definitions hain, Maya copy nahi.

## Flora-new definitions (is cycle me implement hua)

- **Typing mode (default OFF):** ON ho to chat screen ka Mic
  button chhup jata hai — user sirf likh kar baat karta hai.
  Continuous mic / voice view / Baat view isse band nahi hote;
  ye sirf chat row ka Mic button hatata hai. Toggle Settings
  me hai; prefs me save hota hai.
- **Media mode (default ON):** ON = Flora ke jawab system TTS
  ke default (media) stream par — maujooda vyavhaar, isliye
  default ON rakhne se kuch badalta nahi. OFF = voice-call
  stream (earpiece ki taraf, private). Sirf system TTS path par
  asar karta hai; neural engine (Kokoro) apna routing use karta
  hai, is par nahi. Toggle Settings me hai; prefs me save hota
  hai; FloraApp startup par VoiceEngine.mediaMode set karta hai.

## Jo nahi hai

- Koi auto voice-to-text mode change nahi, koi media playback
  mode nahi (music control alag feature hai), koi STT engine
  switch nahi — ye modes sirf upar likhi do cheezon par hain.
