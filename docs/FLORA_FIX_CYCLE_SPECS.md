# Flora Fix-Cycle Specs — Round 1 (Section B, Phase 1: audit + research + spec)

**Order (user order 2026-10-30 ~13:26 IST):** audit → research → deep find → fix → fix par re-audit → test, bina ruke, jab tak 0 na mile.
**Register:** `docs/FLORA_FIX_CYCLE_REGISTER.md` (source of truth; is file ka har update usi me darj hoga).
**Phase 1 = DOCS ONLY** (is push me koi code change nahi hai — code collision ka sawaal hi nahi).
**Phase 2 order:** pehle B3, B4, B5 (FloraApp.kt ko nahi chhoote) → phir B2 (LicenceGate + LicenceClient only, FloraApp.kt nahi) → B1 + B6 tabhi jab parent register me planner coordinator ko DONE-CODE mark kar de (dono FloraApp.kt chhoote hain) + B6 ke liye design-pass dedupe check.
**Hard rules:** kabhi `[skip ci]` nahi; workflow files API se nahi; koi spend nahi; koi nayi API key nahi; koi silent action nahi; 4 stops (paisa, delete/irreversible, user login/2FA, genuine block) hamesha upar; upfront-once confirmation (confirmation sirf ek baar, shuru me); Auto Log har action par; phone-verified ka daava sirf Phase 10 evidence par; CI green ≠ landed (re-audit gate decide karega).

---

## B1 — Background echo guard missing (self-trigger loop ka shak)

**Audit (tip evidence):**
- `voice/BackgroundMicService.kt` — `engine.onSpeechStart` (lagbhag line 136–141) sirf `bgSpeaking = true` + `NotchState` set karta hai; `listener?.stop()` wahan nahi hai. `engine.onSpeechDone` par `startListening()` dobara chalta hai (pehle se maujood).
- `listener?.stop()` sirf do jagah hai: ACTION_STANDBY (lagbhag line 229) aur `onDestroy` (lagbhag line 560) — bolne/sunne ke raaste me kahin nahi.
- `handleHeard` (lagbhag line 306) me jawab bolne se pehle bhi listener nahi rukta.
- Foreground guard maujood par partial hai: `ui/FloraApp.kt` tip lines 263–274 (`isEchoLike`) — sirf **aakhri** Flora message se compare, 50%+ word-overlap ya contains par rokta hai.
- Planner loop bounded hai (`planner/PlannerModels.kt:133` MAX_STEPS = 20; attempts < 3; step timeout 120s) — loop planner se kam, voice-echo se zyada likely hai.
- `onError` par 600ms me listener restart hota hai (`BackgroundMicService.kt` lagbhag line 170–173) — STT network error repeat ho to restart churn badhega (ye B1 ka hissa nahi, note ke taur par darj hai).

**Research (alternatives):**
- (a) TTS ke dauraan listener ko poori tarah stop karo, TTS done par dobara start karo (foreground flow jaisa: MainActivity TTS done par dobara start karta hai).
- (b) Listener chalu rakho par SPEAKING window ke results ko drop karo (flag + grace period).
- (c) Sirf foreground guard ko mazboot karo (aakhri N messages se compare).
- Chuna gaya raasta: (a) background ke liye (sabse saaf — Flora ki awaaz kabhi input hi nahi banegi) + (c) foreground ke liye (aakhri 3 Flora messages tak compare, 50% rule waisa hi). (b) ko isliye nahi chuna kyunki stop/start ka raasta pehle se code me maujood hai (onSpeechDone → startListening) aur drop-window me asli user barge-in bhi mar sakta hai.

**Deep find (root cause, symptom nahi):** Header comment kehta hai "Flora ke bolte waqt sunna roka jata hai", par code me bolne ke dauraan listener zinda rehta hai — comment vs code mismatch. Background mic Flora ki apni awaaz speaker se sun kar use naya input samajh sakta hai → jawab → phir apni awaaz sunna → self-trigger loop. Yehi loop ka sabse bada shak hai.

