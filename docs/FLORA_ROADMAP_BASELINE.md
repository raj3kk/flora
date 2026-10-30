# Flora Roadmap — Phase 0 Baseline (31-Oct snapshot se current state tak)

**Roadmap source:** user-uploaded `Flora_Roadmap.docx` / `Flora_Roadmap.pdf` (dono me wahi ek roadmap hai, 31-Oct-2026 snapshot par based — `Flora-Full-Source-Audit-2026-10-31.md` par).
**Baseline check ka tareeqa:** GitHub API se live tip + tree (read-only), local working copy se compare, deploy/session records se cross-check. Koi secret/key/password/token ki value is document me nahi hai.
**Niyam:** Har item par state alag hai — code-complete / CI-green / deployed / phone-verified kabhi ek doosre se mila kar nahi kaha gaya hai.

## 1. Live tip (check ke waqt)

- Repo: `raj3kk/flora` (PRIVATE, branch sirf `main`)
- Tip SHA: `356956e619bfea2c8678b9e6092d335680013dac`
- Tip commit: "Fix CI #322/#323/#324: PicturesScreen Refresh onClick suspend call — scope.launch me lao" (2026-10-30T20:02:13Z)
- Tip tree: 464 entries, functions tip par **22** (local me 23 — farq sirf `send-push` hai, §3 dekho)
- Tip web: 14 files (13 pages + `models/manifest.json` + `updates/version.json` + `vercel.json`)
- Tip docs: 101 files; tip SQL: 6 canonical files (wahi 6 jo local me hain)
- Tip par `supabase/schema.sql` abhi bhi maujood hai — ye SUPERSEDED hai, is par "do not run" likha hai, kabhi mat chalao.
- Update channel file = `web/updates/version.json` (200, vCode 45, apkUrl + sha256 khaali — channel pending). Root `/version.json` ka 404 alag path hai, channel ka issue nahi hai.

## 2. CI status — snapshot ke baad ka sudhaar (important correction)

Fix run **41120079622** (commit 356956e6) **completed / success** hai. Iska matlab:

