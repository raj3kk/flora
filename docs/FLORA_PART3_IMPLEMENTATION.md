# Flora Part 3 — Implementation (P3-01–P3-17)

**Scope note (binding):** Ye Part 3 hai — Flora ki APNI zaroorat (NEW).
Inko "Maya parity" kabhi mat likhna. Production-ready declaration
kabhi nahi. Final Validation Flow Part 4 me hai (koi "Phase 10"
naam ka phase nahi hai). Catcher rules barkaraar: auto-upload
KABHI NAHI, copy/share/save sirf user-tap par, sanitizer masks
(UTR last-4 only, licence-key shape, tokens/emails/URL query
strings, key/token/password/secret/otp values kabhi nahi),
chat full text kabhi report me nahi, Auto Log me action lines hi.

## P3-01 — Catcher full coverage (3A, PEHLE)

Covered pehle se: crash, BrainClient fail, plan-step fail, system
STT/TTS fail, FCM token-sync fail, Auto Log tail, snapshot,
licence state, user note.

Is push me naya:

- `data/CatcherHooks.kt` — per-surface hooks, sab never-throw:
  DB/storage errors, network history (safe code, URL kabhi nahi),
  lanes (deepgram/kokoro/wakeword/mic/system-stt/system-tts),
  features (builder/browser/routines/reminders/triggers/markets/
  docs/pictures/driving/typing/keys/search/home-sync/whatsapp-reply),
  snapshot se bahar permission denials, licence-record-only,
  server-surfaced (BrainClient/API hooks se hi, raw body kabhi nahi).
- Rotation fix (`data/CatcherEvents.kt`): 100KB paar hone par
  aadha-drop HATAYA gaya — poori file `catcher_events.prev.log`
  me archive hoti hai + ROTATION marker line aati hai. Naya event
  kabhi nahi khota (archive fail par bhi append hota hai).
- Init-health (`data/CatcherHealth.kt` + `FloraApplication.kt`
  wiring): init fail ab silent no-op nahi — probe ka natija
  `lastProbe` me rehta hai, Catcher screen use dikhata hai
  (khaali screen ki jagah). Screen par probe line dikhane ki
  wiring fixer/tester ke paas hai (call-site list neeche).
- FCM delivery failure: on-device se capture NAHI ho sakta by
  design — isko implement karne ki koshish nahi ki gayi. Sirf
  panel-side evidence (ZP-F13) ka note `fcmDeliveryNote()` me hai.
- Licence-client/gate errors: SIRF record — licence behaviour
  me koi badlav nahi (Round 3 domain deliberately untouched tha).

Call-site wiring (fixer ke liye, ek-ek karke):
stores ke catch blocks → `CatcherHooks.dbError/store`;
network clients ke failure paths → `networkError`;
Deepgram/Kokoro/wakeword/mic failure paths → `laneError`;
upar wali 14 features ke failure paths → `featureError`;
permission-denied callbacks → `permissionDenied`;
LicenceClient/Gate ke error paths → `licenceRecord` (record only);
BrainClient/API error paths → `serverSurfaced`;
Catcher screen → `CatcherHealth.healthLine()` + prev-log indicator.

Phone test (Part 4 flow): open → note → copy → paste →
crash-after → crash section present; har naye hook ka ek
jaanboojh kar fail karke Catcher me entry verify karo.

## P3-02–P3-15 — 15 production additions (3B)

- P3-02 crash reporting + health + status page: `data/HealthStatus.kt`
  (app-side model; server health endpoint Part 2 ka scope hai).
  Weak network par cached state, jhootha "no internet" kabhi nahi,
  crash privacy message, khaali screen ki jagah saaf message.
- P3-03 rate limit + abuse: `data/DeviceRateLimit.kt` (device side;
  S1-30 server rate limit Part 2 ka scope hai, align par alag).
  Protected kaam par licence-check flag, window limit, brute-force
  temporary block (5 fail / 15 min → 15 min block).