**Fix design (implementation planner DONE-CODE ke baad):**
- `BackgroundMicService`: `engine.onSpeechStart` me `bgSpeaking = true` ke saath `listener?.stop()` (ya stopListening path jo `startListening` se dobara khul sake — pre-audit me `startListening`/`stop` ka exact pair verify karna); `onSpeechDone` ka maujooda `startListening()` waisa hi rahega.
- `FloraApp` (`isEchoLike`): compare window aakhri 1 se badha kar aakhri 3 Flora messages tak; threshold/rule wahi (contains ya 50%+ overlap). Callers (Deepgram FINAL + system interrupt FINAL + partial stop gate) pre-audit me verify karne hain — koi caller behavior change bina soche nahi.
- Koi naya permission, koi naya service, koi silent action nahi. Har guard-trigger Auto Log me aayega (pehle se jo events hain unke saath, naya spam nahi).

**Pre-audit checklist (implement se pehle):** `startListening()` ka exact behavior (recreate vs resume); `listener.stop()` ke baad `startListening()` dobara kaam karta hai ya nahi (ContinuousListener.stop destroy karta hai — dobara start par naya recognizer banta hai, ye verify karna); `isEchoLike` ke saare callers; `bgSpeaking` flag ke saare readers.
**Phone-test steps (Phase 10, claim nahi):** Background me Flora se lambi baat → apni awaaz par dobara jawab nahi aana chahiye; Auto Log me repeat entries nahi honi chahiye; foreground me Flora ki baat ka tukda sunai de to guard rokna chahiye.
**Attempts:** 0.

---

## B2 — Licence per-reply server round-trip se slowness

**Audit (tip evidence):**
- `data/LicenceGate.kt` — `freshStatus` har gated use par server verify attempt karta hai (013: 15-minute cache suppression hataya gaya — revoke/expire ke baad Pro 15 minute tak nahi chalna chahiye, isliye cache par bharosa karke gated faisla nahi hota). Fail-open: server na pahunche to last-known cached ya local cap.
- `data/LicenceClient.kt` — `post()` SHARED hai (verify, countUsage, syncPushToken, link, refresh, fetchServerUpi sab isi se): `connectTimeout = 15_000`, `readTimeout = 15_000` (lines 230–231). `verify()` `withContext(Dispatchers.IO)` me blocking `HttpURLConnection` chalata hai.
- Worst-case licence stall per reply: unlinked user = 1 post = 30s tak; linked user + expired access token = JWT refresh post (30s tak) + verify post (30s tak) = **60s tak**, brain shuru hone se pehle. (`SessionTokenCache` 55-minute reuse karta hai, isliye expired-token case hi do-post wala hai.)
- `assistant/BrainClient.kt:65` gate call karta hai; uske baad brain chain ke apne timeouts hain (postJson 20s/30s; discovery GET 15s) — wo B2 ka scope nahi hai, par slowness ka poora hisaab rakhne ke liye darj hai.

**Research (alternatives):**
- (a) Cache wapas lagao (15-min ya session cache) — REJECTED: 013 ka faisla jaanboojh kar tha (direct lane — user ki apni keys — ka ekmatra gate app gate hai; server gate sirf proxy lane cover karta hai). Cache lagana revoke promptness ko kamzor karega.
- (b) `withTimeout` se gate ko lapeto — REJECTED (akela kaafi nahi): blocking `HttpURLConnection` read coroutine cancellation ko nahi maanta, isliye timeout ke baad bhi gate socket ke peeche atak sakta hai. Asli bound sirf socket timeout se milega.
- (c) Gate-path ke posts par chhota socket timeout (verify/verifyWithKey par) — CHUNA GAYA: server reachable ho to fresh status waisa hi milega (013 semantics barkaraar); sirf slow/unreachable case ka worst-case ghat-ta hai, jo pehle se cached/local fallback par girta hai.
- (d) Verify ko background prefetch se garam rakho — REJECTED: gate ka faisla phir bhi seconds-purani status par hota, jo 013 ke letter ke khilaaf hai.

**Deep find (root cause):** Har reply ki shuruaat me ek unbounded (30s–60s worst-case) blocking network call hai, jiska timeout har caller ke liye ek jaisa 15s/15s hai — gate path ke liye koi alag bound nahi hai.

