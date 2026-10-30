# Flora Planner-First Routing Fix — SPEC (Pehle-Jaanch: spec → pre-audit → implement)

**User order (2026-10-30 ~13:15–13:18 IST, user ke shabdon me):**
- "Flora har command ko poora PLAN banakar execute kare — sirf YouTube nahi, HAR EK CHEEZ ke liye. Chhota sa command ek baar dega, baar-baar nahi bolega."
- Example: "YouTube kholo to song bajao" → plan me divide ho: YouTube kholna → search me jaana → gana search karke play karna → poora plan banakar execute.
- "Flora ko user kuchh bhi bole, thoda usko samajh kar plan banakar execute karen. Koi bhi cheez ho, confirmation lena ho pehle le le, baar-baar user se nahin puche, execute karen acche se."
- Standing order bhi yehi hai (planner CORE architecture: intention → plan → execute → verify → report, har surface par).

## 0. Confirmation rule (naam lekar — spec + implement + report me yehi naam)

**"Samajh-Pehle, Confirm-Ek-Baar-Shuru-Me" rule — KOI BHI command / KOI BHI surface ke liye:**

1. **SAMAJH PEHLE:** plan banane se pehle intent ko samajhna hai (context/memory se disambiguate karo, andha execute nahi) — phir plan, phir execute.
2. **CONFIRMATION SIRF EK BAAR, SIRF SHURU ME:** agar confirmation chahiye (Auto Mode OFF, ya koi step jahan confirm banta hai) to wo execution shuru hone se PEHLE ek baar le lo — plan dikhake. Execution ke beech me user se dobara KABHI mat poochho, kisi bhi step par nahi.
3. **AUTO MODE ON par:** plan dikhake seedha execute (confirmation ka sawaal hi nahi).
4. **4 safety stops isse UPAR rahenge** (kamzor nahi honge): paisa, delete/irreversible, user login/2FA, genuine block. Farq saaf hai: **stop karna ≠ poochhna** — safety stop par plan ruk kar report karta hai, sawaal nahi poochhta. Isliye mid-execution stop rule ka ullanghan nahi hai.

## 1. Problem (audit se confirmed, 2026-10-30)

- Voice/chat router `FloraApp.kt` me `sendChat` (tip ~line 2293) ke andar hardcoded branches par chalta hai, planner se nahi guzarta.
- Compound block (~4127-4160) me separator **sirf " aur " ya " phir "** hai — **"khol k" / "kholke" nahi**; "gaana bajao" branch (~4274-4296) sirf tab chalti hai jab vakya **"gaana bajao " se shuru** ho.
- Isliye "YouTube kholo to song bajao" jaisa vakya kisi branch se match nahi hota, Brain (AI) ke paas girta hai, jo app khol nahi sakta.
- "Gaana bajao X" branch sirf YouTube **search page ka URL kholta** hai, result check kiye bina "khol raha hun" bol deta hai — video par tap karke play karna juda nahi hai (feature gap, bug nahi).
- `autoExecuteOpen` (~2192) me Accessibility OFF par `NoVerifier` milta hai aur app fire hi nahi hoti (imaandaar line hai, par chain wahi ruk jaati hai).
- **PlannerEngine note:** `PlannerEngine.createPlan(goal)` ya to LearnedEngine offline shortcut hai ya `PlannerBrain.generate` (AI call) — aur `PlanTool` enum me sirf builder tools + `MACRO_UI_STEP` + `NOTE` hain, **app-open/search/tap ka koi tool nahi**. Isliye har voice command ko PlannerEngine ke Room plans se guzaarna galat raasta hai (AI call + latency + keys par nirbhar). Sahi raasta: **planner-shaped deterministic local layer** (neeche §2) — wahi pipeline shape (plan → execute → verify → report), bina AI call ke, bina builder engine chhede.

## 2. Design — VoicePlan (deterministic, local, no AI call)

### 2.1 Model (naya, chhota — `automation/VoicePlan.kt`)