- P3-04 data delete + export: `data/DataDeleteExport.kt` (contract +
  confirm guard; retention 30 din, chat memory exempt). Asli
  Room/Supabase calls wiring ke waqt backend me lagenge.
- P3-05 privacy/terms + consent: `data/ConsentCatalog.kt`
  (mic 24x7, screen/camera user-ke-kehne-par, notification read,
  contacts — har ek ka reason + on/off).
- P3-06 payment hardening (FLORA ROUTE): `data/PaymentVerify.kt`
  + `UtrLedger` — UPI+UTR admin-match hardening (duplicate,
  replay, concurrent approval), receipt UTR last-4 only.
  Razorpay NOT configured — webhook/gateway route NAHI hai;
  wo sirf tab jab gateway aaye (alag decision, invent nahi kiya).
- P3-07 update + rollback: `data/UpdateRollback.kt` (design +
  previous-stable record + user-tap-only steps). LIVE rollback
  GATED hai — P2-06 channel (version.json apkUrl+sha256 filled,
  real release SHA se) ke bina live rollback nahi chalega;
  design darj hai, force nahi kiya gaya. P2-06 Part 2 ka scope hai.
- P3-08 backup/restore test: `data/BackupRestoreGuard.kt`
  (restore fail par original data barkaraar — data loss kabhi nahi).
  Phone execution Final Validation Flow me hogi.
- P3-09 offline/weak-network: `data/OfflinePolicy.kt` (BasicTalk
  offline HAVE; LLM kaam "net aate hi" queue me NAHI atakte —
  user ko saaf bataya jayega, dobara kehna hoga).
- P3-10 accessibility + language: `data/AccessibilityCatalog.kt`
  (label audit checklist + theme tests). G2P Part 1 ka scope hai.
- P3-11 support + contact + feedback: `data/SupportRoute.kt`
  (D-31 record ke mutabiq in-app draft, user-tap send only;
  owner personal details kabhi nahi).
- P3-12 CI/CD + versioning: release workflow P2-06/P2-07 ke
  saath hai (Part 2 scope); yahan `ReleaseChecklist` ka
  rel-11 item (changelog + versionCode badhe) gate bandhta hai.
  [skip ci] sirf log commit par — Flora code par kabhi nahi.
- P3-13 security headers + rotation: `data/SecurityPlaybook.kt`
  (scan patterns — sirf pattern text, koi value nahi; rotation
  steps; header checklist; service key kabhi app me nahi).
- P3-14 battery + performance: `data/PerformanceBudget.kt`
  (measurement method + TARGET numbers — measured claim nahi;
  phone measurement ke baad hi pass/fail).
- P3-15 per-release checklist: `data/ReleaseChecklist.kt`
  (12 items, sab blocking — phone-side + server-side dono ke
  bina release nahi; Part 4 flow is gate ko chalata hai).

## P3-16 + P3-17 (3C)

- P3-16 encryption extension (G18): `data/EncryptionExtension.kt`
  — sirf memories encrypted thi; journal/docs/routines/jobs ki
  migration spec + fail-closed state machine (encrypt fail par
  plaintext fallback kabhi nahi, original barkaraar). Crypto wahi
  MemoryCrypto (AndroidKeyStore AES/GCM) — nayi crypto invent
  nahi ki. User-flow change nahi hoga.
- P3-17 observability: `data/Observability.kt` — app-side
  trace_id contract (`x-flora-trace-id`, format
  `flora-<millis>-<6 chars>`), app-side metric counters, alert
  thresholds (5xx/429, provider fail, queue backlog, FCM fail,
  admin activity, quota anomaly, crash spike), 8 playbooks
  (provider outage, leaked key, payment dispute, bad release,
  migration fail, FCM outage, account compromise, restore).
  OVERLAP FLAG: server functions ka code change Part 2 team ka
  scope hai — yahan sirf app-side contract + client hooks hain,
  server code nahi chheda gaya. Server ko trace header aage
  badhana hai (Part 2 ke paas flag hai).