**Fix design (B3–B5 ke baad; FloraApp.kt nahi chhoota):**
- `LicenceClient.post()` ko timeout parameters do (defaults wahi 15_000/15_000 — baaki callers ka behavior bilkul nahi badlega).
- `verify()` / `verifyWithKey()` gate timeouts pass karenge: connect 5_000 / read 8_000 (pre-audit me Edge cold-start reality ke saath confirm karna — 8s read cold start ke liye kaafi hona chahiye; agar pre-audit me cold-start data isse zyada dikhe to number badha kar likhna, guess par nahi).
- Attempt 1 me refresh path ke timeouts defaults par rahenge (worst expired-token case 15+15+5+8 = 43s; typical case 8s tak). Agar re-audit me refresh dominate karta dikhe to follow-up: timeout ko cache ke through thread karna (alag attempt, alag gate).
- Fallback bilkul waisa hi: cached (fail-open) → local cap. Koi naya cache, koi plan change, koi server change nahi.

**Pre-audit checklist (implement se pehle):** `verify()` / `verifyWithKey()` ke saare callers (gate + session renew — FloraApp me "Licence session renew" kaunsa call use karta hai, ye verify karna; renew background hai, fail par agli baar retry hota hai — timeout change us par bhi lagu hoga, ye note karna); `post()` ke saare callers (koi caller 15s par nirbhar to nahi); Edge cold-start ka koi maujooda data (logs/docs) ho to number usse justify karna.
**Phone-test steps (Phase 10, claim nahi):** Slow network par reply ka wait pehle se kam lagna chahiye (exact second ka daava instrumentation ke bina nahi); reachable server par Pro/Revoke faisla turant waisa hi hona chahiye (koi regression nahi).
**Attempts:** 0.

---

## B3 — STT mishearing (sirf pehla result, hi-IN par Hinglish mix)

**Audit (tip evidence):**
- `voice/ContinuousListener.kt` — intent me `EXTRA_LANGUAGE = "hi-IN"`, `EXTRA_MAX_RESULTS = 1`; `onResults` me `RESULTS_RECOGNITION` ka sirf `firstOrNull()` liya jata hai; `onPartialResults` ka callback maujood hai (foreground barge-in gating ke liye).
- Call sites (3): `ui/FloraApp.kt:494`, `MainActivity.kt:135`, `voice/BackgroundMicService.kt:163` — sab `onResult: (String) -> Unit` signature par hain.
- `voice/SttLanes.kt` — PCM lane hai (Deepgram REST, key ho to; warna null → caller system fallback). Iska ekmatra caller `VoiceEnrolScreen.kt` hai (voice enrolment samples) — live chat/voice STT isse nahi chalta. Deepgram key user-gated hai (sign-in security-code pending) — isliye live STT system recognizer par hi hai.
- Unit tests maujood hain: `app/src/test/java/com/flora/app/voice/NotchAmplitudeTest.kt` (voice test ka raasta maujood hai).

**Research (web-grounded, Android docs):**
- `SpeechRecognizer` `RESULTS_RECOGNITION` list deta hai + `CONFIDENCE_SCORES` float array (`SpeechRecognizer.CONFIDENCE_SCORES` key; values 0.0–1.0, ya -1 jab unavailable ho). `EXTRA_MAX_RESULTS` se alternatives ki sankhya maangi jati hai. CompileSdk 34 par constant maujood hai; runtime par array na mile (purana recognizer) to `getFloatArray` null dega — us case me first-result fallback chahiye (fail-closed).
- Alternatives ka sahi istemal: sirf pehla result lena recognizer ke pehle guess par poora bharosa hai; confidence dekh kar behtar candidate chunna ya low-confidence par alternatives ko command-match me aazmana mishearing ghatata hai — bina user se dobara poochhe (upfront-once rule ke saath).

**Deep find (root cause):** Single-candidate pipeline hai — recognizer ka pehla guess hi final hai, na koi alternative, na koi confidence signal. hi-IN model Hinglish code-mix (Hindi vakya me English shabd) par galat pehla guess de sakta hai, aur pipeline ke paas use sudhaarne ka koi raasta nahi hai.