```kotlin
data class VoicePlanStep(
    val title: String,          // chat me dikhne wala step naam
    val executor: VoicePlanExecutor, // APP_OPEN / SEARCH_URL / IN_APP_SEARCH / TAP_FIRST_RESULT / REPORT
    val input: Map<String, String> = emptyMap(),
    val successCheck: String,   // verify ka tareeka (likhit)
    val fallback: String,       // fail par ek deterministic fallback (ya "ruk + report")
)
data class VoicePlan(
    val goal: String,           // user ka asli vakya (short)
    val steps: List<VoicePlanStep>,
    val stops: List<String>,    // 4 stops me se jo lagu hon
    val noDo: List<String>,     // kabhi nahi (silent send, password type, payment confirm, doosre app ka private data)
)
```

- `PlannerEngine` / `PlanTool` / Room plans **chhede nahi jayenge** — VoicePlan alag lightweight layer hai, taaki builder plans se collision na ho.
- Steps ki sankhya ≤ 6 (compound voice commands ke liye kaafi; MAX_STEPS 20 wali builder limit yahan lagu nahi, par 6 se zyada ka plan banta hi nahi — lambi cheez PlannerScreen ka kaam hai).

### 2.2 Parser (deterministic — `VoicePlanParser.parse(lower, question)`)

Samajh-pehle ke liye parser ye karta hai (kram me):

1. **App resolve (context se):** `VerifiedAppLauncher.findByName` — 0 match → pre-execution sawaal (ek baar): "'X' naam ki app nahi mili, naam dobara boliye."; >1 match → pre-execution sawaal (ek baar): "Ek se zyada app mili: A, B. Poora naam boliye."; 1 match → aage.
2. **Foreground context:** `ForegroundAppContext` — "is me X search karo" jaisa follow-up sirf tab in-app jaayega jab saamne wali app sach me wahi ho jo Flora se kholi gayi thi (InAppSearch ka maujooda niyam, barkaraar).
3. **Separators (is kram me, pehla mila wahi):** " aur ", " phir ", "khol k", "kholke", aur narrow pattern "<app> kholo to <action>" me " to " (generic "to" split KABHI nahi — "to" aam shabd hai, sirf is pattern me).
4. **Position-independent media verbs:** vakya me kahin bhi "gaana bajao / gaana chalao / song bajao / song play / gana sunao" mile to media chain banta hai (startsWith ki shart khatam). Gaane ka naam na mile to pre-execution sawaal (ek baar): "Kaun sa gaana bajau? Naam boliye."
5. **Na pehchana gaya pattern → parser null lautaata hai** — tab maujooda branches → Brain ka raasta bilkul waisa hi chalega (kisi aur command ka behaviour nahi badlega).

### 2.3 Runner (confirm-once — `VoicePlanRunner` + sendChat hook)

Kram (har step Auto Log me):

1. **Plan dikhao (chat me, ek baar):** "Plan: 1. YouTube kholo 2. X search karo 3. Pehla result play karo" + chhota TTS ("Plan bana liya hai.").
2. **Confirm gate (sirf yahan, sirf ek baar):**
   - Auto Mode ON → seedha execute (koi dialog nahi).
   - Auto Mode OFF → ek dialog (maujooda `voiceAppConfirm` pattern par `planConfirm` state): "Ye plan chalau?" — Ha → poora plan execute; Ruko → poora plan cancel (koi step nahi chalega).
3. **Execute step-by-step, har step par verify:**
   - APP_OPEN → `openVerified` result: Opened/AlreadyOpen = pass; NoVerifier → genuine block stop (imaandaar line + Settings intent); NoLaunchIntent/else → fallback ya report.
   - SEARCH_URL → `openWebsiteVerified` (search URL) — result check ke baad hi agla step.
   - IN_APP_SEARCH → `InAppSearch.searchInApp` outcome: Done = pass; TypedOnly/NoSearchBox → fallback SEARCH_URL (ek baar), phir report.
   - TAP_FIRST_RESULT → §3.2 ke mutabiq; fail → search results khule rehne do + imaandaar report ("Search khol diya hai, pehla result khud tap karo").
