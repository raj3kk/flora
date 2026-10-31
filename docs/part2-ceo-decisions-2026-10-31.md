# Part 2 — CEO Decisions Batch 2026-10-31: implementation + evidence

Source of decisions: `Flora-CEO-Decisions-Batch-2026-10-31.md` (D-01–D-32 + naye flags).
Is cycle ka scope sirf Part 2 hai. Part 1 / Part 3 ki files yahan chhedi nahi gayi hain.
Koi secret/key/password value is doc me nahi hai — sirf secret NAMES + set/not-set status.
Koi code change bina CI green ke DONE-CODE nahi kehlata; live deploy claims is doc se nahi karne.

## 1. D-01 + P2-06 — update channel = GitHub Releases (docs/design hissa)

- Repo PUBLIC hai (github.com/raj3kk/flora), isliye Releases 2 GiB/file viable hai.
- Channel maujood hai: maujooda CI workflow (`.github/workflows/android.yml`, tip 256b70a8)
  debug builds ko `debug-build-<run_number>` tag par prerelease asset ke taur par publish karta hai.
- Release channel ke liye naya workflow `.github/workflows/release.yml` banaya gaya hai
  (workflow_dispatch ONLY — har push par release-signing nahi chalegi):
  tag pattern `release-build-<run_number>` (debug pattern se alag, pehchanne layak).
- Prerelease flag faisla: release asset `--prerelease` ke saath publish hoga jab tak
  user ka phone sign-off (D-26) nahi milta — asset download/verify tab bhi ho jayega,
  par "stable release" ka daava sign-off se pehle nahi hoga. Sign-off ke baad wali
  release ko full release banaya jayega (ye step parent/koordinator tay karega).
- K4 fill + K3 step sirf item 10 ke sequence me honge (REAL release SHA verify ke BAAD) —
  is doc me ya is cycle me DEBUG SHA se koi fill nahi hua hai.

## 2. D-02 + P2-07 — CI signing + release workflow

### Signing config (code se verify, tip 256b70a8)

- `app/build.gradle.kts` me `signingConfigs.release`: storeFile = workspace/secure me
  keystore, keyAlias `flora-release`, aur EK hi single-line password file se
  `storePassword` aur `keyPassword` dono set hote hain (file format: single line,
  koi `=`/`:` separator nahi — isliye store vs key password alag nahi hain).
- CI runner par `$HOME` = `/home/runner` hota hai, isliye workflow me keystore +
  password ko `$HOME/workspace/secure/` me likhna hoga (wahi path jo build file padhti hai),
  600 perms ke saath, build ke baad step khatam hone par runner discard ho jata hai.
- Local build route nahi (VM memory env-limit) — CI signing hi route hai (D-02 (a)).

### Secrets (API se set — status sirf)

- Secret names: `FLORA_RELEASE_KEYSTORE_B64` (keystore ka base64 form, secret string
  hoti hai isliye encode zaroori tha) aur `FLORA_RELEASE_KEYSTORE_PASSWORD`.
- Dono secrets GitHub Actions secrets me sealed-box encryption ke saath set ho gaye
  (API response 201 dono par). Values kabhi repo/chat/log/state file me nahi gayi hain
  aur set hone ke baad GitHub se readable nahi hain.
- Release workflow in secrets ko padh kar local files banata hai:
  `FLORA_RELEASE_KEYSTORE_B64` -> base64 decode -> keystore file;
  `FLORA_RELEASE_KEYSTORE_PASSWORD` -> password file; phir `:app:assembleRelease`.

### Workflow push — status

- Workflow files (`.github/workflows/`) GitHub API se push karne par token ke
  `workflows` scope ki kami se fail ho sakte hain (pehle ka lesson) — is cycle me
  workflow commit ka EK API attempt hoga; fail ho to ye item PARENT-BROWSER-STEP
  (web-UI se file create/edit + secrets verify) darj hoga, loop nahi chalega.
