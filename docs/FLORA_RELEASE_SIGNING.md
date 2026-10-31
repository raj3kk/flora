# Flora release signing (Item 5, 2026-10-31)

Nayi release key — purani key LOST hai, reuse KABHI NAHI hua hai aur hoga bhi nahi.
Flora hamesha sideload-only rahegi; Play Store update KABHI NAHI.

## Keystore (repo ke BAHAR — kabhi git/push me nahi aata)
- Path: `~/workspace/secure/flora-release.keystore` (perms 600)
- Alias: `flora-release`
- Type: PKCS12, RSA 2048, self-signed, validity 10,000 din
  (31 Oct 2026 se ~18 Mar 2054 tak)
- Password file: `~/workspace/secure/flora-release-keystore.pwd` (perms 600)
  — password ki VALUE is doc me, repo me, chat me, kisi report me KABHI NAHI hai.

## Build config
- `app/build.gradle.kts` me `signingConfigs.release` joda gaya hai jo keystore path
  aur password ko upar wali LOCAL files se padhta hai — koi value build file me
  hardcode nahi hai. Sirf `buildTypes.release` is config ko use karta hai;
  debug build bilkul untouched hai (CI aaj bhi sirf `assembleDebug` banata hai).
- Key files jis machine par maujood nahi hain (jaise CI runners), wahan release
  build sign nahi hoga — ye sahi behaviour hai, koi silent unsigned release nahi.

## Backup — USER KA KAAM (zaroori)
- User apni taraf se in dono files ki copy kisi SAFE jagah (apni machine / encrypted
  backup) par rakhe: `flora-release.keystore` + `flora-release-keystore.pwd`.
- Ye key kho gayi to dobara nayi key banani padegi aur saare installs ko phir se
  uninstall+reinstall karna hoga — isliye backup abhi karna hai, baad me nahi.
- Purane installs (debug-signed ya purani key wale) ko nayi signed release par
  jaane ke liye EK BAAR uninstall+reinstall karna hoga (signature mismatch).

## Is item ka scope (khatam) vs alag step
- Is item me sirf key + config hai. Asli thin RELEASE APK banana (K1) ek ALAG step
  hai — wo is item me nahi hua hai.
- Release install ka phone test bhi user phone par baaki hai (agent phone test
  nahi kar sakta).