4. **Fail par khud replan (deterministic fallback, ek baar):** fallback step chalao; fallback bhi fail → ruk + imaandaar report. **AI se replan nahi** (koi key/latency nirbharata nahi).
5. **Aakhri report (ek baar, chat + chhota TTS) + Auto Log:** kaun sa step hua, kaun sa nahi, aage kya karein.
6. **Beech me kabhi sawaal nahi.** Safety stop (paisa/delete/login/genuine block) mile to plan ruk kar report karega — ye poochhna nahi hai (§0.4).

### 2.4 Wiring (sendChat hook — sabse kam chhed)

- Hook `sendChat` me **KeyInputGuard + Face Lock gate ke BAAD, hardcoded branches se PEHLE** lagega.
- Sirf tab jab parser koi plan lautaaye; warna maujooda flow bilkul waisa hi (koi regression nahi).
- Typed chat + voice + continuous mic — teeno isi `sendChat` se jaate hain, isliye teeno ko ye fayda milega.
- `voiceAppConfirm` dialog pattern ka dobara istemal (naya dialog design nahi); `planConfirm` state `voiceAppConfirm` se PEHLE declare hogi (declaration-order trap se bachne ke liye).

## 3. Batch 1 — Planner routing (sab commands ke liye, asli fix)

- `VoicePlan.kt` (model) + `VoicePlanParser.kt` (deterministic parse, §2.2) + `VoicePlanRunner` (confirm-once execute, §2.3) + sendChat hook (§2.4).
- Parser patterns (kam se kam): compound open+action ("X kholo aur/to/khol k Y"), media chain (position-independent), single app-open (maujooda confirm niyam plan me dhal kar — Auto Mode ON par seedha, OFF par ek baar confirm), follow-up in-app search (ForegroundAppContext niyam).
- Unit tests: parser tests (separator set, position-independent media verb, "to" narrow pattern, generic "to" split NAHI, unknown → null). Test file: `app/src/test/.../VoicePlanParserTest.kt`.
- Gate: code + CI green + live-tip content verify (fresh fetch diff) + re-audit (naye branches purane behaviour ko nahi badalte — iska saboot: unrecognized input par parser null → maujooda raasta).

## 4. Batch 2 — YouTube media chain + doosra common flow (pehle verticals)

### 4.1 YouTube chain (steps, har step par success check) — Batch 2 design

1. **Open:** `VerifiedAppLauncher.openVerified(YouTube)` — pass: Opened/AlreadyOpen; NoVerifier → stop + imaandaar line + Settings intent; **cold-start retry:** `NotVisible` par ek baar dobara open (audit W4 — badi app pehli baar der se khule to pehla try fail ho sakta hai).
2. **Search:** pehle `InAppSearch.searchInApp(service, song)` (service enabled + YouTube foreground ho to); outcome Done na ho to fallback: search URL `https://www.youtube.com/results?search_query=<encode>` via `openWebsiteVerified`. Fallback use hua to aakhri report me "(search web se khola gaya)" judta hai (fallback transparency).
3. **Play first result (nayi narrow chain — SD-5 general surface NAHI):** naya service helper `clickFirstVideoResult()` (`FloraAutomationService` me):
   - Video ka title TextView khud clickable nahi hota — uska item container clickable hota hai — isliye **sabse GEHRA clickable container** chuna jata hai jiske neeche (2 level tak) title-jaisa text ho. Search-box/nav labels (Search/Khoj/Home/Shorts/Subscriptions/Library/You/Create/Notifications/Filter/Cast/All) aur editable fields ko chhoda jata hai. Tree traversal 800 nodes par capped hai.
   - Verify: `waitForForeground(YouTube pkg, 3000)` → click → 1500ms wait → foreground abhi bhi YouTube → pass ("pehla result khol diya hai, YouTube app me auto-play shuru hota hai").
   - Koi candidate na mile ya foreground badal jaye → step ka `failMessage`: "Search khol diya hai, pehla result khud tap karo (auto-play chain yahan ruka)." — jhootha "play ho gaya" kabhi nahi.
   - **Limit imaandaari se:** ye heuristic hai — asli YouTube UI par verify Phase 10 me hoga (Batch 2 ka gate code+CI+tip hai, phone-play ka daava nahi).
   - **Boundary saaf:** ye sirf YouTube first-result chain hai; poora tap/type/swipe surface SD-P4 me hi banega.
