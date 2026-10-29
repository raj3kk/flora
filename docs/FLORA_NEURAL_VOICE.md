# Flora Neural Voice — Kokoro hf_alpha (Phase 8)

## User-final voice
**Kokoro hf_alpha** (user selection 2026-10-29 ~03:35 IST:
"Ye wala karo, final ho jayega na har ek place par").
Hindi female voice, on-device, Apache-2.0 weights.

## Model source (PUBLIC, verify kiya hua)
- **Repo:** `hexgrad/Kokoro-82M` (v1.0, 27 Jan 2025) —
  HuggingFace page + kokoro-onnx docs se confirm: files
  `kokoro-v1.0.onnx` + `voices-v1.0.bin` isi repo ke hain.
- **Model URL:** `https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/kokoro-v1.0.onnx`
  (325,532,387 bytes)
- **Voices URL:** `https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/voices-v1.0.bin`
  (28,214,398 bytes)
- **SHA-256 (2026-10-29 ke download se verify):**
  - model: `7d5df8ecf7d4b1878015a32686053fed0eebe2bc377234608764cc0ef3636a6c5`
  - voices: `bca610b8308e8d99f32e6fe4197e7ec01679264efed0cac9140fe9c29f1fbf7d`
- **Total ~354 MB.** APK me bundle NAHI — runtime me ek baar
  download (`KokoroModelManager`), SHA verify ke baad hi `.ok`
  marker likha jata hai. Galat hash = reject + `.part` delete.
  Storage: `filesDir/models/kokoro/`.

## Kya is build me poora hai
- Download (progress % + 3 attempts + retry button, Settings me
  "Neural voice model download karo" section).
- Readiness check (`.ok` + dono file).
- Routing: `NeuralSpeaker` — ek hi hook se teenon raaste:
  `VoiceEngine.speak()` (main chat + saare voice.speak),
  `CallAnnouncementReceiver`, `EventAnnounceReceiver`.
  Engine judte hi teenon apne aap neural ho jayenge.
- Sanitizer barkaraar (Phase 2 wala, ab `NeuralSpeaker.sanitize`
  par single-source; `VoiceEngine.sanitizeForSpeech` delegate karta
  hai) — emoji/markdown/URL kabhi nahi bola jayega.
- Announcement raaston me free-talk counting kabhi nahi hoti
  (engine aane par bhi wahi rahega).

## Kya BLOCKED hai (evidence ke saath) — native playback engine
Native engine = sherpa-onnx ka `OfflineTts` (Kokoro pipeline:
phonemizer + acoustic model + vocoder, ek suspending call).
Ye is build me **JUDA NAHI HAI**, wajah:
1. **Koi verified Gradle artifact nahi mila.** Official
   sherpa-onnx docs Android ke liye NDK source build batate
   hain (`build-android.sh` per ABI → `libsherpa-onnx-jni.so` +
   `libonnxruntime.so` → `jniLibs`), Maven Central artifact
   nahi. Ek third-party blog ne coordinate
   `com.github.k2-fsa:sherpa-onnx-android:1.10.0` (JitPack)
   claim kiya — single blog claim hai, official source se
   verify nahi hua, aur us version me Kokoro support hai ya
   nahi, ye bhi verify nahi hua. Unverified dependency build
   me daalna mana hai (galat coordinate = CI fail + phone
   par native crash ka risk).
2. **NDK source build Flora CI me nahi chalega.** Flora CI
   JDK-only `assembleDebug` hai; NDK step ke liye CI workflow
   badalna padega — workflow files API se chhooi nahi ja sakti
   (token me `workflows` scope nahi; sirf GitHub web UI).
3. **Raw ONNX Runtime route bhi guess par tikta hai:** usme
   espeak-ng phonemization + Kokoro model ka exact input/output
   spec chahiye — dono verify nahi hue. Guess karke ship karna
   mana hai.

Isliye: **cloud TTS par chupchaap kabhi nahi jaya gaya.**
Model ready hone par bhi aaj system TTS (Lily profile) hi
bolega. Engine judte hi `NeuralSpeaker.engine` set karna hi
kaafi hoga — koi caller dobara chhedna nahi padega.

## Gate (standing rule, barkaraar)
System TTS **is build me hataya NAHI gaya**. Hatana sirf tab
jab (a) neural sample user se approve ho (DONE 2026-10-29
~03:35 IST) AUR (b) phone test pass ho (Phase 10). Tab tak
Lily profile (pitch 0.95 / rate 0.88) har jagah fixed hai.

## Unblock karne ka raasta (Phase 9/10 follow-up)
1. Sherpa-onnx AAR coordinate official source (repo release ya
   Maven Central) se verify karo, ya NDK build ko CI me web UI
   se jodo (workflow edit) — jo pehle verify ho.
2. `OfflineTts` API (voice = hf_alpha, sid/language) phone par
   verify karo, sentence-chunking ke saath (lambi replies
   ek call me nahi — phone par RTF > 1 ho sakta hai).
3. `NeuralSpeaker.engine` ko us implementation se set karo.
4. Phone test: sample suno → user approve → tabhi system TTS
   hatane ka faisla (alag user order se).

## Honest limits
- Download me resume nahi hai — dobara try poori file phir
  se download karega (Wi-Fi recommended, UI me likha hai).
- Phone par inference speed abhi unknown hai (device par hi
  pata chalega) — isliye chunking + progress UX Phase 10 ke
  phone test ke baad tune hogi.
- Sample quality (hf_alpha ki awaaz kaisi lagegi) phone test
  par user hi judge karega — file-level verify se awaaz ka
  faisla nahi hota.
