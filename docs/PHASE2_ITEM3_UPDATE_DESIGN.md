# Phase 2 — Item 3: In-app update, server se (design + pending steps)

**Channel (sirf maujooda free infra, koi paid hosting nahi):**

| Kya | Kahan | Status |
|---|---|---|
| Check file `version.json` | Repo `web/updates/version.json` → Vercel static (`flora-henna-alpha.vercel.app/updates/version.json`) | **LIVE hoga is push se** (repo push par Vercel auto-deploy; `models/manifest.json` wali pattern se verify hai) |
| APK download | Supabase Storage public bucket `flora-apk` (`flora-models` wali pattern) | **PENDING (neeche steps)** |
| Fallback | Website download page (`/download.html`, aaj SwissTransfer link) | Live hai |

**Kyun yehi channel:**

- GitHub releases NAHI: repo **private** hai — release assets bina
  auth ke download nahi hote; app me token embed kabhi nahi hoga.
- Vercel se APK NAHI: free-plan 50 MB limit (download page par
  likha hai), APK ~100 MB ka hai. Chhota `version.json` Vercel se
  bilkul theek hai.
- Supabase public bucket pattern LIVE prove hai:
  `flora-models/manifest.json` public URL se milta hai aur repo
  file se byte-barabar hai. `flora-apk/flora-latest.apk` aaj 400
  deta hai (bucket/object maujood nahi) — purana workflow step
  kabhi repo me nahi gaya (workflows-scope block, 2026-10-28 doc).

**App ka vyavhaar (`app/update/AppUpdate.kt` + Settings):**

- Sirf manual check — koi auto-check/download/install nahi.
- Compare key = `versionCode` (int). `version.json` ka number
  installed se bada ho tabhi "naya version".
- `apkUrl` khali ho to app website page par bhejega (koi jhootha
  download button nahi). `apkUrl` bhara ho to download → SHA-256
  verify (mismatch par file delete, install rok) → system install
  prompt (user ki "is app se install" permission se).
- Har failure fail-closed: message, kabhi crash/install nahi.

**Naya release publish karne ki prakriya (app badle bina):**

1. (Ek baar) Supabase dashboard/SQL se bucket `flora-apk` public
   banao + public-read policy (docs/FLORA_APK_DIRECT_DOWNLOAD.md
   me SQL hai). GitHub secrets me `SUPABASE_URL` +
   `SUPABASE_SERVICE_ROLE_KEY` rakho.
2. (Ek baar) `.github/workflows/android.yml` me Supabase upload
   step jodo — **ye step API se push NAHI hoga** (token ke paas
   `workflows` scope nahi hai); GitHub web UI se Edit + Commit
   karna hoga (2 minute, wahi purana tareeka).
3. Har release par: APK bucket me `flora-latest.apk` (upsert) +
   `web/updates/version.json` me naya `versionCode`, `apkUrl`,
   `sha256` (ek chhota web push). Uske baad purane app khud
   "naya version" dikhakar seedha download + install kara denge.

**Aaj ki haalat (imaandaari se):**

- Check machinery + `version.json` (versionCode 45 = maujooda tip)
  is push me hai; "Tum latest par ho" wala path turant kaam karega.
- Naye-version wala prompt + seedha APK download tabhi dikhega jab
  upar ke pending steps lagenge — isliye live end-to-end prompt
  abhi demonstrate NAHI hua hai (publish gate, Phase 3 se pehle).
- Signing: release APK unsigned hi rahega jab tak signing par alag
  faisla na ho; sideload identity debug-signed hi hai. Play Store
  update KABHI NAHI (purani key lost hai).