4. **UI-ready wait (audit step 5) — Batch 2 me NAHI, wajah ke saath:** `visibleTexts` me "Search" text dhoondhna YouTube home par bharosa-layak nahi hai (search icon-only ho sakta hai — audit khud W3/W8 me ye gap maanta hai). `searchInApp` ka apna outcome hi asli signal hai; us par fallback hai. Isliye text-poll wait nahi joda gaya (galat signal par plan rokna zyada nuksaan hai).
5. **Purani "gaana bajao" branch ka behaviour:** parser gaana-naam wali media ko claim karta hai, isliye main gaana branch ab sirf missing-song question ke liye bachti hai (uska bina-verify "khol raha hun" us raaste par kabhi nahi chalta). Routine runner ka gaana step (`FloraApp.kt:1055-1064`, result ignore) Batch 4 me result-check par laaya jayega (W3/W4) — Batch 2 me routine runner nahi chheda gaya (W8 mini-router alag domain hai, chhote batch ka usool).

### 4.2 Doosra common flow (audit se chuna gaya — routine nahi, kyunki routine runner me compound/in-app support hi nahi hai, W8)

**Test command:** "WhatsApp kholo aur Priyadarshi search karo" (foreground chat/voice se; background se nahi, kyunki W5 ke hisab se background me compound nahi chalta).

| Step | Kya hona chahiye | Success check | Fail par fallback |
|---|---|---|---|
| 1. Parse | Compound split: appName=WhatsApp, rest=Priyadarshi search karo | Dono hisse non-empty, appName galat app na ho | Separator na mile to brain par mat girao — "kaun si app, kya search karna hai" ek baar upfront poochho |
| 2. App match | `findByName` se ek hi match | Exactly 1 match | 0 → "app nahi mili" + sujhaav; 1+ → ek baar upfront choice (beech me dobara nahi) |
| 3. Verified open | `openVerified` | `Opened`/`AlreadyOpen`, 3500ms ke andar | `NoVerifier` → Permissions/Auto screen + "Accessibility ON karo"; `NotVisible` → ek retry (cold-start), phir "nahi khul paya" (kabhi "khol diya" nahi); `NoLaunchIntent` → Play Store raasta |
| 4. Context carry | `noteOpened(pkg, label)` | Agle step me `matchesForeground` true | Note na hua to research-search par na gire — explicit "is app me" raasta ya step roko |
| 5. UI ready wait | App ka search UI taiyaar ho | `visibleTexts` me "Search" (ya app ka search label) dikhe, fixed delay par bharosa nahi | Timeout par ek scroll + dobara check, phir "search box nahi mila" report |
| 6. In-app search | `searchInApp` | Outcome `Done` ho (sirf `TypedOnly` nahi) | `NoSearchBox` → visibleTexts dikhao (label alag ho sakta hai); `TypedOnly` → Search/Go/Enter dobara, phir "likh diya hai, enter dabao" imaandaar message |
| 7. Result verify + report | Query ka result screen par dikhe | `visibleTexts` me query ya result-item dikhe | Na dikhe to "search kar diya" ka daava mat karo — "search box tak pahuncha, result verify nahi hua" bolo; **result-tap chain is waqt bana hi nahi**, isliye scope search-tak hi hai |
| 8. Report + log | Har step ka AutoLog | Open/search ki alag-alag entry | Koi step log ke bina "ho gaya" na bole |

**Batch 1 me is flow ka status:** parser + runner pehle se steps 1–4 + 6 (submit tak) cover karte hain; step 5 (visibleTexts UI-ready wait) aur step 7 (result-visibility nuance) Batch 2 me aayenge. Routine ke roop me yehi flow **is waqt fail hona hi chahiye** (W8) — use is test ka hissa mat banao jab tak routine runner me compound + in-app support na aa jaye.

## 5. Batch 3 — Builder-entry wiring (user report: debug APK me builder tha hi nahi)