- **M3/M4, M11/M12, M5 — DONE-CODE** (code + CI green, fix run se). Purani fail runs #322/#323/#324 immutable hain, fix run se supersede hui hain.
- **M2 Reminders — DONE-CODE**: iska apna run 41118287092 (e93bd58a) bhi **completed / success** hai.
- **M7+M8, M10, provider lanes, production docs — DONE-CODE (supersede logic se)**: inke apne runs (26c8c637, d716f0d2, 1f156e00, 3821a3aa) pre-fix tree par fail hue the (PicturesScreen break cascade). Fix commit tip 3821a3aa se bana hai, isliye fix tree me ye saara code shaamil hai aur fix run green hai — wahi gate logic jo M3/M4/M5 par laga, in par bhi lagta hai. Phone proof in sab ka abhi bhi Phase 10 me baaki hai — koi phone-verified daava nahi.
- **M1 QR/barcode, brain flip (in-app primary, proxy fallback), pacing verified-step fix** — pehle se DONE-CODE (runs #316/#319 green).
- **M6 Marketing — SPEC ONLY** (vyavhaar unverified, build gated). **M9 Email — GATED** (Gmail OAuth K10).

## 3. Local-only inventory (43 files) — sab push karne layak NAHI hain

Local working copy me tip se 43 files zyada hain. Inka classification:

### 3a. PUSH KARO (Phase 1, is cycle me)
- `supabase/functions/send-push/index.ts` (8474 bytes, local working copy wala exact tested source — deploy session #2 me isi file se deploy hua tha). **Draft file `docs/fcm-pending/send-push-draft.ts` kabhi source nahi hai.**
- Is baseline doc + README update (docs-only).

### 3b. LOCAL HI RAKHO (jaankari ke liye, tip par nahi bhejni)
- Team cycle logs: `docs/FLORA_TEAM_APP_CYCLES.md`, `docs/FLORA_TEAM_SERVER_CYCLES.md` (local-only logs).
- Parent-only files: `docs/FLORA_REMAINING_LIST.md`, `docs/FLORA_DECISIONS.md` (parent ke paas rehte hain — ye list me nahi dikha to bhi mat chhedo).
- Phone test plan v15 (`docs/FLORA_PHONE_TEST_PLAN_v15.md`) — local-only hai, tip par nahi. Ye Phase 10 ka kaam ka document hai; is cycle me isko tip par bhejna zaroori nahi, par Phase 10 shuru hote hi iska tip par hona check karna.
- Baaki local-only specs/design docs (A3 preaudit, builder plan, G10 options, SD plan, vault plan, wagera) — ye design/spec documents hain, production source nahi; inko ek saath bulk me tip par dhakelna drift badhayega, isliye is cycle me nahi bheje gaye. Jo spec implement hogi, uska doc usi push ke saath jayega.

### 3c. KABHI PUSH MAT KARO (stale / draft / duplicate)
- `app/src/main/java/com/flora/app/automation/AppLauncher.kt` — STALE SPLIT: tip ke `VerifiedAppLauncher.kt` me `LaunchableApp` pehle se defined hai; is file ko push karne se duplicate definition ka compile break aayega. Isko push mat karo.
- `app/src/main/java/com/flora/app/planner/MemoryAppSearchIndex.kt` — STALE LEFTOVER: PlannerEngine me isko removed likha hai (store-scan-only decision). Isko push mat karo.
- Drafts: `docs/admin-approve-draft.ts`, `docs/fcm-pending/*-draft.ts`, `docs/usage-count-fixed-draft.ts`, `docs/admin-phase-a-tables.sql` — ye kabhi deploy/push source nahi hain.
- `web/admin-draft.html` — local-only draft page, tip ki live admin page se alag hai; isko push mat karo.
- `local.properties` — kabhi repo me nahi jana chahiye (isme local sdk path hota hai).

### 3d. TIP PAR HAIN, LOCAL ME NAHI (local copy thodi stale hai)
- `docs/FLORA_B2_TRADEOFF.md`, `docs/FLORA_DEEP_AUDIT_20261027.md`, `docs/FLORA_PRODUCTION_READY_PLAN.md` — ye 3 docs tip par hain, local copy me nahi. Push helper sirf di gayi files add karta hai, delete nahi karta, isliye is cycle ke push inko chhedenge nahi. Local copy ko in 3 files se sync karna ek alag safe step hai (is cycle me nahi kiya gaya).

## 4. Defect register C-01..C-15 — current status se reconcile

| ID | Snapshot me | Ab ki sthiti (evidence ke saath) |
| --- | --- | --- |
| C-01 | Signing key LOST, release unsigned | **OPEN — DECISION PENDING (stop point).** Standing: sideload-only, unsigned release hi current raasta hai. Naya keystore / signed release user decision ke bina nahi banega. |
| C-02 | Free matrix contradiction (narrative all-tools vs seed talk+builder) | **CODE-FIX IS CYCLE ME (Phase 5)** — standing rule = Free: saare tools + 600 talk sec/day. Seed, licence-verify fallback aur admin-plan-features defaults — teeno ko isi rule se align kiya ja raha hai (alag commit). Live DB row ka UPDATE (ON CONFLICT DO NOTHING ki wajah se file change live row nahi badlega) aur licence-verify ka dashboard redeploy — parent-browser step, is report ke decision list me hai. Website/pricing pehle se rule ke saath hain ("saare tools"). |
| C-03 | send-push local-only, tip par nahi | **CODE-FIX IS CYCLE ME (Phase 1)** — exact 8474-byte file tip par commit ho rahi hai. Deployed to pehle se hai (deploy session #2); FCM live E2E delivery abhi bhi phone-gated hai. |
| C-04 | version.json apkUrl/sha256 khaali; flora-apk 50 MB cap vs ~112 MB APK | **OPEN — DECISION PENDING (stop point).** Channel file `/updates/version.json` hai, vCode 45 par hai. Hosting choice (GitHub Releases asset route ek option hai) user decision ke bina nahi hogi. |
| C-05 | Admin UID ki users row nahi | **OPEN — PHONE STEP PENDING.** Live check 2026-10-31 ~01:39 IST: `public.users` me 5 rows (28 Oct ki, sab is_admin=false), admin account ki row abhi bhi nahi hai. Fake device_id kabhi nahi, kisi aur ki row ko admin kabhi nahi. Admin tabhi banega jab app-link se asli row banegi. |
| C-06 | Live RLS UNVERIFIED | **OPEN — READ-ONLY INSPECTION PENDING.** SQL files me RLS ON + zero policies ka posture hai (service-role-only design). Live pg_policies check dashboard/browser se hona baaki hai — koi destructive SQL nahi chalaya gaya hai. |
| C-07 | Deepgram security step pending; STT/TTS success-path unverified | **OPEN — USER INPUT PENDING.** Deepgram security code user bhejega; uske baad hi asli JWT se success-path test hoga. 401 sirf gate prove karta hai. |
| C-08 | stt-proxy pending; gmail-proxy 501 draft | **STT: DONE-DEPLOYED** (deploy session #2 me naya create+deploy, gate 401 verified, ab 404 nahi). **Gmail: GATED, disabled hi rahega** (OAuth K10 decision pending — is cycle me deploy nahi hua, kabhi nahi hoga jab tak decision na ho). |
| C-09 | README stale Maya skeleton; project name maya-rebuild | **PARTIAL FIX IS CYCLE ME (Phase 1)** — README ko current Flora architecture par update kiya ja raha hai. `settings.gradle.kts` me `rootProject.name = "maya-rebuild"` ko blindly nahi badla gaya hai (build files me naam badalna CI/artifacts ko chhoo sakta hai; ye alag, verify karke kiya jayega). |
| C-10 | Local 23 vs tip 22 functions; local-only files/docs | **PARTIAL FIX IS CYCLE ME** — send-push commit ke baad functions tip par 23 ho jayenge. Baaki local-only files §3b/§3c ke hisaab se handle hui hain (sab push nahi hui hain, jaanboojh kar). |
| C-11 | Live schema vs SQL: usage.day TEXT vs DATE; device_id nullability | **PARTLY RESOLVED, VERIFY PENDING.** usage.day live DATE hai — isliye usage SQL me `p_day::date` cast ke saath fix ho chuka hai aur deploy session #2 me RPC live ban gaya hai. device_id nullability ka live confirm abhi bhi dashboard check par baaki hai. |
| C-12 | provider_keys plaintext at rest | **OPEN — DECISION PENDING (stop point).** Migration (encrypt / managed secrets) user decision ke baad hi hogi. Is cycle me koi key value kahin likhi, log ki ya badli nahi gayi hai. |
| C-13 | Storage buckets public, zero policies; no upload path in code | **OPEN — DOCUMENTED, NO CHANGE.** Public read intended assets ke liye hai; anonymous write kabhi enable nahi hogi. Code me upload path nahi hai, isliye policy badalne ka kaam tabhi jab upload feature aaye. |
| C-14 | STT seconds Free usage counter me shamil hain ya nahi — unclear | **OPEN — DECISION PENDING (stop point).** usage-count talk_seconds + tool_calls ginata hai; STT seconds ko talk time me ginne ki definition user decision ke bina nahi badli gayi hai. |
| C-15 | Release size/signing/ABI not fully validated | **OPEN — PHONE/BUILD GATED.** Thin RELEASE APK abhi bhi pending hai (unsigned hi banega jab banega); size ka vaada nahi kiya gaya hai. |

## 5. Deploy session #2 — dobara mat karo (already complete)

2026-10-31 ~01:43 IST complete: corrected usage SQL + increment_usage RPC live; 13 functions (10 paste+deploy, admin-approve + admin-list live se exact match isliye unchanged, stt-proxy naya create+deploy, send-push local working copy se); gates 401 verified; gmail-proxy kabhi deploy nahi hua. Is cycle me koi function dobara deploy nahi kiya gaya hai.

## 6. Brain / voice / product rules (is cycle me ulte nahi gaye)

- Brain: app→providers direct PRIMARY, server brain-proxy sirf FALLBACK.
- Voice everywhere: Sample 2 Lily (system-TTS profile); Kokoro Gate 6 HELD.
- Licence: ₹199/month FINAL; Free = 600 talk sec/day + saare tools; UTR exactly 12-digit numeric; payment auto-confirm kabhi nahi.
- Clean-room PERMANENT: creator code/UI/assets/trademark kabhi copy nahi; wholesale Maya copy REFUSED hai jab tak mazboot ownership proof/source na mile (abhi pending hai).
- Excluded (snapshot §13 se match): payment auto-confirm, silent auto-answer/reply, SIM-call TTS injection, WhatsApp call API, hidden accessibility actions, self-granting privileged permissions, Termux bridge, web password autofill/typing, creator voice cloning, old creator credentials/assets reuse — ye sab is cycle me bhi excluded hi rahe hain.
