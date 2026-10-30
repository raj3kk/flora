# Flora Fix-Cycle Register — user order 2026-10-30 ~13:26 IST

**Order:** jitni bhi dikkat user ne batayi hai, sab par audit → research → deep find → fix → fix par re-audit → test ka cycle chalega, bina ruke, jab tak 0 na mile. 5-minute restart watch stall pakad kar cycle dobara chalu karegi.

**States:** OPEN (kaam baaki) / IN-PROGRESS (kisi coordinator par) / DONE-CODE (code+CI+re-audit gate pass, phone verification baaki) / DONE-PHONE (Phase 10 phone test pass) / USER-GATED (user ke phone/grant/decision ke bina band nahi ho sakta — cycle ise force nahi karegi) / BLOCKED (3 fix attempts ke baad bhi na bana — wajah ke saath, cycle har round me wapas aayegi).

## A. Chal rahe kaam (doosre coordinators par — inhe chhedo mat jab tak wo chal rahe hain)

| # | Item | State | Owner |
|---|---|---|---|
| A1 | Planner routing — har command planner se (intention → plan → execute → verify → report) | IN-PROGRESS | planner-fix coordinator |
| A2 | Workflow audit — har surface ka execution raasta (chat/voice/overlay/browser/app-open/automation/builder/settings) | IN-PROGRESS | planner-fix coordinator |
| A3 | Automation surfaces ki list + verdicts (app-open, routines, in-app search, automation service, builder execution) | IN-PROGRESS | planner-fix coordinator |
| A4 | YouTube chain: kholna → search → pehla result play (pehla vertical/test case) | IN-PROGRESS | planner-fix coordinator |
| A5 | Builder entry missing (debug APK me builder dikha hi nahi) — entry/wiring | IN-PROGRESS | planner-fix coordinator |
| A6 | Confirmation rule: samajh pehle → plan → confirmation sirf ek baar, shuru me → poora execute (Auto Mode ON par seedha execute) | IN-PROGRESS | planner-fix coordinator |
| A7 | Full-app design pass: sizes, headers, options, dropdowns (saare options), samajhne layak labels, sections divided, koi option hidden nahi | IN-PROGRESS | design coordinator |

## B. Queued code-fixable items (master cycle coordinator lega, order me)

| # | Item | Source | Note |
|---|---|---|---|
| B1 | Background echo guard missing (Flora ke bolte waqt listener stop nahi hota → self-trigger loop ka shak) | Debug-APK audit §2 | FloraApp.kt chhoone se pehle planner coordinator ke complete hone ka intezar (parent register me mark karega) |
| B2 | Licence per-reply server round-trip se slowness (har gated reply par freshStatus) | Debug-APK audit §1 | Spec me tradeoff likhna: fresh verify vs cache — guess par cache mat lagao, pehle spec + pre-audit |
| B3 | STT mishearing: sirf pehla result (MAX_RESULTS=1), hi-IN par Hinglish mix galat | Debug-APK audit §3/5 | Deepgram key user-gated hai; system-STT sudhaar (alternatives/transcript visibility) code-side |
| B4 | Crash-log export missing (crash.log sirf file me, Auto Log me nahi, export button nahi) | Debug-APK audit §8 | In-app crash-log view/export feature (naya UI, design pass se takkar nahi) |
| B5 | 600s/day cap par "band ho gayi" lagna — cap message + usage display saaf nahi | Debug-APK audit §1 | Cap by design hai; messaging/visibility sudhaar, cap hatana NAHI |
| B6 | Voice-first Baat view me transcript chhupa (STT vs brain error alag nahi dikhta) | Debug-APK audit §3 | Agar design pass ise cover kar le to yahan se hatao (dedupe, dobara mat karo) |

## C. DONE code-side (phone verification Phase 10 me baaki)

- Call/notification announce: 4/4 batches GATE PASS (spec FLORA_CALL_NOTIFY_ANNOUNCE_PLAN.md; tip 4e4c5a91).
- No-accessibility functions + thermal: 4/4 batches GATE PASS (spec FLORA_NOACCESS_FUNCTIONS_PLAN.md).

## D. USER-GATED (cycle force nahi karegi — exact input list ke saath OPEN rahenge)

- Accessibility ON + Auto Mode ON (phone Settings, user khud karega)
- Vivo auto-start + battery-unrestricted ON (user khud karega)
- Gemini/Groq key phone Settings me (haan/na hi batana; value kabhi nahi) + Deepgram sign-in security code (user bhejega)
- App-link + admin (new APK + re-link → users row → is_admin sirf us row par)
- Publish sign-off (Phase 10 gates + user phone sign-off)
- 2× ₹199 asli UTR evidence (12-digit numeric match)
- Deploy session #2 (parent browser; Cycle 123 tip versions; B4 admin-approve scope pehle saaf)
- Phase 10 phone tests (v15 FINAL) — koi bhi phone result server evidence se confirm nahi hoga
- Decision bundle (U1, SD-P0 confirmations, G10 numbers/last-hop, licence tiers, item 36 spend alag explicit) + S1-20 cap number
- Server OPEN 14 (Cycle 124 confirmed) — Cycle 125 design input ya deploy session ke baad
- SwissTransfer faisla 13 Nov 2026 se pehle (extend vs new upload)

## E. Scope tracker reference (duplicate nahi)

