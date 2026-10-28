# Flora neural voice plan — system TTS hatana, Sample-2 jaisi awaaz (2026-10-28)

**User order:** Sample 2 (Lily) jaisi awaaz Flora me chahiye,
"anyhow" — open-source model ya koi bhi tarika; system TTS
hatao. Maya se bhi achha karo.

## Imaandari se pehle (ye baat chhupayi nahi jayegi)

- **Sample 2 (Lily) ki EXACT awaaz koi open-source model nahi
  de sakta.** Lily ek proprietary voice hai — uski copy/clone
  banana (bina permission, bina data) galat bhi hai aur
  possible bhi nahi hai. Jo milega wo usi STYLE ki awaaz hai:
  gahri, dheemi, mellow, ladki ki awaaz, Hindi me.
- **Maya me kya tha:** Pocket on-device neural TTS (Kyutai
  engine) + Hunter cloud TTS. Dono creator ke hain — Flora me
  kabhi copy nahi honge. Flora ko unse ACHHA banana hai to
  raasta hai: licence-verified open-source neural engine +
  Hindi female voice + apni tuning.
- **System TTS kab hatega:** tabhi jab neural engine phone par
  chal kar, user ko sample suna kar, user ki haan ke baad.
  Pehle hatana = Flora goongi ho jayegi. Isliye order ye hai:
  neural engine side-by-side → sample sunao → user select
  kare → tab system TTS hatao. Ye koi delay nahi, safety hai.

## Engine choice (licence-verified, free, on-device)

1. **Kokoro (primary candidate) — Apache 2.0, free:**
   82M param neural TTS, Hindi support hai (lang code `h`,
   female voices `hf_alpha`, `hf_beta` jaise). Quality
   natural, human-jaisi. Model ONNX me chalta hai — Android
   par ONNX Runtime se chal sakta hai.
2. **Piper (fallback) — MIT, free:** lightweight, fast,
   Hindi voices maujood hain (community-trained). Quality
   Kokoro se thodi kam, par phone par sabse halka.
3. **Sherpa-ONNX (Android glue) — Apache 2.0:** dono engines
   ko Android par chalane ka maintained raasta (AAR milta
   hai) — isi se Flora me wiring hogi.

## Flora me wiring plan (audit gate ke saath)

1. **Model download (bucket pattern):** model repo me NAHI
   jayega (22 MB GraphQL limit; model 100+ MB ka ho sakta
   hai). `flora-models` bucket me SHA-verify download —
   wahi pattern jo conformer model par kaam kar chuka hai.
   One-time download, phir hamesha on-device, bina internet,
   bina key, bina kharch — fully automated.
2. **VoiceEngine interface same:** `speak(text)` wahi rahega —
   andar system TTS ki jagah neural engine bolega. Isliye har
   jagah (chat, background, Auto Mode) apne aap badal jayegi.
3. **Sample comparison:** neural engine se 2-3 Hindi female
   samples bana kar user ko sunaye jayenge — user jis ko
   chune, wahi Flora ki awaaz banegi.
4. **System TTS removal (last step):** sample approve + phone
   test pass hone ke baad hi system-TTS path hataya jayega.
   Tab tak dono rahenge, neural default hoga.
5. **Per-user note:** neural voice on-device hai — isme koi
   key nahi lagti, koi server nahi, har user ki apni phone
   par uski apni awaaz chalegi. Ye user ke "one time setup,
   free, fully automated" wale niyam par poora utarta hai.

## Maya se achha kaise

- Maya ki awaaz cloud + on-device dono thi, par uski voice
  data creator ki thi. Flora ki awaaz open-source + apni
  tuning hogi — koi licence ka bojh nahi, koi server kharch
  nahi, offline bhi chalegi.
- Plus Flora ka sample-selection + Settings selector (pehle
  se bana hua) — user kabhi bhi awaaz badal sakta hai.