## 3D — Definitions / gate-decisions ONLY (implementation Part 1 ki hai)

Ye section sirf definition + cross-reference hai — Part 1 ka
code yahan nahi chheda gaya:

- M5 time-only triggers v1: definition GAP-M5 ke saath P1-12 me
  owned hai (DONE-CODE, phone pending). Wider trigger types
  bina naye spec ke nahi — Flora-new definition, Maya semantics
  unverified. OVERLAP FLAG → Part 1 (P1-12).
- M10 typing/media Flora-new definitions: P1-13 me owned hai
  (GAP-M10 definitions, DONE-CODE, phone pending). OVERLAP FLAG
  → Part 1 (P1-13).
- Image-gen service gate decision: gate implementation P1-29 me
  owned hai; Bundle B tiers/prices KABHI invent nahi (decision
  D-08 batch me pending). OVERLAP FLAG → Part 1 (P1-29).
- Sub-agents UI definition: UI implementation P1-11 me owned
  hai, helper-choice decision ke baad. OVERLAP FLAG → Part 1 (P1-11).
- 3D orb own-art spec: implementation P1-18 me owned hai;
  creator assets kabhi nahi — sirf apna art + spec.
  OVERLAP FLAG → Part 1 (P1-18).

## Decisions-needed (batch me, ek-ek karke nahi)

- D-08 Bundle B tiers/prices (image-gen gate ke liye; invent nahi kiya).
- D-31 support route: standing-decision record maujood hai (plan
  me darj); in-app route us record ke mutabiq banaya gaya hai —
  agar owner contact method badle to batch me batana.
- P2-06 host decision (Releases / Drive+Telegram / R2) — P3-07
  live rollback isi par gated hai (Part 2 team ke paas).
- Gateway decision (Razorpay ya koi aur) — P3-06 webhook route
  isi par gated hai; tab tak UPI+UTR route hi hai.
- Server-side trace_id propagation + alerting setup — P3-17 ka
  server half (Part 2 team ke paas flag hai).

## CEO decisions cycle 2026-10-31 — Part 3 items (C4-1, C4-2, Support entry, typing, P3-07, P3-17)

- C4-1 storageError — SIRF wahi wire hua jahan asli storage
  failure pakda ja sakta hai (andha har-jagah hook nahi):
  `models/ModelDownloader.kt:283` (model-download part write /
  rename / .ok write ka catch), `update/AppUpdate.kt:168`
  (update-download cacheDir/updates part write + rename ka
  catch), `ui/CatcherScreen.kt:180` (catcher-report-save
  writeText ka catch — saath me fail par galat "save ho gaya"
  message bhi theek kiya gaya, ab saaf failure message aata
  hai). DataStore stores pehle se `dbError` se wired hain
  (36 sites) — unhe dobara storageError nahi diya (category
  alag hai). Kokoro downloadOne ka catch pehle se
  `laneError("kokoro")` se covered hai — double-record nahi
  kiya. fcmDeliveryNote ko fake hook se NAHI bhara gaya:
  FCM delivery failure on-device se capture nahi ho sakta by
  design — asli evidence panel-side ZP-F13 hi hai (ye note
  `data/CatcherHooks.kt` me pehle se darj hai).
- C4-2 CatcherScreen health line + prev-log indicator —
  LIVE TIP (256b70a8) par NAHI mile (tip ki CatcherScreen me
  na `healthLine` tha, na `prev` indicator), isliye IMPLEMENT
  kiya gaya: `ui/CatcherScreen.kt:142` health line
  (`CatcherHealth.healthLine()`, probe `FloraApplication` se
  pehle se wired hai) + `:143-145` prev-log indicator
  (rotation se bani `catcher_events.prev.log` maujood ho to
  wahi dikhta hai). Collision note: local snapshot me ye
  lines pehle se thi (kisi pichhle cycle ki unpushed edits),
  par tip par nahi thi — implementation tip content se
  dobara banaya gaya, local snapshot ko tip nahi mana gaya.