**Fix design (Phase 2, pehla item; FloraApp.kt nahi chhoota):**
- `ContinuousListener`: `EXTRA_MAX_RESULTS` 1 → 5; `onResults` me confidence array padho; pure function `pickBestResult(results: List<String>, confidences: FloatArray?): String?` — max confidence wala chuno; scores missing / size mismatch / sab -1 ho to first result (maujooda behavior, fail-closed); khali list par koi `onResult` nahi (maujooda behavior).
- `onResult` signature `(String) -> Unit` WAHI rahega — 3 call sites bina badle compile honge (koi FloraApp.kt edit nahi).
- Naya optional callback `onAlternatives: (List<String>) -> Unit = {}` jodo (default khali) — future wiring (planner/design) ke liye; koi caller abhi use nahi karega.
- Unit test: `app/src/test/java/com/flora/app/voice/SttPickTest.kt` — scores se best pick; missing scores → first; sab -1 → first; khali list → null; size mismatch → first.
- Koi language change nahi (hi-IN waisa hi), koi naya permission nahi, koi audio save/log/server nahi (maujooda privacy waisi hi).

**Pre-audit checklist (implement se pehle):** `EXTRA_MAX_RESULTS` ka koi doosra setter to nahi; `CONFIDENCE_SCORES` key ka compile (compileSdk 34) — code me constant reference, runtime null-handling; 3 call sites ki signature-compat (default param se); unit-test runner ka maujooda pattern (`NotchAmplitudeTest` jaisa).
**Phone-test steps (Phase 10, claim nahi):** Hinglish commands (Hindi vakya + English shabd) — pehle galat sunai dene wale examples par sahi command chalna chahiye; STT quality ka antim faisla phone par hi hoga (server evidence se nahi).
**Attempts:** 0.

---

## B4 — Crash-log export missing (crash ka saboot pahunche nahi)

**Audit (tip evidence):**
- `FloraApplication.kt` — crash handler: `filesDir/crash.log` me stacktrace (aakhri 4000 chars, 200KB par rotate — overwrite), koi server upload nahi, pehle wala default handler chain hota hai (crash behaviour bilkul nahi badalta).
- `data/AutoLogStore.kt` — 500 entries, encrypted DataStore, 30-din prune; export/share button kahi nahi hai (sirf Builder/Memories me export hai — audit se).
- `ui/AutoLogScreen.kt` (57 lines, alag file) — Column + LazyColumn + "Log saaf karo" button; call site `ui/FloraApp.kt:5455` pehle se `AutoLogScreen(autoLogStore)` render karta hai — is file ke andar content badalne se FloraApp.kt edit nahi hoga.
- Share pattern maujood hai: `ui/ExtraScreens.kt:50-52` (`ACTION_SEND` + `EXTRA_TEXT` + chooser).

**Research (alternatives):**
- (a) Crash log ko Auto Log me copy karo — REJECTED: crash.log app-private diagnostic hai, Auto Log user-visible action log hai; dono ka maksad alag hai, milane se Auto Log me technical kachra aayega.
- (b) AutoLogScreen me crash-log section jodo (view + user-initiated share) — CHUNA GAYA: maujooda screen, maujooda share pattern, koi naya navigation nahi, koi FloraApp.kt edit nahi.
- (c) adb-only raasta (debug APK `run-as`) — user ke liye raasta nahi hai (PC + USB chahiye); isliye (b) chahiye.
- Safety: stack traces me method/line hoti hai, key material nahi (keys kabhi log nahi hoti — SttLanes/BrainClient comments se); share user ke chooser se, user ki marzi se hoga — silent upload kabhi nahi.

**Deep find (root cause):** Crash ka saboot likha to jata hai, par kisi UI tak pahunche nahi — na Auto Log me, na kisi screen par. Isliye "band ho gayi" ka kaaran phone par bataya hi nahi ja sakta.