- Release workflow ka content (ready for web-UI, agar API fail ho):
  name `Flora Release Build`; on: workflow_dispatch only; permissions contents: write;
  job ubuntu-latest: checkout, JDK 17 (temurin), secrets se keystore+password files
  banana (600 perms), `:app:assembleRelease` (gradle 8.7), phir
  `gh release create "release-build-<run_number>" --prerelease --title ... app/build/outputs/apk/release/*.apk`.
  Test job ka content §9 me hai.

## 3. OPEN-12 per D-05 (principles decided, detail spec-first)

### S1-5 — 15-day rule: confirmation-gated + admin-panel-only

- `supabase/functions/licence-verify/index.ts` (tip): pro licence ek waqt par sirf EK
  device par; naye device par 15 din poore hone se pehle `device_locked: true` +
  `device_change_available_at` milta hai, transfer tabhi jab 15 din poore hon.
  Admin bypass ka zikr `admin-licences` me hai (15-day wait ka admin bypass) —
  matlab change admin-panel route se hi hota hai, app se auto-transfer nahi hai.
  Is cycle me is behavior me koi change nahi.

### S1-6 — gate sirf padhta hai, increment kabhi nahi

- Grep-verify (tip): `brain-proxy` v5 gate, `tts-proxy` v3 gate aur `stt-proxy`
  licenceCheck — teeno sirf `usage.talk_seconds` PADHTE hain, koi increment nahi karte.
- Increment sirf ek jagah se hota hai: `usage-count` function ka `increment_usage` RPC
  (atomic, `supabase/sql/usage_atomic_increment.sql` se deploy hota hai) aur `stt-proxy`
  ka successful-transcript counting path (wahi RPC, §6 dekho). Koi gate increment nahi karta.

### S1-17 — Deepgram

- `stt-proxy`: Deepgram listen, model `nova-2`, language hi/en, server-side key ONLY
  (env `DEEPGRAM_API_KEY` pehle, else `provider_keys` DB row `deepgram`).
- `tts-proxy`: Deepgram speak, model `aura-asteria-en`, text cap 1000 chars.
- Dono me key unset ho to 503 (jhootha transcript/audio kabhi nahi); provider fail par 502.

### S1-27 — NO ALTER, live schema hi truth

- Live schema: `device_id TEXT NOT NULL` (core_tables.sql ka live truth).
- Is cycle me `core_tables.sql` push NAHI hua, koi schema ALTER NAHI hua, aur kabhi nahi hoga.
  `supabase/schema.sql` khud kehta hai wo SUPERSEDED hai — deploy me mat chalao.

### S1-30 — server-side rate-limit state, app-side P3-03 se align

- Design (spec-first, is cycle me naya server behavior nahi): rate-limit state
  server-side rahegi (30/60s window, 5 fail/15 min -> 15 min block — app-side
  `DeviceRateLimit.kt` ke saath align). Server-side state ka table/function abhi
  repo me is naam se maujood nahi hai (grep me sirf app-side `DeviceRateLimit` +
  alag `whatsapp-template-send` ka 30 sends/24h limit mila) — isliye is cycle me
  sirf design darj hai, implement Part 4 review ke baad hoga, live deploy to
  waise bhi parent browser step hai. Koi number invent nahi hua (30/60s, 5/15min
  decisions doc + app-side code se liye gaye hain).

### S2-3 — unlinked path jaisa hai waisa

- `usage-count` v2: unlinked path me body ka `user_id` KABHI trust nahi hota —
  `device_id` se `users` row dhoondhi jaati hai, mili to us owner par increment,
  na mili to `{ counted: false }`, koi row nahi banti, kisi user_id par write nahi hota.
  Unlinked = koi row claim nahi. Koi proof invent nahi hua, koi change nahi.

### S3-2 — 1 phone per licence

- `licence-verify` v2 logic se verify: pro licence ek waqt par sirf ek device
  (NULL-claim pehli verify par claim karta hai; match par active; mismatch + 15 din
  se pehle locked; 15 din poore par transfer). Free plan par koi device lock nahi.
  Koi change nahi.

### S3-3/S3-4 — CORS sirf Vercel project domains, koi wildcard nahi