- SupportRoute UI entry — `ui/CatcherScreen.kt:198` par
  "Support ko mail karo" button: pehle se wired
  `SupportRoute.sendIntent(note)` kholta hai (D-31 CONFIRMED
  destination — website par published mailbox, badla nahi,
  koi naya contact invent nahi, owner personal details kabhi
  nahi). Sirf mail composer khulta hai; bhejna user-tap par,
  auto-send kabhi nahi. Draft = user ki apni note line
  (sanitized, 500 chars cap); note khaali ho to user mail
  app me khud likhega.
- Typing failure surface — DEFINITION (koi galat hook nahi):
  typing mode sirf ek UI flag hai (`FloraApp.kt` typingMode —
  ON = chat ka Mic button chhupa, sirf likh kar baat).
  Typed text wahi shared `sendChat` path se jata hai jahan se
  voice text bhi jata hai; us path ke failures pehle se
  covered hain (BrainClient fail hooks, lane errors) ya user
  ko message ke roop me dikhte hain. Typing ka apna koi
  exception/IO/network failure path NAHI hai — isliye koi
  typing-specific hook nahi lagaya gaya (galat hook lagane se
  behtar hai). Prefs toggle write generic DataStore path hai,
  typing failure surface nahi.
- P3-07 rollback readiness — VERIFY (live rollback GATED):
  `channelReady()` + `PreviousStable` + `isRollbackAvailable`
  + `rollbackSteps` `data/UpdateRollback.kt` me maujood hain,
  par unka KOI caller nahi hai — update UI
  (`FloraApp.kt` update flow) sirf `apkUrl.isNotEmpty()`
  check karta hai, na `channelReady` poochhta hai, na update
  se pehle PreviousStable record karta hai. Ye integration
  gap hai (is cycle me FloraApp.kt ko collision risk par
  nahi chheda gaya). LIVE rollback GATED hai: `web/updates/
  version.json` me apkUrl + sha256 khaali hain, P2-06 channel
  + real release SHA (Part 2 scope) ke bina live rollback
  nahi chalega — design + procedure taiyaar hai, force nahi
  kiya gaya. D-01 (GitHub Releases) se consistent raasta:
  channel fill hone par hi rollback khulega. FLAG → Part 2:
  version.json ka notes text abhi bhi "private-repo ki wajah
  se GitHub releases no-auth channel nahi" kehta hai — ye
  D-01 (repo PUBLIC, Releases decided) se stale hai; notes
  update Part 2 ke channel fill ke saath hona chahiye (is
  cycle me version.json Part 2 scope samajh kar nahi chheda
  gaya).
- P3-17 server half coordination — SPEC FLAG → Part 2 team
  (docs-only note; server code `supabase/functions/*` nahi
  chheda gaya): server-side trace_id propagation (app se aaya
  `x-flora-trace-id` function → tool tak aage badhe) +
  alerting setup (`Observability.kt` thresholds: 5xx/429,
  provider fail, queue backlog, FCM fail, admin activity,
  quota anomaly, crash spike) Part 2 ka kaam hai. App-side
  contract barkaraar hai (`TRACE_HEADER = "x-flora-trace-id"`,
  format `flora-<millis>-<6 chars>`, usme koi tod-fod nahi).
  GAP NOTE (spec banate waqt dhyan rakhna): header abhi
  kisi request par attach nahi hota — `BrainClient` me
  TRACE_HEADER ka koi use nahi hai; propagation spec is
  attachment point ke saath hi test ho payega.

## Tester ko kya test karna hai (is push ke files)

Unit tests (naye): CatcherHooksTest, CatcherRotationTest,
DeviceRateLimitTest, PaymentVerifyTest, ObservabilityTest,
Part3GuardsTest. Virtual test = CI green + ye tests pass.
Phone run ka daava kabhi nahi (VM par APK run nahi ho sakta).
Call-site wiring + Catcher screen health-line wiring fixer ke
paas hai — wiring ke baad har hook ka fail-case phone par
Catcher me dikhna chahiye.
