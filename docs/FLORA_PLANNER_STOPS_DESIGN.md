# Flora Planner Stops Design — CEO D-06 (R4-1 / W11 / R4-4)

Design-first doc, phir implement (is doc ke mutabiq code isi cycle me
badla gaya hai — neeche "Implement" section me file anchors hain).
Authority: CEO Decisions Batch 2026-10-31, D-06. Live tip verify
base: ef88622c (is cycle ke D-03 push se pehle; D-06 push uske upar).

## 1. R4-1 — stops = hard gates (pehle kya galat tha)

Verified maujooda haalat (tip ef88622c, local copy tip-identical):

- `planner/PlannerBrain.kt:162` — prompt schema me `"stops":[], "noDo":[]`
  maujood hai; `parsePlan` (`PlannerBrain.kt:178`) inhe `PlanDraft` me
  parse karta hai; `planner/PlanRoom.kt:253-254` persist aur `:298-299`
  load karta hai; `planner/PlannerScreen.kt:134-138` inhe sirf DIKHATA
  hai.
- Engine path me plan-level stops kabhi enforce nahi hote the:
  `planner/PlannerEngine.kt` ka `runOneStep` (`:788-794`) sirf
  step-level flags (`spend` → stop #1, `irreversible` → stop #2) aur
  quota gate se rokta tha. Plan-level `stops` list bhari hone par bhi
  steps chalte — display-only dead gate.
- VoicePlan path me ulta haal tha: `automation/VoicePlan.kt:76-80`
  (`VoicePlanRunner.execute`) stops non-empty hone par poora plan
  refuse karta hai (hard gate — sahi), par `noDo` kabhi check nahi
  hota tha (default list `VoicePlan.kt:44-49` sirf data thi).
- Parser stops: `automation/VoicePlanParser.kt` kabhi `stops` set nahi
  karta (saare `VoicePlan(...)` construction sites par `stops` param
  maujood hi nahi hai) — isliye VoicePlan path ka stops gate kabhi
  trigger hi nahi hota tha (register ki "dead-guard" shikayat sahi thi).

### Design faisla (D-06)

- **Stops = hard gates, dono paths par.** Koi step stops wale plan me
  nahi chalega; plan PAUSED hoga, wajah ke saath (silent drop nahi,
  chupchaap execute nahi).
- **Parser stops kab set kare:**
  - `VoicePlanParser` (deterministic keyword router: app-open /
    search / tap-first-result): stops KABHI set nahi karta — iska
    domain hi sensitive actions ko cover nahi karta. Sensitive goal
    (paisa/delete/login jaisa) is parser se plan banta hi nahi
    (null), isliye parser-level stops invent karna galat hota.
  - `PlannerBrain` (model JSON parser): stops model output se aate
    hain (schema me field hai) — engine inhe enforce karega.
- **noDo = hard gate, prospective-only.** Pending steps par lagu;
  VERIFIED steps ko retroactively gate nahi karta (pehle ho chuka
  kaam dobara gate karke rokna galat hota). Match rule: step title
  (engine) ya title+input values (VoicePlan) me noDo phrase ka
  case-insensitive substring match. Phrase-match hi sahi hai —
  noDo entries free-text hain, enum nahi; koi naya enum invent
  nahi kiya gaya hai.

### Implement (is cycle)

- `planner/PlannerEngine.kt` — `runLoop` ke shuru me, har iteration
  par: plan-level `stops` non-empty → PAUSED + log + state message,
  return (koi step nahi chalta). Uske baad pending steps par noDo
  phrase-match → PAUSED + log + state message, return.
- `automation/VoicePlan.kt` — `VoicePlanRunner.execute` me stops gate
  (pehle se tha, barkaraar) ke baad noDo phrase-match gate joda gaya
  hai (step title + input values par, wajah me entry + step naam).

## 2. W11 — macro confirms upfront-once

Verified maujooda haalat (tip ef88622c):

- `automation/MacroEngine.kt:247,260` — sensitive Tap (label me
  pay/submit/buy/checkout/login jaisa shabd) aur password-field Type
  par runner mid-run `NeedsConfirm` deta tha; `ui/FloraApp.kt` ka
  `runMacroFrom` har baar dialog dikhata tha (per-step confirm).
- `ui/RoutinesScreen.kt` ka Chalao dialog kehta tha "Sensitive step
  par dobara poocha jayega" — W11 spec
  (`docs/FLORA_PLANNER_ROUTING_FIX_SPEC.md:153`) ke mutabiq ye ulta
  tha; sirf Routines ka confirm upfront-sahi tha.

### Design faisla (D-06)

- **Upfront-once:** macro shuru hone se PEHLE, label se pehle se
  jaane jaane wale sensitive Tap steps ek baar list karke confirm
  honge; Haan ke baad wo steps beech me dobara confirm nahi
  maangenge. Ye confirm-once rule se consistent hai.
- **Fail-closed exception (documented, chhupaya nahi):** password
  field ki maujoodgi plan-time par nahi jaani ja sakti (screen
  runtime par dikhti hai) — isliye password-field Type steps par
  runtime `NeedsConfirm` barkaraar hai. Ye exception sirf us case
  par lagu hai, label-known taps par nahi.
- Chat path par pehle koi upfront confirm tha hi nahi (seedha run,
  phir mid-run dialog) — upfront dialog teeno paths (Chat,
  Routines, TeachOnce) par ek hi jagah (`runMacroFrom`) lagta hai,
  isliye koi path bina upfront confirm ke sensitive step nahi
  chalayega.

### Implement (is cycle)

- `automation/MacroEngine.kt` — `sensitiveStepIndices()` /
  `sensitiveStepLabels()` public helpers + header comment update.
- `ui/FloraApp.kt` — `runMacroFrom` me `fromStep == 0` par upfront
  check (pehle se confirm steps par dialog dobara nahi);
  `macroUpfrontConfirm` dialog (labels ke saath); Haan par saare
  sensitive indices pre-confirm + run; Ruko par abort.
  Mid-run `NeedsConfirm` dialog ab sirf runtime password-field
  fallback ke roop me bachta hai (comment update).
- `ui/RoutinesScreen.kt` — dialog text sudhara ("dobara poocha
  jayega" hataya, upfront-once + runtime exception likha).

## 3. R4-4 — fresh-device semantics

### Design faisla (D-06)

- **Fresh device = zero usage + default OFF states. Koi carried-over
  counter nahi.** Naye device / khaali store par har counter 0 se
  shuru hoga, har toggle OFF hoga, jab tak user khud ON na kare.

### Verify (code pehle se match karta hai — koi behavior change nahi)

- `planner/AiBuildQuota.kt:95-102` — `buildsToday()`: stored day
  khaali ya naya din ho to 0 (exception par bhi 0).
- `data/FloraPreferences.kt:1100-1110` — `talkSecondsUsed` /
  `addTalkSeconds`: stored date aaj se alag ho to used 0 se shuru.
- `data/FloraPreferences.kt` — toggles default OFF: `callTalk()`
  (`?: false`), aur baaki boolean prefs isi pattern par.
- `data/LicenceClient.kt:370-380` — `countUsage`: stored device_id
  khaali ho to `device_id` body me bheja hi nahi jata → server
  `counted:false` (register ka "stored id khaali → counted:false"
  isi se poora hota hai).
- Backup restore counters ko carry nahi karta (backup scope me
  quota/usage counters EXCLUDE hain — audit-log R-B6-7) — isliye
  restore bhi fresh-device semantics nahi todta.

## 4. Kya is cycle me NAHI kiya gaya (saaf)

- Koi naya stop enum, koi naya parser, koi naya permission, koi
  naya UI screen nahi banaya gaya hai.
- VoicePlanParser me stops set karna invent nahi kiya gaya hai
  (upar §1 me wajah darj hai).
- Password-field runtime confirm hataya nahi gaya hai (§2 exception).
- R4-4 par koi code change nahi hai — semantics pehle se match
  karte hain, sirf is doc me record kiya gaya hai.
- Phone test is cycle ka hissa nahi hai (user-phone gated) — upar
  ke gates code-level hain, phone behaviour ka daava nahi hai.