**Fix design (Phase 2, B3 ke baad; FloraApp.kt nahi chhoota):**
- Naya helper `data/CrashLogReader.kt`: `readCrashLog(filesDir: File, maxChars: Int = 4000): String` (file na ho to ""; sab exceptions pakdo); `readFullForShare(filesDir: File): String` (share ke liye poora text, 200KB cap ke saath).
- `ui/AutoLogScreen.kt` me crash-log section: header + aakhri crash text (truncated) ya "Koi crash record nahi hai" empty state + Share button (`ACTION_SEND`, `EXTRA_TEXT`, chooser) + note ("Ye sirf is phone ka crash record hai; share aap khud kar rahe hain").
- V1 me DELETE button NAHI hoga (irreversible-only confirmation rule; writer waise bhi 200KB par rotate karta hai).
- Unit test: reader ke liye (missing file → ""; truncation; bada file → cap).

**Pre-audit checklist (implement se pehle):** Writer ka path (`filesDir/crash.log` — FloraApplication se match); `LocalContext` se `filesDir` lena (signature change nahi — call site waisa hi); share pattern ka exact istemal (ExtraScreens jaisa chooser); koi FileProvider zaroorat nahi (EXTRA_TEXT hai, file URI nahi).
**Phone-test steps (Phase 10, claim nahi):** Asli crash ke baad section me entry dikhni chahiye + share se poora text jaana chahiye. (Test crash jaanboojh kar nahi karaya jayega — phone verification opportunistic hai.)
**Attempts:** 0.

---

## B5 — 600s/day cap par "band ho gayi" lagna

**Audit (tip evidence):**
- Cap by design hai (Free 10 min/day = 600 talk sec/day; Pro unlimited). Cap message do jagah hardcoded hai: `data/LicenceGate.kt` (`FREE_LIMIT_MESSAGE`, private) + `voice/BackgroundMicService.kt` lagbhag line 491 (wahi string dobara likhi hui — drift ka khatra).
- `ui/LicenceScreen.kt` me `FreeRemainingText` maujood hai ("Free: aaj X min Y sec baat bachi hai") — usage display hai, par user use tabhi dekhta hai jab Licence screen khole; cap lagne se pehle koi warning nahi hai.
- `BackgroundMicService` me `AutoLogStore` ka koi istemal nahi hai — cap-hit Auto Log me dikhta hi nahi (cap kab laga, ye pata hi nahi chalta).
- `onSpoken` → `addTalkSeconds` + `countUsage` (talk-seconds counter EK hi source hai — gate aur display dono isi se, do sach nahi banenge).

**Research:** Limit ke cliff par user ko surprise dena galat UX hai — standard raasta: limit se pehle ek baar chetavni + limit lagne par saaf message + log. Cap hatana ya badhana is item ka scope NAHI hai (by design barkaraar).

**Deep find (root cause):** Cap nahi, surprise + invisibility bug hai: na warning, na cap-hit ka record, aur message ki do copies (drift).

**Fix design (Phase 2, B4 ke baad; FloraApp.kt nahi chhoota):**
- Threshold warning (BackgroundMicService): Pro nahi + used 540s paar + aaj warning nahi di → ek baar chhota spoken warning ("Free talk time bas 1 minute bacha hai") + Auto Log event. Warned-flag `FloraPreferences` me date-key se (implement waqt fresh-fetch karke maujooda key pattern se milao — `talkSecondsUsed`/`addTalkSeconds` `FloraPreferences.kt:960/965` ke paas ka pattern).
- Cap-hit Auto Log event: cap path me `AutoLogStore(applicationContext).add(...)` (service context se; FloraApp ki tarah appContext se banta hai).
- Dedupe: `LicenceGate.FREE_LIMIT_MESSAGE` ko `internal` karo, BackgroundMicService wahi const istemal kare (same module — koi API change nahi, koi string change nahi).
- Cap message ka wording WAHI rahega ("Aaj ka free talk time (10 minute) khatam ho gaya hai. Kal phir milega, ya Pro (₹199/month) lijiye.") — user ko pehchana hua message milega.

**Pre-audit checklist (implement se pehle):** FloraPreferences me warned-flag ka pattern (fresh-fetch); cap path me `voice?.speak` maujooda hai (wahi istemal hoga); AutoLogStore service context se banta hai ya nahi (constructor `Context` leta hai — service ek Context hai); Pro path par warning/cap-log kabhi na chale (cachedPlan check maujooda hai).
**Phone-test steps (Phase 10, claim nahi):** Free user 9 minute paar kare → warning bole; cap par message + Auto Log entry dikhe; Pro user par kuch nahi.
**Attempts:** 0.

