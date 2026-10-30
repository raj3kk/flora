# Phase 2 — Item 1: Heavy-lib usage audit + shrink (evidence pehle)

**Base tip:** `d9c000e5bd7f86008c3e21ddfbe980b25e27efef` (Phase 1 final tip,
2026-10-30 ~22:52 IST fresh verify). Local snapshot blob-sha match se
verify karke likha gaya hai; stale content push nahi hua.

**Baseline (evidence):** debug APK `debug-build-307/308/309` =
**233,819,077 bytes** (teeno barabar). Neeche ka breakdown
`app-debug-309.apk` ke ZIP se hai (uncompressed sizes, MB):

| Hissa | 4 ABI total | arm64-v8a akela |
|---|---|---|
| onnxruntime `libonnxruntime.so` | 112.75 | 27.31 |
| MLKit OCR pipeline `libmlkit_google_ocr_pipeline.so` | 36.61 | 9.54 |
| MLKit face `libface_detector_v2_jni.so` | 31.72 | 8.13 |
| TFLite JNI `libtensorflowlite_jni.so` | 15.35 | 3.66 |
| dex (saara Kotlin/Java code) | 35.71 | 35.71 |
| `assets/MobileFaceNet.tflite` | 4.99 | 4.99 |
| `assets/models_bundled/*` (face dep ke andar ke models) | ~3.4 | ~3.4 |
| MLKit OCR model assets | ~2.5 | ~2.5 |
| `assets/voice_match/*` (VAD + mel matrix) | ~0.71 | ~0.71 |
| **Total uncompressed** | **246.08** | **~99.6** |

Matlab: sirf arm64-v8a rakhne se native hissa ~196 MB se ~49 MB
hota hai — ~147 MB uncompressed ki bachat, bina koi feature chhede.
Ye Item 2 ka kaam hai; Item 1 me koi lib hatayi NAHI gayi (wajah
neeche).

## Per-lib audit (call-site evidence ke saath)

### 1. onnxruntime-android 1.28.0 — KEEP on-device (b)

- **Asli call sites:** `app/models/KokoroOrtEngine.kt`
  (`ai.onnxruntime.OnnxTensor/OrtEnvironment/OrtSession` — poora
  inference engine), `app/models/NeuralVoiceAttach.kt` (app-start par
  async attach), `app/models/KokoroModelManager.kt` (model download +
  status), `app/assistant/NeuralSpeaker.kt` (engine null ho to system
  TTS fallback — awaaz kabhi nahi rukti).
- **Model bundle NAHI hai:** Kokoro-82M model (325,532,387 bytes) +
  voices (28,214,398 bytes) sirf runtime download hain
  (`FloraConfig.KOKORO_MODEL_URL/VOICES_URL`, SHA-verify ke saath).
  APK me onnx ka sirf native `.so` hai, model nahi.
- **User-visible hai:** Models screen me neural-voice download button
  + status UI (`FloraApp.kt` ~line 6031, 7013-7051). Hatana = chupchaap
  feature katna — mana hai, isliye nahi hataya.
- **Server path NAHI hai (imaandaari se):** Supabase Edge Functions me
  onnxruntime nahi chalta, Vercel free par 82M-model inference ka
  koi asli raasta nahi. Server TTS (Phase 1 lane) ek ALAG awaaz hai,
  Kokoro ka replacement nahi — isliye "server par le jao" ka daava
  nahi kiya gaya.

### 2. MLKit text-recognition + devanagari — KEEP on-device (b)

- **Asli call sites:** `app/ui/ScanScreen.kt` (Latin client +
  Devanagari client, dono; `InputImage.fromFilePath` se OCR).
- **Server equivalent hai par behtar NAHI:** `VisionClient` (Gemini
  vision) camera sawaal ke liye pehle se hai, par wo user-key par
  nirbhar, online-only aur slow hai. Scan OCR offline, free aur
  bina key ke chalta hai — server par le jaana thin-app ka fayda
  nahi, nuksaan hota.

### 3. MLKit face-detection — KEEP on-device (b)

- **Asli call sites:** `app/ui/FaceScreen.kt` (front-camera preview +
  `FaceDetection.getClient`, presence check + enrolment crop).
- **Privacy wajah:** face presence check phone par hi hona chahiye —
  chehra server par bhejna privacy posture ke khilaaf hota.
  Isliye server path kharij.