- Zero-remaining plan v1: docs/FLORA_ZERO_REMAINING_PLAN.md (99 rows; har cycle me upgrade + confirmation rounds, two-evidence rule).
- Server cycles log: docs/FLORA_TEAM_SERVER_CYCLES.md (Cycle 124 tak).

## Cycle rules (har item par)

1. Audit (code evidence file:line) → 2. Research (kaise hona chahiye, alternatives) → 3. Deep find (root cause, symptom nahi) → 4. Fix (Pehle-Jaanch: spec → pre-audit → implement → CI → re-audit gate) → 5. Fix par re-audit → 6. Fixed ho to agla item + register update; na ho to wapas step 3 (max 3 attempts, phir BLOCKED + wajah + agla item; blocked items par har round me wapas aao).
2. Kabhi [skip ci] nahi, workflow files API se nahi, koi spend nahi, koi nayi API key nahi, koi silent action nahi, 4 stops (paisa/delete/login/genuine block) hamesha upar.
3. Phone-verified ka daava sirf Phase 10 phone evidence par; CI green ≠ landed (re-audit gate decide karega).
4. Progress marker: har code progress ke baad `~/workspace/goals/maya-app-private-rebuild/hidden_files/fix-cycle-progress` me timestamp + item update karo (restart watch isi se stall pehchanegi).

## Round log

### Round 1 — 2026-10-30 (Phase 1: Section B audit + research + spec)
- Spec: docs/FLORA_FIX_CYCLE_SPECS.md (B1–B6: audit evidence file:line + research + deep find + fix design + pre-audit checklist + phone-test steps). Is round me koi code change nahi (DOCS ONLY push).
- Implementation order: B3 → B4 → B5 → B2 → B1 → B6 (B1/B6 planner DONE-CODE ka intezar; B6 par design dedupe check; B2 B3–B5 ke baad).
- Section F: Round 1 me sirf tracked (kaam Section B ke baad); F2/F5/F13 ke parent-browser/web-UI hisse parent ke paas rahenge; Section D force nahi hoga.
- Progress marker: hidden_files/fix-cycle-progress me darj hai.

## F. CEO-ADDED full-stack scope (user order 2026-10-30 ~13:29 IST — CEO perspective se miss hui cheezein)

Cycle ab sirf batayi hui dikkaton par nahi, PURE stack par chalega: server, database, backend, frontend, website, workflow, automation, execution — sab ka pura workflow. CEO (parent) dekhte rahega, cycle rukega nahi. Ye items user ne naam lekar nahi bole the, par CEO review me mile hain — inhe bhi wahi audit → research → fix → re-audit → test cycle lagega. Jo user-gated hain wo force nahi honge.

### Server / Database
- F1: Server OPEN 14 (Cycle 124 confirmed) — Cycle 125 design input ya deploy session ke baad; cycle me active rakho, bhoolo mat.
- F2: Deploy session #2 — SQL usage_atomic_increment SABSE PEHLE, phir Cycle 123 tip functions + send-push sirf local working-copy se; B4 admin-approve scope pehle saaf karna hai.
- F3: Supabase Storage ZERO policies (N9) — codebase me Storage upload path nahi hai isliye anon-upsert zaroorat nahi, par public-by-design document + ruling pending hai.
- F4: S2-9 partial index deploy HOLD (combined deploy session me) + send-push function tip par missing (sirf local working-copy + dashboard live) — source hygiene gap.
- F5: N4/N5 JWT live tests pending (code-gate confirm ho chuka hai, live JWT test baaki hai).

### Backend / Functions
- F6: G10 command path abhi bhi nahi hai (prior-owner hole code-side band hai, par command endpoint + home_commands + app UI ka raasta design stage me hai).
- F7: FCM push-token sync + send-push ka live E2E (device token → function → delivery) phone par unverified hai.

### Frontend / App
- F8: Item 7 ka 67/67 count UNCITED hai (CI workflow test-report XML publish nahi karta) — citation ka raasta cycle me rakho (web-UI test-report step ya --info count run).
- F9: Neural engine residuals — .ok marker ka content validate nahi hota, in-call routing UNVERIFIED, model download resume nahi hai (3 attempts/file), per-class counts CI log me nahi aate.
- F10: Backup scope gap — flora_plans.db backup scope se bahar hai (B6 design) — scope decision pending hai.
- F11: 013 FLAGs + F3-SURF queued items — cycle me nazar me rahen.

### Website
- F12: Website 8 pages live 200 par user phone visual check pending hai + /pay UPI unset hai (public-settings me upi_vpa null LIVE) — admin unblock ke baad set hoga.

### Workflow / CI
- F13: CI workflow 35 lines — test-report publish nahi karta (F8 se juda hai); workflow files API se push nahi hoti (token workflows scope nahi) — workflow badlaav web UI se hi hoga, ye cycle me note rahe.
- F14: Har deploy par live-route gate (/, /api/app/version, /login 200) — framework:null wale empty-deploy lesson se; READY/PROMOTED par bharosa kabhi nahi.

### Execution / Verification (cycle ka antim gate)
- F15: Phase 10 phone verification 0% hai — bina phone evidence ke production-ready ka daava kabhi nahi banega; ye cycle ka antim gate hai.
- F16: SD-P2 (server part; whatsapp-template-send par SPEND approval alag se) → SD-P3 → SD-P4 → SD-P5 → SD-P6 sequence cycle me rahe.
- F17: Grievance officer deferred post-launch (fake naam NEVER) + Deepgram key validity pending — legal/key risks cycle ki nazar me rahen.