**Finding (tip + APK dex + audit se verified, 2026-10-30):**
- Builder code **APK me maujood tha** (da42439b ke `classes6.dex` me 166 BuilderScreen references; tip me `ui/BuilderScreen.kt`).
- Entry **maujood** hai: TopAppBar me "Menu ▾" button (~FloraApp.kt:1507) → DropdownMenu me "Builder" item (~1560) → `Screen.BUILDER` → `BuilderScreen` call site (~5427); voice se "builder kholo" → generic screen map (~4698) → `Screen.BUILDER`.
- **Asli kaaran (audit ne pakda — sirf discoverability nahi):** Menu item ungated hai (B2-11 ruling), **par screen gated hai**: `ui/BuilderScreen.kt:303-323` par `PlanFeatures.isAllowed("builder")` check hai. Licence-verify se mili list non-empty ho aur usme `"builder"` key na ho, to screen sirf ye message dikha kar return kar deti hai: "Builder abhi is plan me nahi hai." Usi block ka comment: server hissa (F2: seed 14→15, allowlist 14→15, panel editor) **pending** hai. Fail-open sirf tab jab list kabhi mili hi nahi ya khali hai (`core/PlanFeatures.kt:33`).
- Matlab: Menu me Builder dikhna + andar sirf gate line milna = user ki nazar me "builder hai hi nahi". Ye entitlement ka mamla hai, code missing ka nahi.
- **Batch 3 wiring (sirf entry + consistency, entitlement change NAHI):**
  1. **Gate message pehle se imaandaar hai (sudhaar ki zaroorat nahi):** `BuilderScreen.kt:303-323` ka block do lines dikhata hai — "Builder abhi is plan me nahi hai." + "Builder (Pro) chahiye — plan-features me 'builder' key aane ke baad ye screen khulegi. Agar licence list abhi mili hi nahi hai to purana vyavhaar hi rahega (fail-open)." Gate hatana NAHI hai (wo F2/server faisla hai, user-gated).
  2. Voice exact "builder" (bina "kholo" ke) → `Screen.BUILDER` (generic map se pehle ek chhota exact check — aaj bare "builder" Brain par girta hai).
  3. Menu item par gate ka ishara: `PlanFeatures.isAllowed("builder")` pure sync read hai (cached volatile list, `core/PlanFeatures.kt:33`) — menu khulte waqt padha ja sakta hai; allowed na ho to item "Builder (Pro)" dikhega, taaki menu aur screen ka bartaav ek jaisa lage (audit suggestion #4). Menu har baar khulne par dobara compose hota hai, isliye cached value hi kaafi hai.
  4. Final report + parent message me user ko seedha raasta + gate ki wajah likhna: Menu ▾ → Builder, ya bolo "builder kholo"; gate line dikhe to F2 server pending ki wajah se hai.
  5. Koi naya builder feature nahi; Settings row / Home shortcut (jo audit me missing mile) Batch 3 me nahi — wo design pass ka area hai.
- **Batch 3 implemented:** (a) voice bare "builder" → `Screen.BUILDER` (sendChat me generic map se pehle exact check; "builder kholo" map entry alag se maujood hai); (b) menu item ab `PlanFeatures.isAllowed("builder")` dekh kar "Builder" ya "Builder (Pro)" dikhata hai (sync cached-list read, menu khulne par compose hota hai) + stale B2-ungated comment sudhara gaya; (c) gate message me koi change nahi (pehle se do-line imaandaar message hai); (d) entitlement change NAHI (F2 server pending). Naya unit test nahi (sendChat branch + menu label CI build se cover; phone check Phase 10 me: gated phone par menu me "Builder (Pro)" + screen par gate message; fail-open phone par seedha Builder screen).

## 6. Workflow audit + automation surfaces list (audit child report se, 2026-10-30)

**Ek line me samasya (audit):** Ek planner nahi, **teen alag router** hain jo jude nahi hain — (1) chat ke hardcoded branches, (2) fallback `IntentRouter`, (3) builder-only `PlannerEngine` — aur daily automation (app-open, search, tap, play) planner ke toolset me hai hi nahi.

**Toote points (har ek file:line ke saath audit report me):**
- **W1 Chat:** `sendChat` pehle saikdon hardcoded branches chalata hai; `IntentRouter.route` sirf fallback hai (`FloraApp.kt:5165`); `Route.Plan` sirf Planner screen kholta + goal prefill karta hai (`5210-5213`) — plan banata/execute nahi karta. Plan sirf explicit phrases par milta hai.
- **W2 Compound:** sirf 2 separators, sirf vakya-start/suffix; doosra hissa fixed `delay(1000)` ke baad dobara `sendChat` se, bina UI-ready verify ke. Background se compound bilkul nahi aata (W5).
- **W3 YouTube:** search page hi aakhri step; play chain bana hi nahi; gaana branch result check kiye bina "khol raha hun" bolti hai (`4275-4296`, routine wala `1055-1064` bhi).
- **W4 App-open:** `openVerified` imaandaar hai (`VerifiedAppLauncher.kt:84-135`), par gaana/routine caller result ignore karte hain; timeout 3500ms (cold-start par `NotVisible` ka risk, koi retry nahi); `findByName` substring match (multi-match = rukawat).
- **W5 Voice background:** command execute hi nahi hota — `handleHeard` sirf `pending` + app foreground + dobara confirmation (`BackgroundMicService.kt:347-360`, `MainActivity.kt:91-93`, `FloraApp.kt:1452-1455`); `isCommand` list me compound/`khol k`/`gana bajao`/planner/`builder`/`routine` kuch nahi (`274-299`) — jo list me nahi wo brain Q&A par girta hai; lock par queue nahi hota; echo guard gap bhi (pehle audit se).
- **W6 Overlay/notch:** button + status pill hai, execution surface nahi; permission na ho to chupchaap return (koi guidance nahi).
- **W7 Browser (B1):** dikhana/padhna/bharna hai, planner se chalana nahi (`ToolRegistry` me browser descriptor-only; `PlanTool` me browser tool hi nahi); form-fill seemit (WebView sirf BrowserScreen active ho tab; native label-guess + fixed submit list); login-wall/captcha par research-fallback (stop #3, design-sahi).
- **W8 Routines:** mini-router hai — na `sendChat`, na MacroEngine, na planner (`FloraApp.kt:960-1220`); compound/in-app/macro-step nahi chalta; fail = "step chhod diya" + delay + drop, koi retry/verify nahi; flag OFF = queue saaf.
- **W9 Builder execution:** `runOneStep` me `successCheck` kabhi evaluate nahi hota (sirf validate + dikhaya jata hai); `PlanTool` me daily automation ka tool nahi; learned shortcut sirf NOTE/MACRO par; background execution kamzor (service fail par bhi plan chalta hai).
- **W10 Planner→automation pul:** 3 manual hops (MacroStore save → Routines screen par Chalao + confirm → PlannerScreen par runner-log confirm); AutoLog marker kho jaye to step hamesha BLOCKED.
- **W11 Confirmation:** aaj ulta hai — macro sensitive-step par beech me `NeedsConfirm` dialog (`MacroEngine.kt:170-192`, `FloraApp.kt:712-717`); planner per-step confirm; sirf Routines ka confirm upfront-sahi hai (`RoutinesScreen.kt:208-237`). Fix = confirmation ka waqt badalna (upfront-once), sirf code nahi.
- **W12 Settings:** automation switches 5 jagah bikhre (AutoMode, Accessibility, Builder tokens, Skills toggle, gates); gate fail par sirf "Ye feature abhi band hai" — kaun sa gate, kabhi nahi bataya; dono flag systems fail-open.

**Surfaces verdict (condensed — poori table audit report me):**
- Code-complete: website open (caller par nirbhar), `clickAnyText`, `typeIntoFocused` (seemit), `goBack/goHome`, BrowserScreen form-fill (sirf active ho tab), B1 browser dikhana/padhna, MacroEngine 8 steps (play/search/swipe/select nahi + beech-confirm), Macros UI, PlannerEngine builder tools, GoalOrchestrator (beech-confirm ke saath), DeviceActions NoAccess, App install, AutoScreen, background mic sunna/jawab.
- Toota: app-open (cold-start/multi-match), in-app search (sirf Search/Khoj text, icon-only box nahi), `clickText` (pehla substring match, verify nahi), `setTextOnFirstEditable` (galat field ka risk), scroll/swipe (target/verify nahi), native form-fill (label guess), routines mini-router, planner-macro adapter (3-hop), notch tap.
- Bana hi nahi: planner-driven browser, planner for daily automation (PlanTool me tool nahi), overlay execution, background execution, macro Swipe/Search/Play/LongPress/Coordinate steps, multi-file builder preview.
- Grant-chahiye: app-open (Accessibility), call/SMS (permission + AutoMode/confirm), Shizuku/One-Tap (Shizuku app + grant), screenshot (API 30+).
- Sirf code me, UI se reachable nahi: purana `AppLauncher.launch()` (dead/legacy — wapas mat jaana), `ToolRegistry` descriptors (execute nahi karte), `searchMemory/selfMap/aiBuildStatus` (chat/BuilderScreen se call site verify nahi hua).

**Batch 4 (Batch 3 ke baad — is spec me darj, implement baad me):**
- Background `isCommand` me compound/media/builder patterns jodo (execution raasta wahi pending → sendChat hook rahega, naya background execution model nahi).
- Routine gaana/website steps me result-check (W3/W4 — "khol diya" sirf Opened par).
- APP_OPEN cold-start retry (NotVisible par ek baar, W4).
- Macro/planner beech-confirm ko upfront-once par lana (W11) — alag design faisla chahiye, isliye Batch 4 me sirf VoicePlan/routine surfaces par lagu hoga.

## 7. Safety / boundaries (sab batches par lagu)

- **NEVER:** silent action; payment auto-confirm; password/2FA type ya store; doosre app ka private data; creator copy; koi nayi API key (YouTube Data API mat laana); koi spend; `[skip ci]`; `.github/workflows/` API se.
- **4 stops** (§0.4) har plan par lagu; stop par ruk + report, sawaal nahi.
- Har plan + har step Auto Log me; full key/token kabhi log/chat/screen par nahi (KeyInputGuard barkaraar).
- General SD-5 surface SD-P4 domain hi rahega — is fix me sirf §4.1 ki narrow chain.
- Call/notify coordinator is waqt voice files chhu raha hai — edit/push se PEHLE live tip se fresh content, chain linear, koi clobber nahi.

## 8. Phone-test steps (Phase 10 ke liye, abhi claim nahi)

- "YouTube kholo to <song> bajao" → plan chat me dikhe → Auto Mode ON par bina dobara poochhe: YouTube khule → search ho → pehla result khule/play ho → aakhri report ek baar.
- Auto Mode OFF par: plan dikhke EK baar confirm maange, confirm ke baad beech me kabhi na poochhe.
- "khol k" wala vakya ("YouTube khol k gana bajao") bhi wahi plan banaye.
- Accessibility OFF par: imaandaar line + Settings intent, chupchaap fail nahi.
- Gaane ka naam na ho to: shuru me ek baar poochhe ("Kaun sa gaana?"), beech me nahi.
- "builder" / "builder kholo" → BuilderScreen khule (Menu ▾ → Builder wala raasta bhi batana).
- Phone evidence chahiye: Accessibility ON? Auto Mode ON? (donon haan/na — value kabhi nahi).

## 8b. Batch 1 deviations (jaanboojh kar, record ke liye)

- **Single app-open Batch 1 me migrate NAHI hua:** maujooda single-open branch pehle se confirm-once compliant hai (Auto Mode ON → seedha execute, OFF → ek `voiceAppConfirm` dialog) — toota hua nahi hai, isliye churn nahi kiya. Planner migration ka agla charan (follow-up) me aayega.
- **Gaane ka naam na mile to runner question nahi poochhta:** parser null lautaata hai, maujooda gaana branch shuru me ek baar poochhti hai ("Kaun sa gaana bajau?") — ye bhi pre-execution hai, rule ka ullanghan nahi; doosra question path banane se bacha gaya.
- **Batch 1 media plan search par khatam hota hai (play Batch 2 me):** TAP step bina `clickFirstVideoResult` ke jhootha hota, isliye Batch 1 me plan steps [open, search] hain aur final report imaandaari se search tak ka daava karta hai. Poora play Batch 2 me aayega.
- **Fix (CI run 41023346683 fail ke baad):** `executeVoicePlan` pehle sendChat ke paas wale andar wale block me tha (16-space level) — hook se dikhta tha, par `planConfirm` dialog (dialog block) se `Unresolved reference: executeVoicePlan` aaya (FloraApp.kt:5824). Wajah: Kotlin local fun sirf apne block ke andar dikhta hai; dialog alag block me hai. Fix: fun ko composable-body level (4-space, `runGuardedVoice` ke bagal) par laaya gaya — wahan se hook aur dialog dono dekh sakte hain. Lesson: FloraApp.kt me naya helper fun hamesha 4-space composable-body level par rakho, ya dialog me logic inline karo (jaise `voiceAppConfirm` dialog karta hai) — andar wale block ka fun dialog se kabhi nahi dikhega.
- **Fix (CI run 41024742806, unit-test fail ke baad — Batch 2 ke saath aaya):** `compoundKholoToMedia` fail hua (152 me se 1): "youtube kholo to arijit ka gana bajao" par parser galat plan banata tha — query "arijit ka" ki jagah "youtube kholo to arijit ka" aati thi. Wajah: separator " kholo to " ke andar hi " kholo" hota hai, isliye `substringBefore` se `first` seedha "youtube" milta hai — par branch `first.endsWith(" kholo")` maang rahi thi, jo kabhi sach nahi hota → appName null → poora vakya standalone media me gir kar song ban jata tha. Fix: " kholo to " branch ab `first` ko seedha app naam maanti hai ("khol k" jaisi). Batch 1 ka gate is combined green run + tip re-audit se poora hoga (fix chhota tha, alag CI cycle ka kharch nahi kiya gaya; Batch 2 ke tests isi raaste ko cover karte hain).

## 9. Pre-audit (implement se PEHLE — result 2026-10-30, tip 4e4c5a91 se verify)

- [PASS] Parser patterns vs maujooda branches: hook sirf recognized pattern par plan lautaata hai, warna null → maujooda branches/Brain raasta untouched. Koi duplicate branch nahi banegi (purani compound/gaana branches Batch 2 tak fallback ke roop me rahengi, phir verify-after-open par laayi jayengi).
- [PASS] `VoicePlan*` / `planConfirm` naam-collision nahi (local tree grep 0 hits).
- [PASS] `planConfirm` state `voiceAppConfirm` (tip ~line 293, remember-state region) ke turant baad declare hogi — sendChat hook aur dialog use se pehle (declaration-order trap se safe).
- [PASS] Hook placement (refined): `sendChat` me KeyInputGuard + Face Lock gate + teaching-routine capture block + pending-contact clarification block — **in sab ke BAAD**, pehli fresh-command branch se PEHLE. Wajah: teaching/clarification jawab fresh command nahi hain; hook unhe intercept nahi karega. Exact line edit waqt fresh tip se confirm hogi.
- [PASS] Koi PlannerEngine/PlanTool/Room change nahi (builder plans untouched) — VoicePlan alag lightweight layer hai.
- [PASS] `InAppSearch.searchInApp` verified: password-node guard → type → submit (Search/Go/Khoj click) → Done/TypedOnly/NoSearchBox.
- [PASS] `openWebsiteVerified` verified: `OpenResult` lautaata hai; service null ho to `FiredUnverified` — runner non-Opened ko fail path me lega (spec §2.3).
- [PASS] Naya service helper (`clickFirstVideoResult`) sirf YouTube chain me istemal hoga (general surface nahi).
- [PASS] Koi naya permission/manifest change nahi (Accessibility service + intents pehle se maujood).
- [PASS] Unit tests: `app/src/test/java/com/flora/app/automation/VoicePlanParserTest.kt` (automation test dir edit waqt confirm hogi; maujooda precedent: `tools/BuilderGuardTest.kt`).
- [PASS] CI chain clean: run 41021786026 (tip 4e4c5a91) SUCCESS — is spec push se pehle koi in-progress run nahi.
- **Pre-audit verdict: PASS — Batch 1 implement kar sakte hain.** §6 (workflow audit) + §4.2 (doosra flow) audit child report aane ke baad bhare jayenge; Batch 1 un par nirbhar nahi hai.