### 4. TFLite 2.14.0 + assets — KEEP on-device (b)

- **Asli call sites:** `app/core/FaceMatcher.kt`
  (`assets/MobileFaceNet.tflite`, 5,233,396 bytes — 192-d embedding,
  GuardianGate ke saath), `app/voice/VoiceMatcher.kt`
  (`assets/voice_match/vad_long_model.tflite` 481,664 bytes +
  `mel_matrix_f32.bin` 262,656 bytes).
- **Encoder pehle se hi thin hai:** `conformer_tisid_medium.tflite`
  APK me bundle NAHI hai — server se download hota hai
  (`web/models/manifest.json`, SHA-verify, 22,134,696 bytes).
  Voice-match bina encoder ke `isAvailable = false` batata hai,
  kabhi fake match nahi deta.
- **Security wajah:** face-match + voice-match GuardianGate
  (PIN + awaaz) ke hisse hain — offline + private rehna zaroori hai.

### 5. CameraX — KEEP (phone-only capability)

- **Call sites:** `ScanScreen.kt`, `FaceScreen.kt`,
  `CameraLookScreen.kt` (sab `ProcessCameraProvider` + `ImageCapture`
  pattern). Task ke mutabiq phone par screen dekhna + camera
  rehna hi hai.

### 6. `models_bundled/*` (contours.tfl, LMprec_600.emd, fssd*, blazeface.tfl)

- **Code me ZERO reference.** Ye face-detection AAR ke andar ke
  models hain, alag dependency nahi — inhe alag se hataya nahi ja
  sakta; hatane ka ek hi raasta face-detection hatana hota, jo
  FaceScreen ko maar deta. Isliye chheda nahi gaya.

### 7. Chhoti dependencies — sab used, koi dead nahi

DataStore (33 files), Room (5), WorkManager (4), Timber (3),
App Startup (2), OkHttp (9), kotlinx-serialization (7),
play-services-auth (GoogleSignIn, `FloraApp.kt` ~6183, ~6783),
Firebase Messaging (`push/FloraMessagingService.kt` + FCM token),
Shizuku API (`automation/ShizukuBridge.kt`). Koi dead dependency
nahi mili — isliye Item 1 me koi dependency hatayi nahi gayi.

## Faisla (Item 1 gate)

- **Hataya gaya: KUCH NAHI.** Har bhari lib ka asli runtime use hai;
  har server-move ka daava ya to jhootha hota (ORT/MLKit Edge
  Functions par nahi chalte) ya feature/privacy ka nuksaan hota.
- **Phone par raha (task ke mutabiq):** mic, speaker, screen dekhna
  (AccessibilityService + vision), tap/swipe/type, wake word
  (`wakeword/WakeWordService.kt` — pure-Kotlin MFCC, koi bhari lib
  nahi).
- **Asli shrink Item 2 se aayega:** arm64-only + R8/minify (release
  variant). Debug flow (CI `assembleDebug`) usse chheda nahi jayega.

## 107 MB wale published build ka farq (pehli jaanch)

- Download page (tip `web/download.html`) ke mutabiq published build
  v2.22.0-rebuild = **112,590,325 bytes (107.4 MiB)**, debug-signed,
  SwissTransfer se milta hai. (Task brief me 107,590,325 likha tha —
  page ke figure se 5,000,000 ka farq hai; page ka figure source of
  truth mana gaya hai.)
- 233 MB debug se farq ki sabse badi wajah ABI + build-type lagti
  hai: 4 ABI native (~196 MB) ke badle arm64 akela (~49 MB) +
  debug-vs-release farq. Item 2 ka release build is hypothesis ko
  number se prove karega ya todega — abhi sirf hypothesis hai,
  daava nahi.
- Page par ye bhi likha hai: website se seedha APK download Vercel
  free-plan ki 50 MB limit ki wajah se band hai — ye Item 3 ke
  channel faisle par seedha asar dalta hai (APK Vercel se nahi
  milega).

## Kya phone-verified NAHI hai

Ye audit sirf code + APK evidence par hai. Koi bhi size ya feature
ka daava phone par test NAHI hua hai — Phase 3 (phone tests +
sign-off) se pehle koi "thin app tayyaar" ka daava nahi.
