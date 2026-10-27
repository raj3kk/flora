# Spec — App Shell (MainActivity + MayaApplication)

Source: behaviour only — videos (Home tabs, setup wizard), manifest
(launcher activity), screenshots (Home/Chat/Yaadein/Drawer). Koi creator
code, UI text, asset ya character copy nahi hoga; design dobara likha jayega.

## Files (honi chahiye)
- app/src/main/java/com/getmaya/android/MainActivity.kt (EXISTS, placeholder)
  Purpose: launcher entry; setup wizard ke baad Home dikhana; tabs Home /
  Scan / Mic / Yaadein / Chat; share intent milne par Chat route kholna;
  `open_route` extra ke 12 verified values ko sahi screen par bhejna
  (key_setup, license, my_pictures, notices, permissions, photos_of,
  scanner, settings_appearance, settings_autoreply, settings_faces_capture,
  settings_guardian, settings_voice_engine). "chat" aur "app_release" route
  nahi hain — unko route list me mat dalna.
- app/src/main/java/com/getmaya/android/MayaApplication.kt (EXISTS, placeholder)
  Purpose: Room, DataStore, WorkManager, foreground-service init; koi key
  ya secret init me nahi.
- app/src/main/AndroidManifest.xml (MISSING — build ke liye zaroori)
  Purpose: launcher, activities, services, receivers, providers, 47 me se
  sirf wo permissions jo module ko chahiye (sensitive permissions ka
  alag audit: WRITE_SECURE_SETTINGS sirf user ke ADB choice par, kabhi
  default/chupke nahi).
- app/build.gradle.kts (MISSING — build ke liye zaroori)
  Purpose: min SDK 26, target SDK 34, Compose + Material3, Room,
  DataStore, WorkManager, CameraX dependencies.
- app/src/main/res/ (EXISTS, EMPTY)
  Purpose: naya theme, strings, layouts — creator ke strings/assets copy nahi.

## Behaviour
- Fresh install: setup wizard → permissions (mic mandatory, baaki reason
  ke saath, "Aage badho" sirf mic ke baad) → Gemini key setup → Home.
- Har tab khulta hai, crash nahi; back/home/recents sahi kaam kare.
- Free mode banner: roz 10 min, ~15 tools (server se usage count).

## Acceptance (Verify → Test → Audit)
1. Module akela chalke test pass kare (unit + phone test).
2. Koi crash / hidden exception nahi.
3. Mic off / incognito / delete kaam kare.
4. Koi purani API key code me nahi hai.
5. Test log docs me darj hai; dexdump/manifest verification ke bina
   "done" claim nahi.