- CURRENT STATE (gap, chhupaya nahi — scope correction, 2026-10-31 tip grep se
  verify): tip par sirf 5 functions nahi, POORE 25/25 functions me
  `Access-Control-Allow-Origin: "*"` hai — `admin-approve`, `admin-flags`,
  `admin-instructions`, `admin-keys`, `admin-licences`, `admin-list`,
  `admin-overview`, `admin-payments`, `admin-plan-features`, `admin-settings`,
  `brain-proxy`, `feature-flags`, `gmail-proxy`, `heartbeat`, `home-sync`,
  `licence-key`, `licence-verify`, `public-settings`, `push-token-sync`,
  `send-push`, `stt-proxy`, `tts-proxy`, `usage-count`, `user-data-delete`,
  `whatsapp-template-send`. Allowlist migration ka scope isi poore 25-function
  set par hai — pehle wali 5-function list scope ko understated karti thi, isliye
  use is poori list se correct kiya gaya hai. `tts-proxy` ka comment khud kehta
  hai S3-4 allowlist HELD hai (host list parent se aane par badlega).
- CEO ITEMS (is doc-fix cycle se darj, koi behavior change nahi):
  - (a) `user-data-delete` bhi `*` ke saath aaya hai — ye maujooda pattern-consistent
    hai, par decided S3-3/S3-4 principle (sirf Vercel domains) ke against hai.
    CEO ko explicitly faisla darj karna hai: is function ko grandfather karo ya
    allowlist migration me include karo — bhoolna nahi hai.
  - (b) P3-04 `provider_keys` contradiction — `provider_keys` GLOBAL table hai
    (primary key `provider`, koi `user_id` column nahi), isliye user-data-delete
    use delete nahi karta. CEO decision pending hai: decisions list se
    `provider_keys` hatao ya alag admin-only global purge route banao; tab tak
    function ka use retained rakhna sahi hai (detail §4 FLAG me hai).
- Is cycle me wildcard ko code me NAHI badla gaya hai, kyunki sahi domain list
  parent browser session se verify honi hai (galat allowlist panel/website tod degi)
  aur live deploy waise bhi parent step hai. Domain list PROPOSAL (verify karni hai):
  - `https://flora-henna-alpha.vercel.app` (website ka Vercel project domain;
    `web/updates/version.json` me `pageUrl` isi domain ka `/download.html` hai)
  - Vercel-assigned hosts: jo bhi Vercel project dashboard me is project ke liye
    listed hon (preview/alias domains) — parent browser session se list confirm kare,
    tabhi allowlist final hogi.
  - Koi `*` nahi, koi wildcard subdomain nahi, koi custom domain tab tak nahi jab tak
    dashboard me confirm na ho.
- Implement step (parent browser step ke saath): allowlist confirm hone ke baad har
  function me `*` ki jagah request Origin ko allowlist se match karke wahi Origin
  wapas bhejna (exact-match, regex/wildcard nahi), phir functions redeploy.

### S4-3/S6-1/S6-2 — SPEC-FIRST, fail-closed defaults

- Is cycle me sirf spec/design: defaults fail-closed (DB error par gate ka
  fail-open note alag hai — gates me DB error par allow hai, jo pehle se documented
  hai; S6-2 ka final fail-closed faisla Part 4 review ke baad implement hoga).
  Koi behavior change is cycle me nahi hua.

## 4. P3-04 server half — user-data delete function (design + code, deploy parent step)

- Nayi function: `supabase/functions/user-data-delete/index.ts` (maujooda functions ke
  pattern par: Deno.serve, CORS preflight, anon-client JWT check).
- Flow: JWT nahi -> 401; body `user_id` JWT user se match nahi -> 403;
  `confirm: true` na ho -> 400 `confirm_required` (reject, kuch delete nahi);
  phir service-role se per-user operational rows delete:
  `push_tokens`, `usage`, `settings`, `home_boxes`, `heartbeats`
  (sab me `user_id` column code/SQL se verify hai).
