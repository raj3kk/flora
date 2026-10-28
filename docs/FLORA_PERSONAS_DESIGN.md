# Personas — design (v1.8, one-by-one: attachments ke baad)

## Kya banega (Flora ke APNE personas, creator ke nahi)
- 3 personas, user Settings/Chat se chunega (prefs PERSONA key):
  1. Flora (default) — friendly + professional, jaisa abhi hai.
  2. Saathi — aur dostana, chhote jawab, hausla dene wala tone.
  3. Guru — padhai/kaam par focus, step-by-step, to-the-point.
- Asar: Chat ke brain prompt ke shuru me ek chhoti style line judi
  hogi (BrainClient.ask(prompt) ko prompt ke saath style prefix
  milega — brain ka code nahi badlega, sirf call site par prefix).
- Voice pitch/rate: VoiceEngine me abhi setPitch/setSpeechRate nahi
  hai — personas v1 me awaaz nahi badlegi, sirf tone/shabd badlenge.
  Pitch wala hissa alag step hoga (TTS settings test ke saath).

## Audit points
- Persona sirf style hai — 18+/romantic persona kabhi nahi (excluded list).
- Prefix user ke asli sawaal ko nahi badlega, sirf tone guide karega.
- Default Flora hi rahega jab tak user khud na badle.
- Koi creator character/naam/awaaz copy nahi hogi.