---

## B6 — Voice-first Baat view me transcript chhupa

**Audit (tip evidence):**
- `ui/FloraApp.kt` tip lines 242–245: voice-first Baat view default ON — likha transcript nahi dikhta, sirf orb + mic; chat sirf "Chat kholo" dabane par dikhta hai.
- Isliye STT error vs brain error phone par alag nahi dikhta (user ko pata hi nahi chalta Flora ne kya suna vs kya samjha).

**Research:** Voice-first design v2.15 ka jaanboojh kar faisla hai — default flip karna product behavior badal dega, isliye default ko haath nahi lagana. Sahi raasta: voice view ke andar hi suna hua text dikhana (design barkaraar + visibility).

**Deep find (root cause):** Invisibility hai, recognition nahi — suna hua text kahin dikhta hi nahi.

**Fix design (planner DONE-CODE ke baad + design-pass dedupe check):**
- Baat view ke andar aakhri suna hua transcript ki chhoti line dikhao (orb + mic ke saath) — ya persistent transcript affordance (implement waqt design pass se takkar check karke tay karna).
- Agar design pass ise cover kar le to B6 ko register se dedupe karke hatao (dobara mat karo) — ye faisla design coordinator ke complete hone par hoga, guess par nahi.

**Pre-audit checklist (implement se pehle):** Design pass me B6 cover hua ya nahi (dedupe); Baat view ka layout (kahan line aayegi, koi option cover na ho — design pass ke rules ke saath).
**Phone-test steps (Phase 10, claim nahi):** Bolo → suna hua text Baat view me dikhe → STT vs brain error alag dikhe.
**Attempts:** 0.

---

## Implementation order + waits (Round 1 ke baad)

| Order | Item | FloraApp.kt? | Wait |
|---|---|---|---|
| 1 | B3 STT alternatives + confidence | Nahi (ContinuousListener only + unit test) | Koi nahi |
| 2 | B4 Crash-log view + share | Nahi (CrashLogReader + AutoLogScreen content only) | Koi nahi |
| 3 | B5 Cap warning + cap-log + dedupe | Nahi (BackgroundMicService + LicenceGate + FloraPreferences flag) | Koi nahi |
| 4 | B2 Licence timeout bound | Nahi (LicenceClient + LicenceGate only) | B3–B5 ke baad |
| 5 | B1 Echo guards (background + foreground) | Haan (FloraApp isEchoLike + BackgroundMicService) | Planner DONE-CODE |
| 6 | B6 Transcript visibility | Haan (FloraApp Baat view) | Planner DONE-CODE + design dedupe |

## Section F (CEO-added) — Round 1 me sirf tracked, kaam Section B ke baad

F1 (OPEN 14/Cycle 125 — gate: design input ya deploy session), F2 (deploy session #2 — parent browser; B4 admin-approve scope saaf karna is cycle ka analysis kaam hai, dashboard execution parent ka), F3 (Storage ZERO policies ruling — docs decision), F4 (S2-9 hold + send-push tip hygiene — tip push is cycle se ho sakta hai, SQL deploy parent browser se), F5 (N4/N5 live JWT — JWT parent browser se, isliye procedure + gate-check is cycle ka, live test ka hissa parent ka), F6 (G10 — SD-P5 LAST, sequence se pehle mat banao), F7 (FCM live E2E — phone-gated), F8 (item 7 citation — web-UI test-report step parent se, ya count-run raasta audit se), F9 (neural residuals — code items, B ke baad), F10 (backup scope decision — decision parent/CEO ka), F11 (013 FLAGs + F3-SURF — F phase me audit se pehchano), F12 (website visual + UPI unset — phone/admin-gated), F13 (CI test-report gap — workflow edit web UI se, parent ka; proposal is cycle ka), F14 (live-route deploy gate — process gate, deploy prep doc me darj), F15 (Phase 10 — antim gate, phone evidence), F16 (SD-P2→P6 sequence — spend approval alag se, sequence mat todo), F17 (grievance + Deepgram risks — user-gated, force nahi).