- RETAINED (audit/financial record): `licences`, payment rows, `admin_audit`.
  Anonymization sirf tab jab legally safe ho — design me ye note hai, silently
  anonymize nahi kiya gaya. Delete ke baad `admin_audit` me ek note row insert hoti hai
  (action `user_data_delete_self`, target = user, after_json me sirf table names +
  retained note — koi personal data/value audit-json me nahi).
- FLAG — decision-implementation contradiction (silently reinterpret nahi kiya gaya,
  parent/CEO resolve karega): decisions doc ki P3-04 list me `provider_keys` bhi
  hai, par `provider_keys` table GLOBAL hai (primary key `provider`, koi `user_id`
  column nahi — `supabase/sql/admin_panel_tables.sql` se verify). Kisi ek user ke
  delete par poori `provider_keys` table delete karna saare users ka panel-key
  route tod dega, isliye is function me `provider_keys` delete NAHI ki gayi hai.
  CEO faisla chahiye: ya to list se `provider_keys` hatao (global keys user-data
  nahi hain), ya alag admin-only global purge ka rasta banao. Tab tak retained.
- LIVE DEPLOY (parent browser step — exact steps report me bhi hain):
  Supabase dashboard -> Edge Functions -> New function `user-data-delete` ->
  is file ka content paste -> Deploy. Koi SQL is function ke liye nahi chahiye
  (tables pehle se maujood hain). Verify: fresh user JWT se
  POST `{ user_id, confirm: false }` -> 400; `{ user_id, confirm: true }` ->
  200 + deleted tables; dobara same call -> 200, deleted counts 0 (idempotent
  delete, koi error nahi); galat user_id -> 403; bina JWT -> 401.

## 5. Provider_keys route (D-21) — admin panel ENCRYPTED verify/fix

- GAP MILA (chhupaya nahi): tip par `admin-keys` `set` action `key_value` ko
  PLAINTEXT store karta tha (`provider_keys.key_value` me seedha), aur
  `admin_panel_tables.sql` ka security note khud kehta tha values at-rest
  encrypted NAHI hain (sirf RLS + no-policy + no-logging protection tha).
  No-logging wala hissa sahi tha: key value kabhi response me nahi, kabhi
  audit-json me nahi, sirf provider + present/missing.
- FIX (is cycle, code — deploy parent browser step):
  - `admin-keys` set: naya Edge secret `PROVIDER_KEYS_ENCRYPTION_KEY` (dashboard
    secret, parent browser step se set hoga — value kabhi chat/repo me nahi)
    se AES-GCM encrypt karke store hota hai, format `enc:v1:<base64(iv+ciphertext)>`,
    key = SHA-256(env string). Env secret na ho to set 503
    `encryption_not_configured` se fail-closed hoga (plaintext write kabhi nahi).
  - `status`: sirf presence booleans pehle jaise, PLUS `providers_encrypted`
    boolean map (format batata hai, value kabhi nahi) — migration verify ke liye.
  - Resolvers (`brain-proxy`, `tts-proxy`, `stt-proxy`): `enc:v1:` prefix wali
    value ko decrypt karke use karenge; env secret na ho to empty (503 path);
    legacy plaintext rows migration ke dauran waise hi padhi jayengi (nahi to
    deploy ke turant baad maujooda DB fallback toot jayega) — migration step:
    encryption secret set + functions deploy ke baad admin panel se har provider
    ki key dobara set kare (set overwrite karta hai, nayi row encrypted hogi),
    phir status me sab `encrypted: true` verify kare. Uske baad legacy passthrough
    hatane ka alag cleanup hoga (is cycle me nahi, taaki migration tootne na paye).
  - Koi key invent nahi hui, koi key chat me nahi maangi gayi. Notion/ElevenLabs
    tokens user admin panel se daalega jab ready hon; Deepgram Edge secret pehle
    se set hai (env-first resolution ki wajah se DB fallback ki zaroorat nahi).

## 6. D-10 — S1-20 cap 600s/day (Free) + per-call 120s cap: verify

