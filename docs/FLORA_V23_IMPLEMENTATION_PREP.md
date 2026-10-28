# Flora v2.3 voice/face match — implementation prep (LOCAL, push nahi)

Ye file sirf taiyaari hai. Push tabhi hoga jab v2.2.1 ka CI audit
PASS ho jaye (per-item gate). Koi model binary, koi key, koi
template is file me nahi hai.

## Pehle se maujood cheezein (dobara add nahi karni)
- `tensorflow-lite:2.14.0` app/build.gradle.kts me pehle se hai.
- `com.google.mlkit:face-detection:16.1.7` pehle se hai (Face
  screen presence-only ke liye use ho rahi hai).
- `security/PinVault.kt`: AndroidKeyStore AES/GCM pattern pehle
  se hai — template storage isi pattern par banega, naya
  crypto design nahi likha jayega.

## Face-match — seedha raasta
- Pipeline: ML Kit detection → face crop → 112x112 resize →
  MobileFaceNet TFLite → 192-d embedding → enrolled template se
  cosine similarity.
- Licence: upstream sirius-ai/MobileFaceNet_TF Apache 2.0
  (2026-10-28 verified). Kisi repo ka Kotlin/Java code copy
  nahi hoga — sirf model file + apna inference code.
- Threshold: phone par tune karna hoga; kisi doosre repo ka
  0.8 score-threshold yahan cosine par copy nahi karna (wo
  score conversion tha, cosine nahi). Test entry me match /
  non-match dono cases likhne hain.
- Model file repo me daalne se pehle: size + sha + source
  note karna hai. APK size budget: v2.2 APK 54,540,574 bytes;
  ~5 MB model se ~59-60 MB tak jana expected hai — push plan
  me likhna hai.

## Voice-match — ASLI RISK (isliye alag step me validate hoga)
- Conformer speaker encoder ko seedha wav nahi diya ja sakta.
  Usse pehle chahiye: VAD model, phir stacked log-mel features
  (32 ms frame, 10 ms step, 128 mel bins, left-context stack 3,
  stride 3 → 512-d feature) — ye frontend sidlingvo (Python)
  me hai, Kotlin me nahi.
- Isliye v2.3 ka voice hissa do charan me hoga:
  1. Kotlin mel-frontend prototype: ek hi wav par sidlingvo
     embedding vs Kotlin embedding — cosine > 0.99 aaye tabhi
     aage badhna hai. Ye prototype phone test se pehle, local
     verification hai.
  2. Uske baad hi enrolment (3-5 samples, mean embedding) +
     live match UI.
- Agar frontend prototype match nahi karta, to voice-match ko
  "kaam karta hai" claim nahi karna — face-match akela bhi
  PIN unlock nahi karega (design rule barkarar).
- Licence: tflite-hub/conformer-speaker-encoder, Hugging Face
  API metadata me license apache-2.0, gated nahi (2026-10-28
  verified). Files: conformer_tisid_medium.tflite +
  vad_long_model.tflite + vad_long_mean_stddev.csv. Poora
  repo storage ~30.3 MB (medium + small + vad) — medium ka
  alag size implementation waqt note hoga.

## Naye files (implementation waqt, isi order me)
1. `data/BiometricTemplates.kt` — KeyStore-encrypted template
   store (face embedding + voice embedding alag-alag keys,
   kabhi server/backup par nahi).
2. `core/FaceMatcher.kt` — ML Kit crop + TFLite inference +
   cosine (apna code, koi copy nahi).
3. `core/VoiceMatcher.kt` — mel-frontend + VAD + conformer
   inference + cosine (prototype pass ke baad hi).
4. Settings me enrolment screen — consent pehle, default OFF,
   samples sirf on-device, delete-all button ke saath.
5. Test entry update: docs/FLORA_VOICE_FACE_TEST_ENTRY.md me
   frontend-prototype step + phone match/non-match steps.

## Declaration-order / audit checklist (push se pehle)
- Har nayi state/launcher pair: state ki line < launcher ki
  line (grep se verify, comment par bharosa nahi).
- Nullable state (Uri?, Bitmap?) ko non-null API me dene se
  pehle `?: return` guard.
- Brace/paren balance har file me 0 diff.
- Koi template, embedding, audio sample, token kabhi log
  nahi hoga; koi model binary commit message me nahi.
- Phone test ke bina audit PASS nahi (design me pehle se
  likha hai) — CI PASS sirf build gate hai.
