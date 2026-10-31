# Flora Part 2 — REAL Release APK Build Procedure (P2-07 / ZP-K1)

> Status: PROCEDURE + VERIFY ONLY. Is round me koi release APK nahi bana
> hai, koi keystore/password push nahi hua hai, koi secret value is doc
> me nahi hai. DEBUG build final publish NAHI hai.

## 1. Signing config verify (tip se, 2026-10-31)

- `app/build.gradle.kts` (tip, local se match): `signingConfigs.release`
  local files se padhta hai — koi key/password value file me hardcode
  NAHI hai.
- Release expectations (tip se): R8 minify + resource shrink ON;
  `arm64-v8a` akela ABI filter; versionCode 45.
- Keystore existence only verify hua: secure jagah par 2 files 600 perms
  par maujood hain (keystore + password file). Contents kabhi padhe nahi
  gaye, password kabhi print nahi hua, koi value is doc/report me nahi hai.
- Keytool entry-count check is round me NAHI chalaya gaya: uske liye
  keystore password chahiye hota hai, jo yahan enter nahi kiya gaya hai.
  Entry count user-attention session me, user ki maujoodgi me verify hoga.

## 2. VM memory check — local build STILL-BLOCKED

- `free -m` (is round me): total ~7934 MB, available ~853 MB.
- Rule: available ~1 GB se kam ho to local `assembleRelease` try mat karo.
- Natija: is round me local build TRY NAHI kiya gaya hai — still-blocked
  likha ja raha hai, fail hone ka natak nahi kiya gaya hai.
- Purane installs par note: key badli hai, isliye naye release par
  uninstall + reinstall hoga (upgrade-in-place purani key se nahi hoga).

## 3. Signing-flow decision (D-02) — teeno raaste, faisla batch me

- (a) CI signing: keystore GitHub Actions secrets me — user approval
  chahiye, third-party disclosure hai. Bina decision ke keystore GitHub
  secrets me KABHI mat daalo.
- (b) Local build: VM memory theek hone ke baad (abhi ~853 MB available
  — blocked).
- (c) User-attention session build: neeche §4 ke mutabiq.
- Koi raasta 2–3 baar fail ho to wahi route dobara mat ghumao — alternate
  raaste par jao, bina ruke.

## 4. User-attention session build procedure (bina CI secret disclosure ke)

1. User apne session me keystore/password ki maujoodgi confirm kare
   (password kabhi chat/log me nahi aayega).
2. Build machine par sirf us session me: clean checkout (fresh tip) →
   `assembleRelease` → artifact + SHA-256 + provenance note (kaunsi commit,
   kaunsa versionCode, kaunsa ABI).
3. Artifact ka ABI/native inspect karo (arm64-v8a hi, thin build).
4. SHA-256 ko 2D fill procedure (K4) ko do — DEBUG build ka SHA kabhi nahi.
5. Session khatam hone par build machine se keystore/password ka koi
   copy na rahe (local files apni secure jagah par hi rahenge, repo me
   kabhi nahi).
6. Is procedure ke baad bhi publish tabhi jab Parts 1–3 DONE-CODE +
   virtual-test pass hon (publish gate Part 4 ka hai — D-26 publish
   sign-off gate ka label yahan darj hai, koi decision yahan nahi liya
   gaya hai — yahan publish ka daava nahi).
