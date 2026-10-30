# FLORA ZERO-REMAINING PLAN — v2 (LIVING, 2026-10-31)

**Order:** user order 2026-10-30 ~11:33 IST — "kya kya baaki hai, nahi hua
hai, chahe koi bhi state me ho ya koi bhi confirmation me — pura research
karke plan banao, plan ko upgrade karte rehna cycle ke saath jab tak 0 kuch
na chhute, confirmation round ke saath."
**Status: v2 LIVING.** Ye plan tab tak living hai jab tak ZERO na ho.
**Scope of this doc:** sirf plan/research. Is doc se koi code edit, push,
deploy, admin action, spend, message ya secret-write NAHI hota.
**Parent-only files chhue NAHI gaye:** FLORA_REMAINING_LIST.md aur
FLORA_DECISIONS.md sirf padhe gaye hain (read-only); ye doc local working
copy me hai, repo par push parent ke faisle se hoga.

## v2 changelog (2026-10-31, CEO cycle — user order 00:48 IST "sab ko ek
saath deploy karo, Builder workflow theek karo, plan me miss add karo")

**Moves (purani state → nayi state, 2 evidences ke saath):**
- D1, D2, D3 (IN-PROGRESS → LANDED): Section C DONE code-side
  (coordinator 21:37 IST 2026-10-30) + R4-8 plan-header fix commit
  37131454 (CI run #314 SUCCESS, release debug-build-314 live).
  Phone verification F6 me baaki hai (isliye ZERO nahi).
- D4 thermal (SPEC-PENDING → LANDED): coordinator 21:37 IST ke
  "no-access+thermal 4/4" gate + wahi R4-8 header fix; v1 ka "spec
  likha hi nahi gaya" note superseded hai. Phone test F6 me baaki.
- B4 admin-approve scope (UNVERIFIED → CLOSED-pack-decision): is cycle
  ki commit-history evidence — admin-approve ka aakhri tip change
  a90f0f37 (2026-10-30 10:19 IST, S1-1 revival fix) session #1
  (06:05–07:07 IST) ke BAAD ka hai; tip == local (20,682 B, blob
  b2a94f4c6acd). Faisla: session #2 me INCLUDE (paste-check list
  me) — paste-from-tip idempotent-safe hai (live pehle se barabar
  hua to no-op, nahi to fix).
- S3-5 (CLOSED-DOCS local-only → CLOSED tip+CI): Server Cycle 128
  commit 254bb6fe (push_tokens.sql SUPERSEDED marking tip par) +
  CI run #313 (41109870210) SUCCESS. Server OPEN 14 → 13 PAKKA
  (Cycle 129 mandatory poora hua).
- R4-8 repo portion → DONE-CODE (§4 me darj): commit 37131454 +
  CI #314 SUCCESS + naye tip se re-audit PASS.
- R4-7 → VERIFY-CLOSED (§4 me darj): BuilderScreen 553 lines me
  generate/AI/deploy 0 hits + PlannerBrain.kt:62 generate via
  PlannerEngine :271/:752 (builder scout 2026-10-31, local copy +
  R4-7 verify-close se match).

**Pack reconciliation (v1 §3B vs Cycle 123 6-function list):** dono
aadhe the. Session #1 ke baad badle hue function files ka poora union
(commit a90f0f37 + d3bdf7aa + 0ebc79f4 ki file lists se) = 10
functions (admin-approve, brain-proxy, licence-verify, push-token-sync,
admin-licences, admin-plan-features, home-sync, licence-key, tts-proxy,
usage-count) + stt-proxy (naya) + admin-list (version skew) +
send-push (local working-copy only) = 13 paste-check items + SQL
pehle. §3B v2 me FINAL hai.

**Added rows (§3K, 12 naye):** K1 thin RELEASE APK build, K2 flora-apk
bucket + read policy, K3 workflow upload step, K4 version.json
apkUrl+sha256 fill, K5 update-prompt live demo, K6 Builder plan B4
file-precision, K7 feature-flags CORS comment scope, K8 STT live
verify, K9 TTS live probe, K10 Gmail OAuth + deploy + removal gate,
K11 Maya-parity checklist mapping, K12 ex-Maya users commercial terms.

**Stale-note corrections (§0.1 me items 9–13):** H8 "web-UI only"
STALE hai (workflow-scope token 2026-10-30 ~19:40 IST se; API pushes
0f21c002/4c17f535 se verified); public-settings 401-vs-200 probe
method ka farq hai (headerless POST 401 gateway gate, anon-key probe
200 upi_vpa null — koi contradiction nahi); version.json apkUrl +
sha256 khali hain (channel pending, §3K).

## 0. Method — sources padhe gaye (guess par kuch nahi)

- FLORA_REMAINING_LIST.md (37 entries, poora padha) — parent single-writer.
- FLORA_DECISIONS.md (tail + deploy-session + UTR sections) — append-only.
- FLORA_TEAM_SERVER_CYCLES.md (Cycle 119–124 sections; Cycle 124 lines
  24314–24355 poora padha) — freshest server evidence.
- FLORA_TEAM_APP_CYCLES.md (tail: B7 re-check + deploy-prep ~06:10 IST).
- FLORA_SD_IMPLEMENTATION_PLAN.md (SD-P0..P6 headings + dependency map).
- FLORA_FULL_SCOPE_PLAN.md (parent doc pass ke baad ka state).
- FLORA_PERCENT_GAP_PACK_20261030.md (362 lines, poora).
- FLORA_IMPLEMENTATION_AUDIT.md (128 lines, poora; snapshot tip 1de5dfa0).
- FLORA_LIVE_AUDIT_20261030.md (142 lines, poora; snapshot ~09:21 IST).
- FLORA_PHONE_TEST_PLAN_v15.md (FINAL, 177 lines, poora).
- FLORA_NOACCESS_FUNCTIONS_PLAN.md (Phase 0 SPEC ONLY, poora).
- FLORA_P1_APPEARANCE_SPEC_RECORD.md (34 lines — declaration record).
- Item 36 teachonce plan (477 lines) + server spec (sd-p0/, 394 lines,
  DRAFT ONLY marking) — headings + gates.
- G10 design options doc (275 lines — options A/B/C + safety baseline).
- Deploy session #1 execution package (171 lines — session #1 ka prep,
  session #2 ka pack NAHI hai; session #2 pack = Cycle 122 + Cycle 123
  tip versions, neeche §3B me).
- SD coordinator final synthesis 2026-10-30 (parent ko deliver hua —
  SD-P1 8/8 gates + freeze-gap confirms ka source).
- Noaccess coordinator (running, 2026-10-30 ~11:27 IST order) — uska
  plan doc upar padha gaya hai; uska kaam yahaan duplicate NAHI hai.