- VERIFY (code, tip):
  - `usage-count` khud cap enforce nahi karta (uska comment ye saaf kehta hai) —
    wo counter hai; enforcement gates me hai.
  - `brain-proxy` v5 gate + `tts-proxy` v3 gate: Free -> aaj ka `usage.talk_seconds`
    < 600 ho to allow, nahi to 429 `free_limit`. Gate sirf padhta hai.
  - `stt-proxy` licenceCheck: wahi < 600 gate (429), PLUS counting path:
    successful transcript par Deepgram `metadata.duration` se
    `counted = min(ceil(duration), 120)` — per-call cap 120s barkaraar hai,
    sirf Free count hota hai, sirf successful non-empty transcript par,
    UTC-day boundary wahi jo gate + usage-count use karte hain.
  - Plans seed (`supabase/schema.sql`) me bhi Free `talk_seconds_per_day: 600` hai.
- Gap nahi mila — isliye D-10 par koi code fix nahi hua, evidence ke saath verify darj hai.
  Live 429 probe (Free <600s -> 429 jaisa ZP-F12) parent/phone live step hai, code se nahi.

## 7. D-14 — scoped policy + plans backup me NAHI: verify

- `flora-models` PUBLIC exception: repo me storage bucket/policy code is cycle ke
  scope me nahi mila (koi upload path code me nahi hai, sirf DriveClient ka zikr
  plan me hai) — exception "sirf model downloads ke liye, documented by design"
  ke taur par barkaraar hai; is cycle me koi policy file banayi ya badli nahi gayi.
- Backup scope (`FloraBackupManager.kt`, tip): VERIFY —
  - Settings: EXACT 14 named keys (avatar, persona, ageConfirmed, voiceSample, theme,
    wakeEnabled, wakeThreshold, voiceMatchThreshold, callAnnounce, eventAnnounce,
    drivingMode, callTalk, continuousMic, voiceView) — iske bahar koi settings key nahi.
  - Secrets: sirf encrypted blob ke andar (PIN-blob GCM encrypt; app-link refresh +
    builder tokens + API keys).
  - OUT list me saaf hai: licence key slot + payment/UTR slots, quota counters,
    usage counters kabhi backup nahi hote; restore ke aakhir me mandatory
    app-link re-sync + LicenceClient re-verify (server hi sach hai).
  - Isliye plans/licence state backup me nahi hai — koi fix nahi hua. App backup
    code is item ke liye chhedna allowed tha, par zaroorat nahi padi; doosre parts
    ka behavior bilkul nahi badla gaya.

## 8. D-15 — K7 comment: is cycle ke aakhri push me ride kiya (solo push nahi)

- NOTE (imaandaar): pehli do server pushes (user-data-delete, provider_keys fix)
  me K7 comment nahi tha — wo is cycle ke aakhri push (K4 fill + K3 step) me
  `supabase/functions/feature-flags/index.ts` ke header comment ke taur par
  joda gaya hai, jo khud ek server file hai. K7 kabhi SOLO push nahi hua —
  hamesha kisi doosre kaam ke push ke saath ride kiya, jo D-15 ka maksad hai.
- Clarification ka content: feature-flags ka CORS block sirf isi function par
  lagta hai; S3-3/S3-4 allowlist faisla alag se Part 2 doc §3 me darj hai.
  Sirf comment hai, behavior change nahi.
  (Note: `supabase/sql/usage_atomic_increment.sql` + function comments me K7 ka
  purana zikr maujood hai — wo is cycle me badla nahi gaya.)

## 9. CI test-job — content (workflow push ke saath; fail ho to parent web-UI)

- Naya job `unit-tests` (maujooda `build` job ko chhede bina, uske saath chalega):
  runs-on ubuntu-latest, checkout + JDK 17 (temurin) + gradle 8.7, arguments
  `:app:testDebugUnitTest --tests "com.flora.app.data.CatcherEventsTest" --tests "com.flora.app.data.WatchlistStoreTest"`.
  Note: maujooda `build` job me pehle se "Run unit tests" step hai (poora
  `:app:testDebugUnitTest`), isliye ye job Part 1 team ke do tests ka focused
  gate hai, debug build job ka duplicate nahi.
