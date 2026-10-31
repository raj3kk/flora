# Flora Part 2 — Update Channel 2D (K2 → K3 → K4 → K5, STRICT ORDER)

> Status: DRAFT ONLY. Kisi nayi service par UPLOAD nahi hua hai, koi
> bucket/policy nahi bani hai, version.json FILL nahi hua hai, koi
> workflow file API se push NAHI hui hai. Sab kuch D-01 host decision par
> GATED hai. Ye doc decision ke baad ke steps ko taiyaar rakhta hai.

## K2 — Host options comparison (decision ke liye, faisla nahi)

| Host | Fayde | Nuksaan / dhyaan rakhne wali baat |
|---|---|---|
| GitHub Releases (primary candidate) | Repo PUBLIC hone ke baad viable hai; release asset 2 GB/file tak (kai secondary sources se confirm — Flora APK ~107 MB isse bahut neeche hai, isliye size kisi bhi reading me rukawat nahi); LFS quota nahi khata; version ke saath release notes judte hain | Release banane ka tareeka (web UI vs CI step) D-01 ke saath tay hoga; APK ko git history me commit NAHI karna hai (100 MB git-file limit alag cheez hai) |
| Drive + Telegram | User ko jaana-pehchana raasta; SwissTransfer interim route jaisa hi feel | Link expiry + download cap (SwissTransfer interim par 250 downloads / 15 din jaisi seemayen thi); versioning + SHA ka haath se milaan chahiye; automation kamzor |
| R2 (Cloudflare) | Sasta object storage, public link stable | Naya account/bucket/policy setup + kharch ka faisla (spend approval alag se chahiye); Flora ke liye abhi koi R2 setup nahi hai |

- Interim sach (live verify, 2026-10-31): /download abhi bhi v2.22 build
  dikhata hai (SwissTransfer link, 13 Nov 2026 tak valid, SHA-256 page par
  likha hai, debug-signed note ke saath). Ye interim route hai, final
  channel nahi.
- Decision needed: D-01. Iske bina K3/K4/K5 aage nahi badhenge.

## K3 — Workflow-step DRAFT text only (web UI se lagane ke liye)

> YE SIRF DRAFT TEXT HAI — `.github/workflows/` me kuch push NAHI hua
> hai (API se workflow push BLOCKED hai: token ke paas workflows scope
> nahi hai). Ye step tab lagega jab D-01 + release APK (P2-07) dono hon,
> aur tab bhi GitHub web UI se Edit + paste + Commit se.

```yaml
# DRAFT — release APK ko chosen host par publish karne ka step (naam
# aur host D-01 ke baad final honge; yahan sirf shape darj hai)
# - name: Publish release APK
#   run: |
#     echo "Host + artifact path D-01/P2-07 ke baad yahan aayenge"
#     echo "SHA-256 nikaalo, version.json fill ke liye coordinator ko do"
#     echo "Koi keystore/password is step me kabhi nahi aayega"
```

- Dead CI upload step ka purana record: web UI se lagane par hi theek
  hoga — API se dobara ghumana mana hai.

## K4 — Fill procedure (BLOCKED: D-01 + P2-07 SHA)

1. P2-07 se REAL release APK + SHA-256 milne ke baad hi shuru karo.
2. `web/updates/version.json` me sirf ye bharo: `apkUrl` (chosen host ka
   seedha link) + `sha256` (release APK ka poora hash) + versionCode/
   versionName (release ke mutabiq).
3. Isi fill ke saath `notes` ka stale "private-repo" wala text bhi theek
   karo (wo note ab galat hai — repo public hai). Notes update akela
   kabhi nahi — fill ke saath hi.
4. Push ke baad: website par version.json padhkar verify karo (apkUrl +
   sha256 khaali nahi, versionCode installed versionCode se bada hai).
5. DEBUG build ka SHA kabhi fill nahi hoga — sirf REAL release APK ka.

## K5 — Live demo procedure (user-tap steps, auto kuch nahi)

1. User phone par purana version kholo → Settings/Update check dabao
   (khud se).
2. Naya version dikhe to user khud Download dabaye (auto-download kabhi
   nahi).
3. Download poora hone par SHA-256 ka milaan user khud kare (page par
   likhe SHA se).
4. User khud Install dabaye (auto-install kabhi nahi) → app khole →
   version dikhe.
5. Koi step fail ho to Catcher report + screenshot coordinator ko do;
   dobara auto-retry nahi.
