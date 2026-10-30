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

### 4.1 YouTube chain (steps, har step par success check)

1. **Open:** `VerifiedAppLauncher.openVerified(YouTube)` — pass: Opened/AlreadyOpen; NoVerifier → stop + imaandaar line + Settings intent.
2. **Search:** pehle `InAppSearch.searchInApp(service, song)` (service enabled + YouTube foreground ho to); outcome Done na ho to fallback: search URL `https://www.youtube.com/results?search_query=<encode>` via `openWebsiteVerified`.
3. **Play first result (nayi narrow chain — SD-5 general surface NAHI):** naya service helper `clickFirstVideoResult()` (`FloraAutomationService` me):
   - `waitForForeground(YouTube pkg, timeout)` → phir tree traverse: search-box/header/navigation nodes chhodo, pehla clickable node jiska text non-empty ho aur jo "Search"/"Khoj"/nav label na ho → click.
   - Verify: click ke baad short wait + foreground abhi bhi YouTube → report "pehla result khol diya hai (auto-play YouTube app me shuru hota hai)".
   - Koi candidate na mile → search results khule rehne do + imaandaar report ("Search khol diya hai, video par khud tap karo") — jhootha "play ho gaya" kabhi nahi.
   - **Boundary saaf:** ye sirf YouTube first-result chain hai; poora tap/type/swipe surface SD-P4 me hi banega.
4. **Purani "gaana bajao" branch ka behaviour:** is chain ke aane ke baad wo branch sirf tab chalegi jab parser plan na bana paaye (fallback) — uska "khol raha hun" bina-verify bolna tab bhi galat hai, isliye Batch 2 me us branch ko bhi verify-after-open par laaya jayega (result check ke baad hi message).

### 4.2 Doosra common flow (workflow-audit report se final hoga — PENDING-AUDIT)

- Audit child (workflow + surfaces) ki report se sabse zyada istemal hone wala doosra flow chuna jayega (routine-run ya generic compound app-open+in-app-action me se).
- Us flow ka test case bhi Batch 2 me aayega (steps + success check per step + fallback), taaki fix sirf YouTube tak seemit na rahe.
- Jab tak audit report na aaye, Batch 2 ka doosra flow SPEC-PENDING rahega — guess par nahi banega.

## 5. Batch 3 — Builder-entry wiring (user report: debug APK me builder tha hi nahi)

**Finding (tip + APK dex se verified, 2026-10-30):**
- Builder code **APK me maujood tha** (da42439b ke `classes6.dex` me 166 BuilderScreen references; tip me `ui/BuilderScreen.kt` ungated — B2-11 ruling: koi invented key gate nahi).
- Entry **maujood** hai: TopAppBar me "Menu ▾" button (~FloraApp.kt:1507) → DropdownMenu me "Builder" item (~1560) → `Screen.BUILDER` → `BuilderScreen` call site (~5427); voice se "builder kholo" → generic screen map (~4698) → `Screen.BUILDER`.
- Isliye problem **code missing nahi, discoverability** hai: lambi dropdown me 7th item + voice raasta user ko pata nahi tha.
- **Batch 3 wiring (sirf entry, koi naya builder feature nahi):**
  1. Voice exact "builder" (bina "kholo" ke) → `Screen.BUILDER` (generic map se pehle ek chhota exact check).
  2. Final report + parent message me user ko seedha raasta likhna: Menu ▾ → Builder, ya bolo "builder kholo".
  3. Koi naya screen/button design nahi — dropdown reorder jaisa cosmetic churn nahi (7th item scroll ke bina bhi dikhta hai; asli gap jaankari ka tha, jagah ka nahi).

## 6. Workflow audit + automation surfaces list (PENDING-AUDIT — audit child ki report se bharega)

- Sections: (a) har surface par pipeline toote points (chat/voice/overlay/browser/app-open/automation/builder/settings) with file:line; (b) surfaces verdict table (code-complete / toota / bana hi nahi / grant chahiye); (c) UI se unreachable surfaces ki alag flag list.
- Ye sections audit report aane ke baad isi spec me append honge, phir Batch 2/3 ke design se cross-check honge.

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

## 9. Pre-audit (implement se PEHLE — is spec ke baad, isi file me append hoga)

- [ ] Parser patterns vs maujooda branches: koi duplicate/override nahi (hook sirf recognized pattern par, warna null).
- [ ] `planConfirm` state declaration order (use se pehle) + `voiceAppConfirm` se naam-collision nahi.
- [ ] `VoicePlan*` naam-collision nahi (PlannerModels/PlanDraft se alag).
- [ ] Koi PlannerEngine/PlanTool/Room change nahi (builder plans untouched).
- [ ] Naya service helper sirf YouTube chain me istemal (general surface nahi).
- [ ] Koi naya permission/manifest change nahi (CAMERA/A11y pehle se maujood).
- [ ] Unit tests ka naam + jagah tay (VoicePlanParserTest).