- v2 sources (2026-10-31): Server Cycle 128 entry (S3-5 tip close,
  commit 254bb6fe, CI #313 SUCCESS); App cycle R4-8 entry (commit
  37131454, CI #314 SUCCESS, release debug-build-314); server-shift
  log Phase 0/1/2 COMPLETE entries + Phase 2 audit (heavy-lib KEEP,
  release command, channel pending); CEO live-verification scout
  (website 8/8 + version.json 200, Supabase 11/11 401 + stt/gmail
  404, 2026-10-31 ~00:50 IST); CEO builder scout (BuilderScreen 553
  lines 0 hits, PlannerBrain.kt:62, PlannerEngine :271/:752,
  2026-10-31); GitHub API fresh reads (tip 37131454, runs
  #312/#313/#314 success, post-session-#1 file-change union).

## 0.1 Source disagreements — jahaan sources takraye (freshest primary jeetta hai)

1. **Server OPEN count:** Full Scope §1B/§6.2 abhi bhi "OPEN 18 (Cycle 122)"
   kehta hai; Cycle 124 (fresh GitHub API finals #266/#267) kehta hai
   **OPEN 14 (P0 0, P1 2, P2 12)**. Freshest jeetta hai: **14**.
   Full Scope ka 18 ab stale hai (agli upgrade me sudhaar note).
2. **Remaining list item 10** abhi bhi "OPEN 44 (Cycle 119)" kehta hai —
   Cycle 124 ke 14 se stale hai. List parent-only hai, isliye yahaan
   sirf DOCS row (H14) banayi gayi hai, list edit NAHI ki gayi.
3. **App cycles log tail ~06:10 IST par ruka hai** (deploy prep), jabki
   SD batches uske baad land hue. App track ke liye freshest evidence
   commits + CI runs + SD synthesis hai, app log tail NAHI.
4. **Implementation audit snapshot tip 1de5dfa0 (~10:35 IST)** ke
   "NOT VERIFIED (CI pending)" verdicts (SD Batch 4, Lane B Batch 1)
   baad ke SUCCESS runs (#262, #261) + gates se supersede ho gaye.
5. **Live audit §5 ke stale points 1–4, 6** (prep doc S2-9, list items
   2/4/27/31, Full Scope counts/NEVER, B5_CI_PENDING) parent doc pass
   2026-10-30 se fix ho gaye — ab CLOSED-DOCS (§4 me). Uske §5.5
   (item 37 wording trap) aur §4 ke kuch gaps abhi bhi khule hain (§3H).
6. **Percent-gap pack ka v15 DRAFT** parent ne finalize kar diya
   (docs/FLORA_PHONE_TEST_PLAN_v15.md FINAL) — draft status khatam.
7. **Deploy session #1 package ka send-push source** (docs/fcm-pending
   draft) Cycle 122 finding se SUPERSEDED hai — sahi source local
   working-copy `supabase/functions/send-push/index.ts` hai (§3B3).
8. **Item 7 list text** ("67 tests, 8 FAILED", ~01:20 IST run) baad
   ke green runs se stale hai — par count abhi bhi UNCITED hai (§3H7);
   stale text ko green proof mat samajhna, aur purane fail ko
   current fail mat samajhna.
9. **S3-5 v1 me CLOSED-DOCS tha (local marking), tip par unmarked
   tha** — Cycle 128 ne tip-vs-local diff se pakda (sirf 4-line
   header ka farq) aur tip par marking pahunchayi (254bb6fe, CI
   #313 SUCCESS). v2 me S3-5 poori tarah CLOSED hai; "local note =
   closed" ka shortcut dobara mat lagana — tip par verify karo.
10. **§3B ka 9-function pack vs Cycle 123 ka 6-function pack** —
    dono aadhe the (v1 ke 9 me brain/tts/home-sync the jo Cycle 123
    ne nahi chhua tha; Cycle 123 ke 6 me admin-approve nahi tha).
    Sahi set = session #1 ke baad badle hue files ka union (§3B v2).
11. **H8 "web-UI only" STALE hai** — token 2026-10-30 ~19:40 IST ko
    workflow scope ke saath replace hua; workflow-file API pushes
    (0f21c002, 4c17f535) se verified hain. H8 ka unblocker ab
    "Contents API PUT pattern" hai, web-UI-only nahi. (H8 ki state
    DOCS-PENDING hi hai — sirf raasta badla hai.)
12. **public-settings 401 vs 200 contradiction NAHI hai** — headerless
    no-auth POST par Supabase gateway 401 deta hai (yehi watcher +
    CEO scout ne dekha); anon-key ke saath probe par function 200
    deta hai (upi_vpa null, session #1). Probe method likhe bina
    status compare mat karo.
13. **version.json apkUrl + sha256 khali hain** (live 200, vCode 45)
    — ye koi fail nahi hai, channel pending hai (§3K K2–K4). App
    is haal me download page par bhejega, update prompt ka live
    demo iske bina nahi ho sakta (K5).

## 0.2 State legend — STRICT, kabhi conflate nahi karna

- NOT-STARTED — kaam shuru hi nahi hua, koi gate bhi nahi laga.
  (Is v1 me standalone NOT-STARTED: KOI NAHI — har unstarted item
  ka ek naam wala gate hai, isliye wo apne gate ki state me hai.)
- SPEC-PENDING — spec freeze hona baaki hai (SPEC-FIRST items).
- IN-PROGRESS — coordinator running, spec/code chal raha hai.
- CODE-COMPLETE — code likha, CI/gate baaki.
- CI-GREEN — CI run SUCCESS, gate declaration baaki.
- GATE-PASSED — re-audit gate declare hua, deploy/phone baaki.
- LANDED — gate-passed + tip par (code sense me poora).
- DEPLOYED-LIVE — live environment par deploy hua.
- PHONE-VERIFIED — user ke phone par evidence ke saath pass.
- HELD — likhit wajah se ruka hai (decision/gate ka intezar).
- USER-GATED — sirf user ka action/input kholega.
- DECISION-PENDING — exact sawaal ka jawab chahiye (neeche darj).
- SPEND-PENDING — explicit spend approval chahiye (build ≠ spend).
- SEQUENCE-GATED — predecessor poora hone par hi shuru hoga.
- DOCS-PENDING — sirf doc/note/marking baaki hai.
- UNCITED-EVIDENCE — kaam hua ho sakta hai, par citation nahi hai.
- **Rules (binding):** CI green ≠ landed; landed ≠ deployed; deployed ≠
  phone-verified. HELD items REMAINING gine jayenge jab tak unka
  decision na aaye — chupchaap drop kabhi nahi. DROPPED/CLOSED sirf
  recorded user/CEO decision ke saath (§4).

## 1. Counts by state — v2 (106 remaining rows)

| State | Count | Rows |
|---|---|---|
| HELD | 17 | A1–A13, C6, D5, G15, J1 |
| SEQUENCE-GATED | 21 | B1, B2, B3, B5, C8, C11–C16, E4, E5, F1, F14, F15, F17, K3, K4, K5, K8 |
| USER-GATED | 24 | C5, E1, E2, E3, E6, F2–F13, G1, G2, G4, G7, K1, K2, K9 |
| DECISION-PENDING | 18 | F16, G3, G5, G6, G8, G9, G10, G16, H1, H2, H4, H5, H15, H16, H18, K7, K10, K12 |
| SPEC-PENDING | 8 | C1–C4, C9, G12, H17, H19 |
| UNVERIFIED | 5 | C7, G11, G13, G14, G17 |
| DOCS-PENDING | 9 | H8–H14, K6, K11 |
| IN-PROGRESS | 1 | H6 |
| SPEND-PENDING | 2 | C10, H3 |
| UNCITED-EVIDENCE | 1 | H7 |
| **TOTAL REMAINING** | **106** | |
| PHONE-VERIFIED (remaining me) | 0 | — yehi zero ka matlab hai (§5) |

**Headline v2:** 106 rows baaki hain (v1 ke 99 se: −5 moves
[D1–D4 LANDED, B4 CLOSED] + 12 naye §3K rows). Sabse bada hissa ab
bhi user-gated (24) + sequence-gated (21) + decision-pending (18)
hai — matlab zyada kaam kisi decision, user action, ya predecessor
ka intezar kar raha hai, na ki bina owner ke pada hai. LANDED items
(SD-P1 8 batches, item 34, item 37, server Cycle 122–124 items,
no-access D1–D4) phone-verified NAHI hain, isliye wo "closed" nahi
gine gaye — unki verification rows F3/F4/F5/F6 me USER-GATED hain.
CLOSED/DROPPED items §4 me alag hain (count me nahi).

## 2. Frozen — kya freeze ho chuka hai, kya nahi

**FROZEN (locked, dobara kholne ki zaroorat nahi):**
- Freeze-gap closure: SD final synthesis 2026-10-30 se likhit confirms —
  SD-4/SD-1/SD-6 gaps batch me CLOSE hue; SD-8 me 8.3/8.4/8.5/8.6/8.7/
  8.8/8.12/8.14 CLOSE hue; held rahe 8.9/8.13 (SPEC-FIRST), 8.10
  (spec freeze pending), 8.11 (dedicated batch); item 36 PARTIAL →
  app part CLOSE, §1.4 held, relay SD-P2.
- Item 37 spec v2 (735 lines, Accessibility-only) — frozen.
- Noaccess Phase 0 spec (Batch A/B/C + exclusions) — frozen (spec doc).
- UTR rule: exactly 12 digits numeric (user-confirmed 2026-10-30).
- Licence ₹199/month FINAL; Free 600s/day cap = limit, bug nahi.
- Deploy order (session #2): usage_atomic_increment.sql PEHLE, phir
  functions, phir send-push (working-copy se).
- B7 rule: re-check se pehle landed → final APK; slipped → SD-P6
  vNext; publish kabhi SD se block nahi hota.
- CEO D1–D6 (D4 "saare tools", D2 Piper DROPPED, D3 store-scan-only
  FINAL, D4 publish trigger, D5 emoji interim nahi, D6 G18 gate).
**NOT FROZEN (spec/decision baaki — §3 me rows hain):** SD-3 product
shape, SD-P5 Lane B contract, 8.9/8.10/8.11/8.13 specs, C7 pairing,
Market definition, full Gmail OAuth spec, G12 disambiguation, item 37
planner-step contract, thermal mitigation spec.

## 3. Har remaining item — ek row (id, state, evidence, owner, unblocker, next cycle)

Evidence column me commit / CI run / log line hai. Jahaan evidence
nahi hai, wahaan state UNVERIFIED hai — promote nahi kiya gaya.

### §3A. Server OPEN 13 (Cycle 128 CONFIRMED — S3-5 CLOSED tip+CI #313; P0 0 / P1 2 / P2 11)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| A1 | S1-5: 15-day rule confirmation-gated me badalna | HELD | Server log Cycle 124 HELD re-check; header STANDING RULE | Lane B | Owner design decision (G9): auto-transfer ko confirmation-gated banana hai ya nahi | Cycle 125 |
| A2 | S1-6: gate increment kare (double-count risk) | HELD | Cycle 124 HELD re-check; header: gate sirf padhta hai | Lane B | App reporting reliability ka design decision | Cycle 125 |
| A3 | S1-26 automation leg: SwissTransfer expiry watch | HELD | Cycle 122 narrowed + Cycle 124 HELD; static page automate nahi ho sakta | Lane B | Watcher/cron decision, 13 Nov 2026 se pehle | Cycle 125 |
| A4 | S1-17: tts model aura-asteria-en English-only | HELD | Cycle 124 HELD re-check | Lane B | Provider/model decision | Cycle 125 |
| A5 | S1-27: licences.device_id NULL-able ALTER | HELD | Cycle 124 HELD re-check; deploy session me run nahi hua | Lane B | Dashboard decision/session (ALTER chalana) | Deploy #2 |
| A6 | S1-30: licence-key guess rate limit DB store | HELD | Cycle 124 HELD re-check | Lane B | Rate-limit DB store design | Cycle 125 |
| A7 | S2-3: unlinked token-ownership proof | HELD | Cycle 124 HELD re-check; JWT-mandatory se app registration toot sakta hai | Lane B | Token-ownership design decision | Cycle 125 |
| A8 | S3-2: device-binding enforcement | HELD | Cycle 124 HELD re-check; brain device_id compare nahi karta, tts leta hi nahi | Lane B | Device-binding design decision (AI-gate behavior badlega) | Cycle 125 |
| A9 | S3-3: usage-count JWT-mandatory | HELD | Cycle 124 HELD re-check; app Wave B device_id bhejta hi nahi | Lane B | JWT-mandatory design decision | Cycle 125 |
| A10 | S3-4: origin allowlist | HELD | Cycle 124 HELD re-check; `*` posture barkaraar | Lane B / parent | Parent se Vercel host list (origin guess kabhi nahi) | Cycle 125 |
| A11 | S4-3: admin-licences create supersede-fail par 200+warning+full key | HELD | Cycle 124 HELD re-check | Lane B | Owner faisla: auto-revert vs single RPC | Cycle 125 |
| A12 | S6-1: licence-key self-reveal status guard | HELD | Cycle 124 HELD re-check | Lane B | LFMS faisla: 400/404 vs dead-key warning | Cycle 125 |
| A13 | S6-2: brain/tts gate fail-open DB error par | HELD | Cycle 124 HELD re-check; fail-open header me documented design choice hai | Lane B | Owner faisla: fail-closed karna hai ya documented fail-open rakhna hai | Cycle 125 |

### §3B. Deploy session #2 (parent-browser dashboard; pack = Cycle 122 + Cycle 123 tip versions)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| B1 | SQL: usage_atomic_increment.sql SABSE PEHLE | SEQUENCE-GATED | Cycle 122 Batch 2 (unique index + increment_usage RPC); Cycle 124 pack UNCHANGED | Parent-browser | Deploy session #2 start (user ka deploy wala tap — offer pending) | Deploy #2 |
| B2 | 13 paste-check items, tip versions (har paste se pehle dashboard-editor header-compare; farq ho tabhi paste, baad me 401 gate verify) — SQL B1 ke baad: (1) Cycle 123 ke 6: licence-verify, usage-count, push-token-sync, admin-licences, admin-plan-features, licence-key; (2) Cycle 122 Batch 2 ke 3: brain-proxy, tts-proxy, home-sync; (3) admin-approve (B4 faisla: INCLUDE); (4) admin-list (repo v2/v3 vs live v1 skew — INCLUDE, recommendation §3B note); (5) stt-proxy (NAYA deploy, tip version); (6) send-push SIRF local working-copy `supabase/functions/send-push/index.ts` se (tip par maujood hi nahi — 22 tip functions vs 23 local; draft file se KABHI nahi) | SEQUENCE-GATED | CEO cycle 2026-10-31: commit file-lists a90f0f37 + d3bdf7aa + 0ebc79f4 ka union (session #1 ke baad badle hue 10 functions) + tip-vs-local identity checks (admin-approve/admin-list/stt-proxy/gmail-proxy/SQL tip == local) + tip tree (send-push absent) | Parent-browser | Deploy session #2 (B1 SQL pehle, phir ye 13; gmail-proxy is session me KABHI nahi — 501 draft, OAuth decision K10 par pending) | Deploy #2 |
| B3 | send-push redeploy, LOCAL working-copy se | SEQUENCE-GATED | Cycle 122: `supabase/functions/send-push/index.ts` hi sahi source; docs/fcm-pending draft PURANA | Parent-browser | Deploy session #2 (draft se redeploy KABHI nahi) | Deploy #2 |
| B4 | admin-approve redeploy scope — RESOLVED v2: INCLUDE (paste-check list me) | CLOSED-pack-decision | CEO cycle 2026-10-31: admin-approve ka aakhri tip change a90f0f37 (2026-10-30 10:19 IST, Cycle 122 Batch 1, S1-1 website-path NULL-row status filter revival fix) session #1 (06:05–07:07 IST) ke BAAD ka hai; tip == local (20,682 B). Live-vs-tip barabari bahar se verify nahi ho sakti (koi version endpoint nahi), isliye paste-check hi faisla karega: dashboard content tip se alag hua to paste (fix), barabar hua to no-op — dono haal me safe | Lane B / parent | Koi alag unblocker nahi — B2 ki paste-check list me shamil hai | Deploy #2 |

**§3B note (v2, admin-list recommendation):** admin-list ka aakhri
tip change 2026-10-29 ka hai (session #1 se pehle), phir bhi cycle
logs live ko v1 aur repo header ko v2/v3 kehte hain. Recommendation:
INCLUDE — kyunki Vercel panel tip se auto-deploy hota hai (live panel
pehle se tip panel hai), aur function hi ek matra hissa hai jo sirf
dashboard paste se update hota hai; panel-naya/function-purana skew
ko session #2 hi band karega. Paste se pehle header-compare zaroori
hai (barabar hua to skip + verify note). Ye recommendation hai,
parent ka faisla antim hai — par bina admin-list ke session #2
"poora" nahi kahlayega, ye baat darj hai.
| B5 | Deploy verify criteria (OPTIONS 200 + ACAO `*`; home-sync dusre user 403 / same 200; revoke/reveal non-active 400; licence-key list total+truncated; brain 503/502; usage RPC increment; send-push no-auth 401 + panel {ok:true,message_id}; web duplicate-UTR warning; step-confirm "baaki hai"; bina-checkbox signup rok; admin audit bina RLS error) | SEQUENCE-GATED | Cycle 122 deploy pack verify section | Parent-browser | Deploy session #2 ke verify steps | Deploy #2 |

### §3C. SD track (SD-P1 done siva held sub-items; baaki phases sequence-gated)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| C1 | SD-8 8.9 Marketing | SPEC-PENDING | SD synthesis: records me sirf "PENDING" mention, behaviour detail nahi | SD track | Behaviour spec freeze (SPEC-FIRST, guess par build nahi) | SD resume |
| C2 | SD-8 8.13 Touch Guard | SPEC-PENDING | SD synthesis: SPEC-FIRST, detail nahi | SD track | Behaviour spec freeze | SD resume |
| C3 | SD-8 8.10 Whiteboard-full | SPEC-PENDING | SD synthesis: spec freeze pending | SD track | Full-scope spec freeze | SD resume |
| C4 | SD-8 8.11 drawer/widgets | SPEC-PENDING | SD synthesis: dedicated batch chahiye (drawer refactor + AppWidget receiver ke apne gates) | SD track | Dedicated batch spec + gates | SD resume |
| C5 | SD-1 Drive sub-item | USER-GATED | SD Batch 2: Drive DISABLED (item 14 note); SD plan: Drive item 14 OAuth ka intezar | User | Drive OAuth registration poora karna | User action |
| C6 | SD-1 Vercel deploy-trigger | HELD | SD synthesis: placeholder-file deploy user ki live site badal sakta tha; connection-check only | SD track | Deploy-trigger design decision (safe trigger ka raasta) | SD resume |
| C7 | SD-6 Maya task-list parity detail | UNVERIFIED | SD synthesis: 178 records me per-behaviour record nahi; koi bada parity claim nahi kiya gaya | SD track | Per-behaviour parity record ya "parity claim nahi" ka likhit faisla | SD resume |
| C8 | SD-P2: SD-2 OpenRouter third leg (Gemini→Groq→OpenRouter) | SEQUENCE-GATED | SD plan §2 SD-P2: held session ke baad alag deploy; drafts sd-p2/ me hain | Lane B | Deploy session #2 ke baad alag SD-P2 deploy | SD-P2 |
| C9 | SD-P2: SD-3 hosting-provisioning-as-product | SPEC-PENDING | SD plan: product shape SD-P0 spec me freeze hoga; price kabhi invent nahi | Lane B / user | Product shape approval + user se price (user de tabhi) | SD-P2 |
| C10 | SD-P2: item 36 whatsapp-template-send (1 call = 1 recipient/template/send; rate 30/24h + burst 5/10min; 10-min confirmation freshness; audit hash+last-4 only) | SPEND-PENDING | Item 36 server spec (sd-p0/, 394 lines, DRAFT ONLY); Bundle C: SPEND NOT APPROVED | Lane B | Explicit spend approval (alag se; build approval spend approval nahi) + SD-P2 deploy | SD-P2 |
| C11 | SD-P3 neural track: 8.1 STT (Deepgram), 8.2 TTS picker, 8.15 Voice Guardian port, 8.16 neural wake, 8.17 dataset, item 36 voice part (hf_alpha) | SEQUENCE-GATED | SD plan §2 SD-P3: engine + validation run 1 ke baad; drafts sd-p3/ 01–06 + hold-map | Lane C | Validation run 1 + Gate 6 ke baad (Deepgram code user se bhi chahiye — G2) | SD-P3 |
| C12 | SD-P4: SD-5 general automation (tap/type/swipe) + item 36 upgrade path | SEQUENCE-GATED | SD plan: Phase 10 publish ke BAAD, queue jump nahi | Lane A | Phase 10 publish | SD-P4 |
| C13 | SD-P5 LAST: SD-7 Home hardware track + G10 revisit (box button/family share/manual fallback/Bluetooth+WiFi/sequential/voice+app control, 4/8 channel, relay terminals, setup, demo loads; bill prediction bina accuracy claim; order/coupon price kabhi invent nahi) | SEQUENCE-GATED | SD plan §2 SD-P5 AAKHRI; G10 doc options A/B/C; prior-owner hole Cycle 122 S1-8 se code-side band (403 guard), command path abhi bhi NAHI hai | Lane B + A | Last-hop clear + Lane B contract (saare numbers) + nayi clearance + Pehle-Jaanch + post-session alag deploy | SD-P5 |
| C14 | SD-P6 verification + release (slip hue batches vNext; publish block nahi) | SEQUENCE-GATED | SD plan §2 SD-P6; B7 rule | Parent / SD track | SD-P1..P5 ke baad | SD-P6 |
| C15 | PC client (item 18, P9-1) | SEQUENCE-GATED | Percent-gap Track 4 SPEC DONE; G13 phone-half `PcLinkScreen.kt` maujood; OS user se confirm hoga (pin nahi) | Lane A | Phone APK publish (uske baad hi build) | Post-publish |
| C16 | Publish bundle (item 19) | SEQUENCE-GATED | Percent-gap Track 1B checklist DRAFT DONE; CEO D4: gates + sign-off = authorisation | Parent / Lane D+A | Phase 10 gates pass + user phone sign-off (G4) | Phase 10 |

### §3D. No-accessibility functions + thermal (user orders 2026-10-30 ~11:27 + ~11:33 IST)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| D1 | Batch A: time/date/day, battery %, device info, storage, RAM, network type, Wi-Fi state read, alarm/timer/dial/settings intents (A1–A11) | LANDED | Section C DONE code-side (coordinator 21:37 IST 2026-10-30) + R4-8 plan-header fix commit 37131454 (CI #314 SUCCESS, status IMPLEMENTED + GATE PASS) | Noaccess coordinator | Phone test F6 me baaki | Phase 10 |
| D2 | Batch B: torch on/off (CAMERA permission, flash check, fail-closed, process-local state caveat) | LANDED | Section C DONE code-side (coordinator 21:37 IST 2026-10-30) + R4-8 plan-header fix commit 37131454 (CI #314 SUCCESS) | Noaccess coordinator | Phone test F6 me baaki (phone par CAMERA grant user se) | Phase 10 |
| D3 | Batch C: volume read/set, ringer normal/vibrate, DND silent (Notification Policy Access grant-gated) | LANDED | Section C DONE code-side (coordinator 21:37 IST 2026-10-30) + R4-8 plan-header fix commit 37131454 (CI #314 SUCCESS) | Noaccess coordinator | Phone test F6 me baaki (DND ke liye user ka grant) | Phase 10 |
| D4 | Thermal management: phone garam ho to Flora ko pata chale + manage kare (status read, mitigation, warning, Auto Log) | LANDED | Section C DONE code-side (coordinator 21:37 IST 2026-10-30: "no-access+thermal 4/4") + R4-8 plan-header fix commit 37131454 (CI #314 SUCCESS); v1 ka "spec likha hi nahi gaya" note superseded hai — implementation tip par hai | Noaccess coordinator | Phone test F6 me baaki | Phase 10 |
| D5 | Deferred v2 candidates: Bluetooth state read, own-window brightness | HELD | Noaccess plan §2: documented deferral (BLUETOOTH_CONNECT + wizard row chahiye; window plumbing chahiye) | Noaccess coordinator | v2 decision (wizard row / plumbing) — v1 ko saaf rakhne ke liye abhi nahi | v2 |

### §3E. Neural engine + Gate 6 (Lane C code-LANDED, phone evidence baaki)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| E1 | Validation run 1: Cases 1–6 (4 speak paths, attach-fail, >509 tokens, interrupt/stop, in-call, inference timing) | USER-GATED | Neural protocol (538 lines): 7 cases sab PENDING-PHONE-EVIDENCE; model SHA local copies se match (onnx 7d5df8ec…a6c5, voices bca610b8…f7d, ~354 MB runtime download only) | User (phone) | Phone par run 1 execution + evidence (stopwatch timing haath se) | Phase 10 |
| E2 | Case 7 approval (user approval checkpoint) | USER-GATED | Protocol: user apne shabdon me in-app sample par ("Kokoro hf_alpha lagi" + "manzoor karta hun") — reference sample/CI green approval NAHI | User | User ke apne shabdon me approval, Case 1–6 ke BAAD hi | Phase 10 |
| E3 | Step 1 dono-UI-states check (Settings status button: juda nahi / jud gaya) | USER-GATED | Protocol §8 Step 1 (commit 574fd227); dono states phone par unverified | User (phone) | Phone par dono states ka evidence (screenshot + line text) | Phase 10 |
| E4 | Gate 6: Lily removal (system TTS hatana) | SEQUENCE-GATED | Lane C FINAL: Gate 6 HELD/CLOSED-shartein; system TTS abhi har jagah (Lily profile pitch 0.95/rate 0.88) | Parent / Lane C | E1 + E2 + E3 + Phase 10 pass — chaaron poore hon tabhi | Post-Phase 10 |
| E5 | G2P open flags: पहनना nn vs nː (Path B O8), C2=ɦ tension (Path A run 1), शहर test, O2/O3/O4/O5/O7 validation-open | SEQUENCE-GATED | G2P spec v0.2 (567 lines) + fix a426359a; espeak VM me NAHI hai (verified) | Lane C | Validation run 1 (Path A run 1 se faisle) | Phase 10 |
| E6 | N6 licence-additive LIVE line (key row par hi key path, keyless rows untouched) | USER-GATED | Code-read VERIFIED-CLOSED (Lane D 23:46 IST); live PENDING; licence-verify Cycle 122 Batch 1 + Cycle 123 Commit B me badla → live run se pehle naye tip par contract re-read zaroori | User (JWT) + parent | User JWT + fresh-tip contract re-read, Phase 10 me | Phase 10 |

### §3F. Phase 10 (execution 0% — prep poora hai, execution user/phone par hai)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| F1 | Final APK build (CI `:app:assembleDebug` green + artifact, commit + run evidence) | SEQUENCE-GATED | Percent-gap Track 1B step 1; published abhi v2.22 (112,590,325 B, sha 6987e625…e09) purana hai | Lane A / parent | SD-P1 + noaccess batches ke CI green + gate ke baad build | Phase 10 start |
| F2 | v15 phone tests (screens, permissions ek-ek karke, keys, tools, SOS, face, docs, connect, routines, gaadi, talk-limit 600s = limit not bug, backup restore) | USER-GATED | v15 FINAL (177 lines); v14 SUPERSEDED | User (phone) | User ka phone waqt + F1 ka APK | Phase 10 |
| F3 | Item 37 §10 measurements (latency 200–400ms, loop 300–600ms, fps 2–5, secure-window verify, Accessibility-OFF, no-per-session-dialog, Vivo kill/restart, battery/heat) | USER-GATED | v15 §3b; item 37 LANDED 0a9af82c + 030fd95c, gate PASS; §10 UNVERIFIED | User (phone) | Phone par measurements + saare safety checks PASS | Phase 10 |
| F4 | Item 34 phone checks (age gate teeno paths, Settings switch, wizard card + greeting, restore fail-closed, memory controls) | USER-GATED | v15 §3c; item 34 LANDED 27380f1c, gate PASS (a)–(j); phone UNVERIFIED | User (phone) | Phone par checks | Phase 10 |
| F5 | SD-P1 8 batches ka phone verification (orb swap, charm drag/tap/remove, edge-glow states, SD-1 asli accounts par, task-list reboot, SD-8 toggles, P1/P2/P3 actions, vault/identity flows) | USER-GATED | SD synthesis: phone-verified 0% (gates code+CI+tip scope ke hain) | User (phone) | Phone par har batch ka test | Phase 10 |
| F6 | Noaccess phone tests (spec §6 ke 10 steps: time, battery, storage/RAM, Wi-Fi state/settings, torch grant/deny, volume 50% + step, DND grant/deny, alarm prefill, dialer, Auto Log entries) | USER-GATED | Noaccess plan §6 (claim nahi) | User (phone) | D1–D3 land hone ke baad phone par test | Phase 10 |
| F7 | Admin unblock: app band → reopen → login (wahi account) → re-link → users row → SIRF us row par is_admin=true → /admin verify | USER-GATED | Deploy session #1: users row 0 rows (UID 1cde8e7b…); percent-gap Track 5 checklist DRAFT | User (phone) + parent (dashboard SQL) | User ka re-link + "re-link ho gaya" batana | Phase 10 |
| F8 | /admin verify + panel keys set (admin-settings se) | USER-GATED | Live audit §2: panel keys user hi set karega; admin access ke baad | User | F7 ke baad | Phase 10 |
| F9 | /pay UPI set (admin-settings upi_vpa) — P0 LIVE payment blocker | USER-GATED | Anon-key re-probe SETTLED: public-settings 200 `upi_vpa: null, status: "unset"` (LIVE confirmed) | User (admin) | F7 ke baad admin-settings se UPI set karna | Phase 10 |
| F10 | 2× ₹199 pending payments ka faisla (28 + 29 Oct, UTRs TEST-like) | USER-GATED | List item 29 (M2); standing rule: real UTR match ke bina approve KABHI nahi | User (asli UTR) + admin | Asli UTR evidence + is_admin (F7) | Phase 10 |
| F11 | N4 key-path LIVE test (JWT gate: no Bearer 401, JWT≠body user 403, missing/foreign key identical invalid_key) | USER-GATED | Code-gate confirm session #1; live behaviour JWT ke bina nahi | User (JWT) | User JWT, Phase 10 me | Phase 10 |
| F12 | N5 D8 server-gate LIVE test (Pro allow; Free <600s warna 429; gate sirf padhta hai, increment nahi) | USER-GATED | Code-gate confirm session #1; live cap-behaviour JWT ke bina nahi | User (JWT) | User JWT, Phase 10 me | Phase 10 |
| F13 | S1-15 panel push test (send-push redeploy condition) | USER-GATED | Cycle 122 recount: condition poori (session #1 send-push redeploy), panel push test Phase 10 verify-pending | User / parent | Phase 10 me panel se push test | Phase 10 |
| F14 | Publish: SwissTransfer NEW upload + download flip (naya version/size/SHA/package) + website live verify | SEQUENCE-GATED | Percent-gap Track 1B steps 6–7; CEO D4 | Parent | Phase 10 gates pass + G4 sign-off | Phase 10 |
| F15 | 10x publish check + 4-part delivery (admin/server/website/app) | SEQUENCE-GATED | Percent-gap Track 1B step 8 | Parent | F14 ke baad | Phase 10 |
| F16 | SwissTransfer Option A: purani v2.22 link EXTEND (Option B = F14 se alag kaam) | DECISION-PENDING | Percent-gap Track 1C; deadline **13 Nov 2026**; spend faisla alag se chahiye | User | User ka faisla 13 Nov 2026 se pehle (agar publish usse pehle pakka na ho) | User decision |
| F17 | Phase 7 E2E licence flow (signup → link → approve → verify) | SEQUENCE-GATED | Decisions 07:07 IST: G18 pass is criterion ko poora NAHI karta; phone/admin path par pending | User + parent | F7 + F11/F12 + phone path | Phase 10 |

### §3G. User-gated / decision / verify-pending (list items + follow-ups)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| G1 | Grievance officer ka asli naam (fake kabhi nahi) | USER-GATED | List item 14; decisions: user-gated hi rahenge | User | Asli naam dena | User action |
| G2 | Deepgram security code (console code) | USER-GATED | List item 14; Deepgram sign-in flipify.com@gmail.com par security-code step par atka hai | User | Console code bhejna (Deepgram lane + 8.1 isi par) | User action |
| G3 | Dialer call-talk decision (item 23/N7, v15 §5) | DECISION-PENDING | List item 23: "pehle user bataye"; v15 §5 OPEN | User | User ka jawab: Flora ko default dialer banana hai ya nahi | User decision |
| G4 | Publish phone sign-off (khud user ka) | USER-GATED | CEO D4: gates + sign-off = authorisation (fresh 'publish karo' alag se nahi chahiye, sign-off user ka hi) | User | Phase 10 gates ke baad user ka sign-off | Phase 10 |
| G5 | Planner backup scope (planner plans backup me aayen ya nahi) | DECISION-PENDING | List item 14; B6 design: plans scope se BAHAR hain (design boundary) | User | User ka faisla: scope me laana hai ya B6 boundary rakhni hai | User decision |
| G6 | 013 FLAGs faisla | DECISION-PENDING | List item 14 (user-gated) | User | User ka faisla | User decision |
| G7 | G14 dashboard step (user dashboard step) | USER-GATED | List item 14; item 26 A-3 scope ambiguity ke saath | User | User ka dashboard step poora karna | User action |
| G8 | Background auto-execute rule (item 12 sub) | DECISION-PENDING | Live audit §4: user verbatim jawab chahiye | User | Verbatim jawab: confirmation-required + Auto-Mode-ON exception rakhna hai ya badalna hai | User decision |
| G9 | Item 34 O-1: startup migration (pre-gate stored romantic ko retroactively gate karna) | DECISION-PENDING | SD synthesis: alag decision chahiye (non-gating follow-up) | Parent / user | Alag decision | SD resume |
| G10 | Item 34 O-3: background-mic prefix (D-I34-2) | DECISION-PENDING | SD synthesis: prefix code me nahi mila; bina order ke koi kaam nahi | Parent / user | Order (prefix lagana hai ya nahi) | SD resume |
| G11 | Item 28 (M1): B6 P2-1/2/3 per-point fixes (partial state, patli audit-log entry, stale cached folder ID) | UNVERIFIED | Live audit §2 + implementation audit §3: A3 gate inhe automatically cover nahi karta jab tak explicitly fold na ho | Lane A | Fold-into-A3 vs separate batch ka faisla + per-point evidence | SD resume |
| G12 | Item 37 planner-step contract ("screen dekho" step) | SPEC-PENDING | List item 37: planner-step contract alag item hai | Lane A | Alag spec (planner step mapping) | SD resume |
| G13 | Whole-app MediaProjection removal (v2 spec ke baad bhi poore app me koi usage na rahe) | UNVERIFIED | List item 37: alag confirmed-decision item; v2 spec sirf spec supersede karti hai, poore app ka usage nahi | Lane A | App-wide grep confirm (MediaProjection hits 0) + record | SD resume |
| G14 | Item 6 D4 app-side text removal (LicenceClient "Transfer … ko hoga" message) | UNVERIFIED | List item 6: app-side Lane A queue me (A3 ke baad); B7 batches D4/D4-F1 hain par is message ka grep-confirm is plan me nahi mila | Lane A | Grep confirm: D4/D4-F1 batches ne ye message hataya ya nahi; nahi to Lane A follow-up | SD resume |
| G15 | Item 12 emoji interim (expressive phrasing + pitch/rate modulation) | HELD | CEO D5: abhi build item NAHI, Phase 10 evaluation item rahega | Parent | Phase 10 evaluation (neural expressive kaafi hua to interim drop, nahi to enhancement) | Phase 10 |
| G16 | Item 13 website-approve identification (S1-1 se match kare ya alag nikle) | DECISION-PENDING | List item 13 note; live audit §2 orphaned; U1 (H1) se juda | Parent / user | U1 confirm ya alag identification faisla | Cycle 125 |
| G17 | Item 17 (N1): Notch P2-1…P2-6 per-point fold confirm | UNVERIFIED | List item 17: A3 hardening batch ke saath; A3 LANDED (c2ed591e, gate PASS) par per-point fold ka explicit record is plan me nahi mila | Lane A | Per-point fold ka explicit record (ya alag follow-up) | SD resume |

### §3H. Decision bundle + evidence/docs leftovers

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| H1 | Bundle A: U1 website-builder DONE confirm | DECISION-PENDING | Cycle 124: U1 ABHI BHI PENDING (bundle A, owner input par); live pages 200 hona confirm NAHI hai | Parent / user | Owner input: U1 confirm (kya "saare tools" live + PAY-1/2/3 + step badges + checkbox + download wording + links — sab live hain, ye confirm karna hai) | Cycle 125 |
| H2 | Bundle B: SD-P0 confirmations (G10 numbers DRAFT-PROPOSED, last-hop A vs B, licence tiers, transfer/family-share) | DECISION-PENDING | SD plan dependency map; Cycle 124 bundle HELD | Parent / user | Explicit confirmations (licence tiers/prices kabhi invent nahi) | Cycle 125 |
| H3 | Bundle C: item 36 spend | SPEND-PENDING | Cycle 124: SPEND APPROVED NAHI; build approval spend approval nahi hai | User | Explicit spend approval (alag se) | User decision |
| H4 | N9 ruling: anon-upsert policy chahiye ya public-by-design intended hai | DECISION-PENDING | Session #1 dashboard-verify: Storage ZERO policies, 3 PUBLIC buckets; Cycle 122 ruling-ready note (codebase me Storage upload path NAHI, sirf DriveClient) | Lane B / parent | Ruling: public-by-design document karo (recommendation) ya scoped policy jab future feature client upload maange | Cycle 125 |
| H5 | S1-20 per-call cap number | DECISION-PENDING | Cycle 122: koi contract number nahi; RPC-first fallback implement hua (cap leg HELD) | User | User se cap number (mile tabhi usage-count me cap lagega) | User decision |
| H6 | G17 rotation watch (Vercel leaf Jan 27 2027, Supabase leaf Jan 22 2027) | IN-PROGRESS | Cycle 122/124: G17 unchanged, sirf indicator change par revisit; blocker nahi | Lane B | Watch jaari (koi action nahi jab tak indicator na badle) | Cycle 125 |
| H7 | Item 7 count citation: 67/67 | UNCITED-EVIDENCE | Percent-gap 2b: #257 Jobs API + console log se step-level PASS cite hua (`:app:testDebugUnitTest` BUILD SUCCESSFUL 1m 12s); count lines log me nahi aati; workflow test XML publish nahi karta (SD synthesis ne bhi confirm kiya: test-results/junit/xml 0) | Parent / Lane A | Test-report XML publish step (H8) ya `--info` count line + item 7 re-audit pass | Cycle 125 |
| H8 | Item 7 workflow test-report step (web-UI only) | DOCS-PENDING | Token ke paas workflows scope NAHI hai (trees/contents API 404, createCommitOnBranch FORBIDDEN) — sirf GitHub web UI se Edit + paste + Commit | Parent / user (web UI) | Web-UI se test-report publish step jodna (ya `--info` run ka faisla) | Web-UI |
| H9 | Docs/README Termux mentions ki safai | DOCS-PENDING | Percent-gap 2d: app/ tree grep 0 (code-tree close); docs mentions ki safai alag docs-only faisla; audit-log entries rewrite/delete NAHI hongi | Parent | Docs-only faisla (kaun se mentions hatane hain) | Confirmation upgrade |
| H10 | G12 list text update (ruling R-A2-2 se) | DOCS-PENDING | Implementation audit §2 item 9: ruling + GoalOrchestrator code maujood; list item 26 ka text stale hai | Parent (single-writer) | List item 26 ka text ruling se update karna | Confirmation upgrade |
| H11 | Route-parity checklist (45 destinations vs 54 screens — NOT-A-MISS, par paper checklist nahi mili) | DOCS-PENDING | Implementation audit §2 item 8: COVERED as miss; paper checklist file nahi mili (documentation kaam, feature miss nahi) | Parent | Route-by-route checklist file banana (ya "checklist nahi chahiye" ka faisla) | Confirmation upgrade |
| H12 | SD-P0 drafts discoverability (drafts `apk-recovery/sd-p0/` me, docs/ me nahi) | DOCS-PENDING | Implementation audit §4: future reader sirf docs/ dhoondhega to miss karega | Parent | Index/pointer note (sd-p0 drafts ka docs/ se link) | Confirmation upgrade |
| H13 | Item 37 list wording trap (LANDED ke baad "shuru nahi hui" wali line — sirf v1-spec removal ka context hai) | DOCS-PENDING | Live audit §5.6; list item 37 ka aakhri vakya | Parent (single-writer) | List me context note jodna (quote sirf context ke saath) | Confirmation upgrade |
| H14 | Remaining list item 10 recount text (abhi bhi "OPEN 44" kehta hai) | DOCS-PENDING | List item 10 (Cycle 119 text) vs Cycle 124 OPEN 14 (§0.1.2) | Parent (single-writer) | Item 10 ka text Cycle 124 ke 14 se update karna | Confirmation upgrade |
| H15 | Market module definition (Maya side-menu "Market" — stock-price tool DONE hai, module scope undefined) | DECISION-PENDING | Implementation audit §2 item 1: NEEDS-DEFINITION (C1) | Parent / user | Definition: Market sirf stock-price tha ya usse aage (uske baad hi build item banega) | User decision |
| H16 | C7 music-app pairing definition (favourite-song storage P3 LANDED hai; pairing alag hai) | DECISION-PENDING | Implementation audit §2 item 7: pairing NEEDS-DEFINITION; SD synthesis: C7 abhi bhi pending definition hai | Parent / user | Pairing ka definition (kaun se apps, kaise pair hoga) | User decision |
| H17 | Full Gmail OAuth spec (P2 me sirf notification-snippet reading hai) | SPEC-PENDING | SD synthesis: P2 me email notification-read + mailto reply hai; full Gmail OAuth spec pending definition hai | SD track | Full Gmail OAuth spec (consent + read/reply scope) | SD resume |
| H18 | Voice-layer trained-model decision (turn/addressee detection model) | DECISION-PENDING | Implementation audit §2 item 5: COVERED (named+partial), trained-model decision baaki; Hunter/Pocket ke Flora equivalents ko fresh spec chahiye, copy kabhi nahi | Parent / user | Trained-model decision (banana hai ya nahi, kaun sa approach) | User decision |
| H19 | Wake-word sub-TODO split + 24×7 mic disclosure TODO (8.16/8.17 bundle-level par hain) | SPEC-PENDING | Implementation audit §2 item 6: sub-TODO split baaki; 24×7 mic disclosure-UI verification TODO parent ko Phase 10/8.16 par jodna hai | SD track | Sub-TODO split + disclosure TODO ka spec | SD resume |

### §3J. Deferred (user decision se taal diya gaya — drop NAHI hai)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| J1 | Item 30: G10 control endpoint (command path) — registration-only barkaraar | HELD | User decision 2026-10-30 ~00:58 IST: "Control ko defer karo, drop nahi"; G10 doc (275 lines, options A/B/C, safety baseline MUST); prior-owner hole code-side band (Cycle 122 S1-8, 403 guard), command path abhi bhi NAHI hai | Parent / Lane B | Last hop clear (phone→box ya box firmware) + nayi clearance + Pehle-Jaanch + deploy session ke BAAD alag deploy (SD-P5 me, C13 ke saath) | SD-P5 |

### §3K. Server-shift leftovers + Builder workflow + update channel + Maya-parity (v2 additions, CEO cycle 2026-10-31)

| ID | Item | State | Evidence | Owner | Unblocker (exact) | Next cycle |
|---|---|---|---|---|---|---|
| K1 | Thin RELEASE APK build (arm64-only + R8, vCode 45 base) | USER-GATED | Server-shift log Phase 2 OPEN: Gradle build subagent exec-confirmation timeout par atka (daemon start par mar gaya); taiyaar command project root se `JAVA_HOME=~/workspace/build-tools/jdk-17.0.21+9 ~/workspace/build-tools/gradle-8.7/bin/gradle :app:assembleRelease --console=plain`; release UNSIGNED banega (keystore invent mana hai); Play Store update KABHI NAHI (key lost); 32 MB ka vaada NAHI (math: arm64-only native ~49 MB + R8 shrink) | User-attention session | User-attention session me command chalana (subagent retry se wahi timeout aayega, isliye is cycle me attempt NAHI kiya gaya) | User session |
| K2 | flora-apk public bucket + read policy (Supabase dashboard/SQL) | USER-GATED | version.json live 200, apkUrl + sha256 KHALI; SQL docs/FLORA_APK_DIRECT_DOWNLOAD.md me maujood; bucket PUBLIC EMPTY hai (50 MB cap vs ~112 MB APK — purana route DEAD tha, nayi policy ke saath dobara dekhna hoga) | Parent (dashboard) | Dashboard/SQL se bucket + read policy lagana | Deploy #2 ke saath |
| K3 | CI workflow me release-APK upload step | SEQUENCE-GATED | Server-shift coordinator correction: "API se push NAHI hoga" STALE hai — workflow-scope token 2026-10-30 ~19:40 IST se, API pushes verified; step API se jud sakta hai, web-UI-only nahi | Lane A / parent | K2 (bucket URL/host) ke baad step likhna — bina host ke step likhna guess hoga | K2 ke baad |
| K4 | version.json me apkUrl + sha256 bharna | SEQUENCE-GATED | version.json notes field khud kehta hai: bucket + upload lagte hi isi file me bharna hai (ek chhota web push); app badle bina update chalu ho jayega | Parent / Lane A | K1 + K2 + K3 ke baad (release APK ka asli SHA-256) | K1–K3 ke baad |
| K5 | Update-prompt ka live demo (Settings "Update check karo" → versionCode compare → download + SHA-256 verify → install prompt) | SEQUENCE-GATED | Phase 2 Item 3 code tip par hai (ffce6fbc, CI #312 SUCCESS); demo tabhi hoga jab version.json me 45 se bada versionCode + apkUrl bhara jaye; KOI auto-check/download/install nahi — har kadam user ke tap par | User (phone) | K4 + phone par demo | Phase 10 |
| K6 | Builder plan B4 ki file-precision (FLORA_BUILDER_PLAN.md B4 section me PlannerBrain/PlannerEngine ka naam + line citation nahi hai) | DOCS-PENDING | Builder scout 2026-10-31: plan doc me `BuilderScreen` 0 mentions (galat nahi hai), par B4 ko "brain-proxy-chain code-writing + planner engine" kehta hai bina file naam ke; sahi files: PlannerBrain.kt:62 generate via PlannerEngine :271/:752; BuilderScreen 553 lines me generate/AI/deploy 0 hits | Parent / Lane A | B4 section me file:line citation jodna (code change nahi, doc precision hi hai) | Confirmation upgrade |
| K7 | feature-flags CORS comment ka scope (index.ts:39–46 "CORS abhi NAHI laga, S2-1 QUEUED" vs usi file ke CORS_HEADERS + OPTIONS preflight) | DECISION-PENDING | App cycle R4-8 ne server domain keh kar chheda NAHI; comment ka scope (is function vs poora S2-1 item) saaf nahi hai | Server team | Server team se clarification: comment kis scope ka hai, aur S2-1 ka status kya hai | Cycle 129 |
| K8 | STT lane live verify (stt-proxy deploy ke baad JWT success-path probe) | SEQUENCE-GATED | stt-proxy tip par hai (d9c000e5, repo-only, live 404 expected); batch REST v1, JWT + licence gate; success-path asli user JWT ke bina UNVERIFIED hai | Parent-browser + user (JWT) | Session #2 me stt-proxy deploy + user JWT se live probe | Deploy #2 ke baad |
| K9 | TTS lane live success-path probe (tts-proxy + Deepgram key) | USER-GATED | tts-proxy v3 live hai (401 gate); Deepgram key ke bina lane 503 → system fallback (yehi design hai); success-path user JWT + key ke bina UNVERIFIED hai | User (JWT + Deepgram code) | Deepgram security code + user JWT | Deploy #2 ke baad |
| K10 | Gmail lane: OAuth decision + gmail-proxy deploy + app-side Gmail option hatane ka gate | DECISION-PENDING | gmail-proxy DRAFT ONLY (tip par hai, OAuth config na ho to 501, koi Google call nahi); design doc FLORA_SERVER_GMAIL_DESIGN.md; app-side option is round me NAHI hata (gate: server replacement live verify ke baad) | User | Gmail/Google OAuth decision (Cloud project, consent screen, gmail.readonly scope, redirect URI, gmail_tokens table) + live verify ke baad hi app-side option hatana | User decision |
| K11 | Maya-parity checklist mapping (2026-10-30 ~23:42 wali Maya vs Flora comparison list ko plan rows se map karna) | DOCS-PENDING | User direction 2026-10-31 ~00:04 IST: target Maya-parity hai; checklist = research list (server/app split + Maya server/app + farq); is plan me abhi parity items rows se jude nahi hain | Parent / CEO | Comparison list ke har point ko row-id se jodna (ya "mapping nahi chahiye" ka faisla) | Confirmation upgrade |
| K12 | Ex-Maya users ke commercial terms (free/discount/nayi khareed + list/payment-proof ka source + price) | DECISION-PENDING | User-stated context: Maya ke liye pay karne wale users app maang rahe hain; unki list/payment-proof ka koi record user ke paas nahi (dashboard access gone); Flora ₹199/month FINAL hai — ex-Maya users par yehi lagega ya kuch aur, ye faisla baaki hai | User | User ka faisla (terms + list/proof source + price) — iske bina unhe kuch deliver mat karo | User decision |

## 4. CLOSED / DROPPED reference (count me NAHI — sirf record ke liye)

- **CLOSED v2 (2026-10-31, CEO cycle):** S3-5 push_tokens.sql
  SUPERSEDED marking (commit 254bb6fe + CI #313 SUCCESS — tip par
  verify, local-only note ab nahi); R4-8 repo portion DONE-CODE
  (commit 37131454 + CI #314 SUCCESS + re-audit PASS — no-access
  plan header, syncPushToken KDoc, CONNECTOR.md v2 shape);
  R4-7 VERIFY-CLOSED (Builder AI route planner path hai, missing
  nahi — BuilderScreen 553 lines 0 hits, PlannerBrain.kt:62 via
  PlannerEngine :271/:752); B4 pack decision CLOSED (admin-approve
  session #2 me INCLUDE, §3B v2); server-shift Phase 0/1/2 code
  DONE (runs #305–#312 SUCCESS; Phase 3 = Phase 10 + sign-off →
  publish); no-access D1–D4 LANDED (phone verification F6 me baaki,
  isliye ZERO nahi).
- **CLOSED (code+gate):** A1 battery+One-Tap, A2 app part, A3 hardening,
  B5-02, B5-04, D4 server-side, S1-10 code/repo, N8 (server + live
  session #1), G18 (parent code-level cross-check 07:07 IST), B7
  re-check READY (10/10 batches), SD-P1 8 batches (LANDED — phone
  pending, isliye §3F5 me verification row hai), item 34 (LANDED —
  §3F4), item 37 (LANDED — §3F3), Cycle 122/123/124 server items
  (LANDED repo — deploy §3B me).
- **CLOSED-DOCS (parent pass 2026-10-30, grep-verified):** prep doc
  §1A-1 S2-9 location fix; list items 2/4/27/31 updates; Cycle 3
  CLOSE declaration (decisions log 13/13, 0 NEW, stop MET); Full
  Scope OPEN 18 + §5.4 SUPERSEDED + §1B note (ab §0.1.1 ke hisaab
  se 18 bhi stale hai — 14 sahi hai); B5_CI_PENDING RETIRED note;
  v15 FINAL (v14 SUPERSEDED note ke saath); S3-5 push_tokens.sql
  SUPERSEDED/DO-NOT-RUN note (grep 1 hit); P1 spec record
  (FLORA_P1_APPEARANCE_SPEC_RECORD.md — declaration ka record,
  original spec artifact ka daava nahi).
- **CLOSED (evidence-settled):** anon-key re-probe (6/6 functions
  expected responses; headerless 401 gateway ka tha — koi regression/
  bypass nahi); N11 code-tree (grep 0); N6 code-read (live §3E6 me);
  route 45-vs-54 as miss (NOT-A-MISS — 54 screens vs 45 destinations);
  G12 as miss (ruling R-A2-2 + code maujood — list text §3H10 me).
- **DROPPED (recorded decision se):** item 31 Piper fallback (CEO D2,
  2026-10-30 ~00:59 IST); silent features / password store / purani
  keys-data-signing reuse (NEVER, standing); silent auto-reply/
  posting (NEVER, G19); web password typing (B1 guard); Play Store
  (signing key LOST = fact, sideload-only); Free Fire autopilot /
  aimbot / mod-menu (research-only, implementation NOT authorized).
- **DEFERRED, not dropped:** J1 (G10 control) + item 2 ka G10 control
  UI deferral (item 30 me) + D5 emoji interim (§3G15).
- **DEPLOYED-LIVE (session #1, server+panel):** 5 SQL + 21 functions +
  reset-password redirect URL (Total URLs: 1) + N8 live strings +
  N9 answer (ZERO policies). Admin part Phase 10 me pending (§3F7).

## 5. CONFIRMATION-ROUND PROTOCOL (standing rule — har upgrade par lagu)

Ye protocol is plan ke har upgrade par lagu hai, aur user ke order
ke mutabiq plan tab tak upgrade hota rahega jab tak ZERO na ho.

**Upgrade triggers (inme se koi bhi ho to upgrade banta hai):**
1. Har coordinator cycle completion ke baad (SD / Lane B / noaccess /
   audit / koi bhi team cycle).
2. Har deploy session ke baad (dashboard paste + verify).
3. Har user decision ke baad (koi bhi decision bundle item, cap
   number, sign-off, ya user action poora hone par).
4. Har naye tip move / CI final result ke baad jo kisi row ki state
   badal de.

**Har upgrade cycle ke 5 steps (koi step skip nahi):**
- **(a) Fresh re-verify EVERY item ki state primary evidence se:**
  GitHub API runs (head SHA + conclusion), tip file contents (grep),
  cycle logs (appended sections). Watcher ticks AKELI evidence kabhi
  nahi (wo sirf signal hai, gate nahi); memory summaries akeli
  evidence kabhi nahi; purani cycle ki report akeli evidence kabhi
  nahi (dobara verify karo ya "carried, re-verify pending" likho).
- **(b) Move ONLY items jinki evidence badli ho.** Koi item guess,
  andaze, ya "lagta hai ho gaya" par move nahi hoga. Evidence na
  badli ho to row waisi ki waisi rahegi, chahe kitne upgrade ho jayein.
- **(c) Two-evidence rule:** kisi item ki state promotion ke liye
  2 independent evidences chahiye (jaise: CI green + gate declaration;
  ya: commit diff + CI run; ya: dashboard verify + log entry).
  Ek evidence se sirf "CI-GREEN" ya "CODE-COMPLETE" tak ja sakte
  ho, LANDED tak nahi. Phone-verification ke liye phone evidence +
  re-audit gate chahiye (server/code evidence phone ka substitute
  kabhi nahi).
- **(d) Version bump + dated changelog:** har upgrade par version
  badhega (v1 → v2 → …) aur changelog me likha jayega: kya move
  hua (row + purani state → nayi state + 2 evidences), kya add
  hua (naye rows + source), kya confirm hua (bina move ke re-verify
  hue rows). Changelog ke bina upgrade maana hi nahi jayega.
- **(e) Report of moved/added items ONLY:** upgrade report me sirf
  wahi items aayenge jo move hue ya add hue (poori 99-row table
  dobara sunana nahi hai). Bina move/add ke upgrade par sirf
  "koi move nahi, plan vX barkaraar" likha jayega.

**ZERO ki paribhasha (binding):** ZERO tabhi hai jab har row ya to
PHONE-VERIFIED ho ya formally CLOSED/DROPPED ho recorded user/CEO
decision ke saath. LANDED, DEPLOYED-LIVE, GATE-PASSED, CI-GREEN —
koi bhi ZERO nahi hai. HELD rows ZERO me tabhi aayengi jab unka
decision aa jaye aur wo implement ho ya formally drop ho. Jab tak
ek bhi row baaki hai, plan LIVING hai aur cycle jaari hai.

## 6. Top blockers (is v1 ke hisaab se, kram me)

1. **Admin unblock (F7) — sabse bada single blocker:** iske bina UPI
   set (F9, P0), payment review (F10), panel keys (F8), N4/N5 live
   tests (F11/F12), S1-15 panel push test (F13) — sab ruke hain.
   Unblocker: user ka app restart + re-link + "re-link ho gaya".
2. **Phone waqt (F2–F6, E1–E3):** Phase 10, neural run 1, item 37 §10,
   item 34, SD batches, noaccess — sab phone evidence par ruke hain.
   Koi server/code evidence inka substitute nahi hai.
3. **Deploy session #2 (B1–B5) + B4 scope sawaal:** repo LANDED
   items live nahi hain jab tak dashboard paste na ho. B4
   (admin-approve scope) deploy se pehle saaf hona chahiye.
4. **SwissTransfer deadline 13 Nov 2026 (F16 + A3):** extend vs
   new-upload ka faisla + expiry-watch automation leg — deadline
   se pehle faisla nahi aaya to link toot sakta hai.
5. **Decision bundle (H1–H5, H15–H18, G3/G5/G6/G8–G10/G16):**
   15 decision-pending rows — inke bina SD-P0 confirmations, N9
   ruling, cap number, G10 numbers, licence tiers sab ruke hain.
6. **Spend approvals (C10 + H3):** item 36 relay + spend — bina
   explicit approval ke SD-P2 ka server hissa aage nahi badhega.
7. **Item 7 citation (H7 + H8):** chhota kaam hai par "67/67" ka
   daava iske bina kabhi cite nahi hoga — web-UI step + re-audit
   chahiye.
8. **Thermal spec (D4):** naya order hai, spec abhi likha hi nahi
   gaya — noaccess track me pehle spec, phir pre-audit, phir
   implement (Pehle-Jaanch kram toota nahi jayega).
**v2 note (2026-10-31):** blocker 8 ab nahi raha — D4 LANDED hai
(phone test F6 me baaki). v2 ke naye top blockers: (a) thin RELEASE
APK build (K1, user-attention session); (b) update channel K2–K5
(bucket + workflow step + version.json fill + demo); (c) deploy
session #2 — pack ab FINAL hai (§3B v2: SQL pehle + 13 paste-check
items), sirf parent-browser session ka intezar hai; (d) ex-Maya
users ke terms (K12) — iske bina unhe kuch deliver nahi hoga.

## 7. Upgrade log

- **v1 — 2026-10-30:** Pehla poora zero-remaining plan. Sources §0 me
  darj hain. 99 remaining rows (§3A–§3J) + closed/dropped reference
  (§4) + confirmation-round protocol (§5). Agla upgrade: pehle
  trigger par (§5 triggers) — v2 me sirf moved/added items report
  honge.
- **v2 — 2026-10-31 (CEO cycle, user order 00:48 IST):** Moves: D1–D4
  → LANDED, B4 → CLOSED-pack-decision, S3-5 → CLOSED (tip+CI),
  R4-8 → DONE-CODE, R4-7 → VERIFY-CLOSED. Added: §3K K1–K12.
  Pack reconciliation: §3B FINAL (SQL pehle + 13 paste-check items).
  Stale corrections: H8 web-UI-only → API pattern; public-settings
  probe-method note; version.json channel note. Counts: 99 → 106
  (−5 moves + 12 naye). Sources + disagreements §0/§0.1 me darj
  hain. Agla upgrade: deploy session #2, K1 build, ya koi user
  decision aate hi.