- ISSUE-5 local execution: is cycle me local run NAHI hua — snapshot me `gradlew`
  nahi hai aur VM par sirf ~480 MB free memory thi (gradle run VM ko maar sakta
  tha). Task ke mutabiq CI job hi verification hai, blocker nahi; retry loop nahi chalaya gaya.
- CI me tests fail hon to fix tester ke baad fixer round me honge.

## 10. SEQUENCE — status (todna nahi)

1. Secrets + workflow: secrets SET (API 201, dono) — workflow file ka EK API attempt
   hoga; fail ho to parent web-UI step, uske baad hi release build trigger ho sakta hai.
2. Release build (CI, workflow_dispatch): workflow live hone ke BAAD hi trigger hoga
   (API dispatch attempt, fail ho to parent web-UI trigger) — is cycle me abhi GATED.
3. APK download + verify (aapt2 badging: com.flora.app, versionCode, versionName;
   sha256): GATED (build ke baad).
4. TABHI K4 fill (`web/updates/version.json` me apkUrl + sha256 — sirf REAL release
   SHA se, DEBUG SHA se KABHI NAHI) + `/download` update: GATED. Is cycle me
   version.json/download.html ko chheda NAHI gaya hai (purane notes abhi bhi
   maujood hain: private-repo wala note + 50 MB clause + Swiss v2.22 link —
   ye sab tabhi theek honge jab real SHA mile; pehle badalna galat hota).
5. TABHI K3 step (workflow me update-upload step ka draft/fill, real SHA par
   depend) + Swiss nayi upload ki TAIYAARI: GATED. Swiss upload KHUD parent
   browser step hai (nayi upload, extend nahi; sirf CI-green REAL build ki;
   current link 13 Nov 2026 tak).
6. Uske baad: Part 4 validation -> user phone test -> final re-audit (sequence
   decisions doc §Sequence ke mutabiq).

## 10b. SEQUENCE UPDATE — real release build mil gaya (is cycle me hi)

- Release CI run 41211883993 completed/success (workflow_dispatch, head 175f0483):
  signing-restore step success, assembleRelease success, publish success.
- Published asset: tag `release-build-1` (prerelease, D-26 sign-off tak),
  file `app-release.apk`, size 72,571,820 bytes.
- Download verify (VM par, APK run nahi — sirf file verify):
  aapt2 badging: package `com.flora.app`, versionCode 45, versionName
  `2.22.0-rebuild`, application-debuggable flag NAHI (release variant);
  SHA-256: `8dead87873d7e539ecff1685a312ecd63cad3964a0a1c3cbf7d260a535166602`.
  Apksigner verify VM par nahi chal paya (java VM par nahi mila) — ye APK ka
  fail nahi hai, CI release-signing step + non-debuggable badging hi evidence hai.
- TABHI K4 fill hua: `web/updates/version.json` me apkUrl (release-build-1 asset)
  + upar wala REAL release SHA (DEBUG SHA se kabhi nahi) + notes se private-repo
  note aur 50 MB clause hataya gaya (D-01 Releases route se superseded).
  `/download.html` bhi update hua: naya size/SHA/link, release-signed note,
  Swiss v2.22 link superseded note.
- TABHI K3 step: `release.yml` me "Record update metadata (K3)" step joda gaya
  (publish ke baad SHA + download URL run summary me; koi commit/push nahi karta).
  K3 ka asli update-upload step Publish release APK hi hai (tag release-build-1).
- Swiss nayi upload ki TAIYAARI: asset + procedure ready hai (nayi upload, extend
  nahi, sirf CI-green REAL build ki — ye build wahi hai), par Swiss upload KHUD
  parent browser step hai, is cycle me upload nahi hua.
- Agla trigger: Part 4 validation (is release build par) -> user phone test +
  sign-off (D-26) -> tabhi full-release flag + final re-audit.

## Out-of-scope flags

- D-27 Home server-half (SD-P5 LAST): is cycle me nahi chheda gaya.
- `core_tables.sql` push / schema ALTER: kabhi nahi (S1-27).
- Dashboard changes (Supabase/Vercel), Swiss upload khud: parent browser steps.
- Paid gateway: koi spend nahi; UPI+UTR hi rahega, Razorpay NOT configured.
