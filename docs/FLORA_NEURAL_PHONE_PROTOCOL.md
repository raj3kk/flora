# Flora — Neural Voice Phone Test Protocol (Lane C Step 5, Phase 10 se juda)

**Status: PROTOCOL PREPARATION ONLY — EXECUTION NAHI HUI HAI.**
Is document me koi phone result claim NAHI kiya gaya hai. Har case
**PENDING-PHONE-EVIDENCE** hai. Phone test user khud debug APK se
chalayega; jo suna / kitni der lagi / attach status kya tha, wo har
case ke evidence template me user bharega. Bina us evidence ke koi
case "pass" nahi kaha jayega.

**Source:** `docs/FLORA_ORT_ATTACH_PREAUDIT.md` §6.5 (7 phone cases)
aur §8 Step 5. Is protocol ki case list wahi hai — apne man se koi
case badla, hataaya ya joda nahi gaya hai. Pre-audit me protocol file
ka koi proposed naam nahi tha, isliye ye naam rakha gaya hai:
`docs/FLORA_NEURAL_PHONE_PROTOCOL.md`.

**Routing facts ki verification:** neeche §1 ki routing table GitHub
API se fresh tip `d2851983` par grep se re-verify ki gayi hai
(protocol likhne ke waqt ka tip). Code aage badhe to line numbers
drift ho sakte hain — protocol chalane se pehle routing table ki
lines dobara check kar lena; agar koi path badla ho to protocol ko
bina coordinator ke faisle ke aage mat badhao.

---

## 0. BINDING GATE — is protocol me sabse pehle padhne wali line

> **System TTS (Lily) SIRF tab hatana jab in-app neural sample
> user-approval + Phase 10 phone test pass ho.**
>
> **Reference sample files in-app approval NAHI hain** — kisi file me
> rakhi hui sample recording sun lena approval nahi hai. Approval
> sirf tab hogi jab user ne **phone par installed debug APK ke andar**
> neural sample khud suna ho aur Case 7 ke checkpoint par apne
> shabdon me confirm kiya ho ki awaaz Kokoro hf_alpha hai, system
> TTS (Lily) nahi.
>
> Is gate ke pass hue bina "neural ready", "Lily hata do", ya koi
> phone-result claim nahi karna hai. Ye gate pre-audit §8 Step 6 ka
> binding gate hai; ye protocol us gate ka execution document hai,
> uska faisla nahi.

---

## 1. Prerequisites — test shuru karne se pehle

### 1.1 Kaun sa APK lena hai (tip ka naam nahi, tareeka)

- **CI artifact `flora-debug-apk` usi tip ka lena hai jis tip par
  ye protocol run ho raha ho.** "Latest tip" ka naam yahan isliye
  nahi likha gaya hai kyunki tip protocol likhne ke baad badal sakta
  hai — galat tip ka APK lene se test galat build par chalega.
- Tareeka: (1) jis commit par protocol run karna hai, us commit ka
  CI run `Flora Android Build` green hona chahiye; (2) usi run ka
  `flora-debug-apk` artifact download karo; (3) evidence template me
  us commit ka short id + CI run number likh do, taaki baad me pata
  chale test kis build par hua tha.
- Purani installed Flora ko pehle uninstall karo (ya kam se kam
  confirm karo ki debug APK hi khula hai) — do build ka mix-up
  evidence ko kharab kar dega.

### 1.2 Settings me Kokoro model download (hf_alpha)

- Phone par Flora kholo → **Settings** → **Neural voice — Kokoro
  hf_alpha (on-device, phone par hi)** section.
- **"Neural voice model download karo"** button dabao. Download
  **~354 MB** ka hai — **Wi-Fi par hi karo**, mobile data par nahi
  (resume is build me nahi hai: beech me toota to dobara poora
  shuru se download hoga — pre-audit §7.2).
- Download ke dauraan Settings chhodkar mat jao: Settings chhodne
  par download coroutine cancel ho sakti hai aur `.part` delete ho
  jayegi (pre-audit §5.2) — phir dobara shuru se karna padega.
- Download success ke baad status line me ye dikhna chahiye:
  "Neural voice model taiyaar hai (Kokoro hf_alpha). Neural engine
  jud raha hai…" — ye line **attach success ka saboot nahi hai**,
  sirf download success ka hai. Attach ka sachha status agle
  button se jaancho.

### 1.3 Attach-status button (Lane C Step 1 me joda gaya)

- Usi Settings neural section me **"Neural engine status jaancho"**
  button hai (fresh tip par `ui/FloraApp.kt:6420`; status check
  `NeuralVoiceAttach.isAttached()` line 6414 par).
- Button dabane par do me se ek line aani chahiye:
  - **"Neural engine jud gaya — ab Kokoro hf_alpha bolega."**
    → attach success. Iske baad ke cases me neural awaaz aani
    chahiye.
  - **"Neural engine abhi juda nahi hai — dobara judne ki koshish
    ho rahi hai…"** → attach nahi hua; is haalat me Lily hi
    bolegi. Kuch der baad button phir dabao; phir bhi na jude to
    ye Case 2 (attach-fail) ka evidence hai, koi crash ya atakna
    nahi hona chahiye.
- **Har case se pehle status button dabakar status note karo.**
  Bina status jaane "neural suna / Lily suni" ka daava adhoora hai
  — awaaz aur status dono ka milna hi evidence hai.

### 1.4 Phone par taiyaari

- Phone charge par ho (lambi reply + download ke liye), volume
  aadha se zyada ho, Do Not Disturb **OFF** ho (taaki call/event
  announcement sunai de).
- Ek **stopwatch** taiyaar rakho (doosra phone ya ghadi) — code me
  inference timing ka koi log nahi hai (pre-audit §7.3), isliye
  waqt haath se note karna hoga (Case 6).
- In-call case (Case 5) ke liye **doosra phone** chahiye hoga jisse
  call ki ja sake.
- Call announcement (Case 1B) aur event announcement (Case 1C) ke
  liye un features ki permission / setting ON honi chahiye; agar
  koi feature phone par OFF hai to us subcase ko "feature OFF tha"
  likhkar chhodo, "pass" mat likho.
- Har case ka evidence **turant** likho — baad me yaad se bharna
  evidence nahi hai.

### 1.5 Routing table — kaun sa bolna kis raaste se jaata hai (fresh tip par verified)

| Bolna | Raasta (code) | Fresh tip line |
|---|---|---|
| Main chat + saare `voice.speak` | `VoiceEngine.speak()` → engine ho to Kokoro, nahi to system TTS fallback | `assistant/VoiceEngine.kt:167`, engine read `:185`, fallback `:199-203` |
| Call announcement | `NeuralSpeaker.speakFixed()` | `automation/CallAnnouncementReceiver.kt:50` |
| Event announcement | `NeuralSpeaker.speakFixed()` | `automation/EventAnnounceReceiver.kt:44` |
| Wake ack ("Haan, boliye") | `NeuralSpeaker.speakFixed()` | `wakeword/WakeWordService.kt:127` |
| In-call bolna | `VoiceEngine` (via `FloraInCallService`) | `calls/FloraInCallService.kt:43, 57-59, 79` |
| Stop / interrupt | `VoiceEngine.stop()` → `NeuralSpeaker.stop()` (engine stop) + system `tts.stop()` | `assistant/VoiceEngine.kt:215-222`, `assistant/NeuralSpeaker.kt:64-70` |
| Lambi reply chunking | Engine tokens ko 509-509 par chunk karta hai; `>509` par `infer()` guard | `models/KokoroOrtEngine.kt:90` (`MAX_TOKENS = 509`), `:315` (`chunked`), `:367` |
| Attach | `NeuralVoiceAttach.attach()` — app-start + Settings download-success | `models/NeuralVoiceAttach.kt:63`, set `:106`, fail `:109-115` |
| Attach status | `NeuralVoiceAttach.isAttached()` — Settings status button | `ui/FloraApp.kt:6414-6420` |

### 1.6 Awaaz ki pehchaan — Kokoro hf_alpha vs Lily fallback (kaan se kaise pehchanenge)

- **Lily (system TTS fallback):** phone ka system TTS, Flora ke
  Lily profile par — pitch `0.95`, rate `0.88` (fresh tip par
  `VoiceEngine.kt:96-97`); sunne me **gahri (pitch neeche) +
  dheemi + mellow** (code comment, `VoiceEngine.kt:83`).
- **Kokoro hf_alpha (neural):** on-device ORT engine se bani
  awaaz. Iski pehchaan ka **ek hi imaandaar tareeka** hai:
  (1) status button "jud gaya" dikhaye, **aur** (2) awaaz Lily
  baseline se **saaf alag** lage.
- **Baseline pehle banao:** Case 2 (attach-fail, bina model) me
  Lily ko dhyaan se suno aur apne shabdon me likho (gahri?
  dheemi? robotic?). Uske baad hi Case 1 me neural awaaz se
  tulna karo. Bina baseline ke "ye neural hai" ka daava mat karo.
- **Reference sample files se tulna approval nahi hai** (§0 gate
  dobara padho). Agar awaaz Lily jaisi hi lage par status "jud
  gaya" dikhaye, to use **mismatch** likho — pass mat likho.

---

## 2. Test cases — §6.5 ke 7 cases (sab PENDING-PHONE-EVIDENCE)

Har case ka format ek jaisa hai: **Uddeshya → Steps → Kya sunna
chahiye → Status button par kya dikhna chahiye → Timing note →
Pass/Fail criteria → Evidence template (khaali).**

### Case 1 — Download → attach → pehla neural utterance (4 speak paths)

**Uddeshya:** Model download ke baad attach hone par pehli baar
neural awaaz aaye, aur wo Lily se saaf alag ho. Is case ke andar
pre-audit §6.5 ke mutabiq **4 speak paths** check hote hain —
main chat, call announcement, event announcement, wake ack — aur
in-call ka cross-reference Case 5 me hai.

**Pehle karo:** §1.2 download + §1.3 status button. Status "jud
gaya" aane ke baad hi neeche ke subcases chalao. Agar status "juda
nahi" aaye to Case 1 ko rokkar Case 2 ka evidence bharo.

**1A — Main chat (`VoiceEngine.speak`):**
1. Flora chat kholo, koi chhota Hindi sawaal likho (jaise "Namaste,
   tum kaun ho?").
2. Jawab suno — poora, bina katke.
3. **Kya sunna chahiye:** Kokoro hf_alpha neural awaaz; Lily
   baseline se saaf farq; awaaz beech me rukni / atakni nahi chahiye.
4. **Status button:** isse pehle "jud gaya" dikha ho.
5. **Pass:** neural awaaz + poora jawab + koi crash nahi.
   **Fail:** Lily awaaz (jabki status "jud gaya" tha), ya aadha
   jawab, ya crash / atakna.

**1B — Call announcement (`speakFixed`):**
1. Doosre phone se is phone par call karo (call announcement
   feature ON ho).
2. Announcement suno ("Call aa raha hai, number … se.").
3. **Kya sunna chahiye:** wahi neural awaaz jo 1A me suni thi.
4. **Pass:** neural awaaz me announcement. **Fail:** Lily awaaz,
   ya announcement na aana (to kaaran likho: feature OFF tha?
   permission nahi thi?).

**1C — Event announcement (`speakFixed`):**
1. Flora me koi event/reminder aise set karo jiska announcement
   bajna ho, aur us waqt ka intezaar karo (ya test event trigger
   karo, jo tareeka app me ho).
2. Announcement suno.
3. **Kya sunna chahiye:** wahi neural awaaz jo 1A me suni thi.
4. **Pass:** neural awaaz me announcement. **Fail:** Lily awaaz,
   ya announcement na aana (kaaran likho).

**1D — Wake ack (`speakFixed`, "Haan, boliye"):**
1. Wake word bolo (wake feature ON ho).
2. Ack suno ("Haan, boliye").
3. **Kya sunna chahiye:** wahi neural awaaz jo 1A me suni thi,
   turant — lambi der ka gap nahi.
4. **Pass:** neural awaaz me ack. **Fail:** Lily awaaz, ya ack na
   aana (kaaran likho).

**Timing note (har subcase):** pehli awaaz aane tak ka waqt
(stopwatch se, Case 6 me jama karo).

**Evidence template — Case 1 (har subcase ke liye alag bharo):**
```
Case 1_ (1A / 1B / 1C / 1D): ________
Build (commit short id + CI run): ________
Status button par kya tha: ________
Kya suna (apne shabdon me, Lily se farq kya laga): ________
Pehli awaaz tak ka waqt (sec): ________
Poora bola ya aadha/katka: ________
Crash / atakna hua? ________
PASS / FAIL / INCONCLUSIVE (kaaran): ________
```

### Case 2 — Attach-fail ka rasta (model files ke bina)

**Uddeshya:** Model na ho to app crash na kare, Lily hi bole, aur
Settings status imaandaar rahe (pre-audit §4.3 GAP-2 ka phone-side
check).

**Steps:**
1. Model download kiye **bina** (ya naya install jisme model kabhi
   download nahi hua ho) Flora kholo.
2. Chat me koi sawaal puchho aur jawab suno.
3. Settings → neural section kholo, status padho, phir **"Neural
   engine status jaancho"** button dabao aur line note karo.

**Kya sunna chahiye:** Lily (system TTS) awaaz — yehi is case ka
**Lily baseline** hai. Ise dhyaan se suno aur apne shabdon me
likho; Case 1 ki tulna isi se hogi.
**Status button par kya dikhna chahiye:** "Neural engine abhi juda
nahi hai…" wali line (ya iske barabar imaandaar line) — "jud gaya"
**nahi** dikhna chahiye.
**Pass:** Lily boli + koi crash nahi + status imaandaar (juda nahi
dikha).
**Fail:** crash, ya atakna, ya status "jud gaya" dikhaye jabki
model hai hi nahi, ya koi awaaz hi na aaye.

**Evidence template — Case 2:**
```
Case 2: attach-fail rasta
Build (commit short id + CI run): ________
Kya suna (Lily baseline, apne shabdon me): ________
Status button par kya tha: ________
Crash / atakna hua? ________
PASS / FAIL / INCONCLUSIVE (kaaran): ________
```

### Case 3 — Lambi reply (>509 tokens)

**Uddeshya:** 509-token chunk boundary par awaaz toote nahi, koi
chunk chhoote nahi, aur `onDone` ek hi baar aaye (taaki free-talk
counting na toote) — pre-audit §6.5 case 3.

**Steps:**
1. Status "jud gaya" confirm karo (Case 1 ke baad).
2. Flora se lambi jawab maango — jaise "Mujhe koi lambi kahani
   vistaar se sunao" ya koi aisa sawaal jiska jawab 2-3 minute se
   kam ka na ho. Agar pehla jawab chhota aaye to lamba jawab
   maangkar dobara try karo; jo prompt diya, use evidence me likho.
3. Poora jawab shuru se aakhri shabd tak suno — beech me mat roko.

**Kya sunna chahiye:** Poora jawab, kram me, bina koi hissa
chhoote. Chunk boundary par **sur (prosody) thoda badal sakta
hai** — ye jaana hua, documented deviation hai (engine
`KokoroOrtEngine.kt:309-314` comment; pre-audit D2) — sur badalna
**fail nahi hai**.
**Status button par kya dikhna chahiye:** "jud gaya".
**Timing note:** poora jawab kitni der chala, aur chunk boundary
par koi gap dikha ya nahi (sec me).
**Pass:** koi hissa chhoota nahi + jawab poora hua + uske baad
Flora dobara bolne ko taiyaar thi (onDone ek baar — agla sawaal
normal chala).
**Fail:** beech ka hissa gayab, ya jawab beech me atak gaya, ya
jawab khatm hone ke baad Flora bolna band kar de (dobara sawaal
par jawab na aaye), ya crash.
**Inconclusive (fail nahi):** jawab itna chhota raha ki >509-token
boundary tak pahuncha hi nahi — to "boundary tak pahuncha nahi"
likho aur lambi prompt se dobara try karo.

**Evidence template — Case 3:**
```
Case 3: >509-token reply
Build (commit short id + CI run): ________
Prompt (kya maanga): ________
Jawab kitni der chala (min:sec): ________
Koi hissa chhoota? ________
Chunk boundary par gap / sur badla? ________
Jawab ke baad agla sawaal normal chala? ________
PASS / FAIL / INCONCLUSIVE (kaaran): ________
```

### Case 4 — Interrupt / stop

**Uddeshya:** Naya speak purana turant roke, aur Stop button
neural path par bhi kaam kare (pre-audit §6.5 case 4).

**Steps:**
1. Status "jud gaya" confirm karo.
2. Flora se lamba jawab maango; jawab bolte waqt hi **doosra
   sawaal bhejkar** naya speak trigger karo — purani awaaz turant
   rukni chahiye aur nayi shuru honi chahiye.
3. Phir ek aur lamba jawab chalao aur beech me **Stop button**
   dabao — awaaz turant rukni chahiye.
4. Stop ke baad ek chhota sawaal puchho — Flora ko dobara normal
   bolna chahiye (stop ke baad latakna nahi chahiye).

**Kya sunna chahiye:** Purani awaaz turant band + nayi awaaz
saaf shuru (step 2); Stop par poori khamoshi (step 3); Stop ke
baad dobara normal bolna (step 4).
**Status button par kya dikhna chahiye:** "jud gaya" (poore case
me).
**Timing note:** Stop dabane se awaaz band hone tak ka waqt
(~1 sec ke andar hona chahiye; jo bhi ho, waqt likho, andaza nahi).
**Pass:** dono interrupt (naya speak + Stop) turant kaam kare +
uske baad dobara bolna normal ho.
**Fail:** purani awaaz chalti rahe, ya Stop ke baad bhi awaaz na
ruke, ya Stop ke baad Flora bolna band kar de.

**Evidence template — Case 4:**
```
Case 4: interrupt / stop
Build (commit short id + CI run): ________
Naya speak par purani awaaz ruki? (turant / der se ___ sec / nahi): ________
Stop button par awaaz ruki? (turant / der se ___ sec / nahi): ________
Stop ke baad dobara bolna normal tha? ________
PASS / FAIL / INCONCLUSIVE (kaaran): ________
```

### Case 5 — In-call (neural awaaz doosri taraf sunai de ya nahi)

**Uddeshya:** In-call path par neural awaaz ki audibility check
karna — ye pre-audit §3.4 ka **UNVERIFIED** item hai: in-call
bhi usi AudioTrack (`USAGE_MEDIA`, 24 kHz mono) se bajega, par
earpiece vs speaker par kya sunai dega, ye phone test ke bina
nahi kaha ja sakta.

**Steps:**
1. Status "jud gaya" confirm karo.
2. Doosre phone se is phone par call karo; Flora ke in-call
   jawab (jaise "Namaste, Flora bol rahi hun…") ko **doosre
   phone par** suno.
3. Pehle **speaker** par, phir **earpiece** par (jo tareeka app
   me ho) dobara check karo.
4. Apne phone par bhi suno ki awaaz kahan se aa rahi hai
   (speaker / earpiece).

**Kya sunna chahiye:** Ye case **khoj (discovery)** hai, pass ka
pehle se pakka jawab nahi hai. Doosri taraf awaaz saaf sunai deni
chahiye — agar nahi sunai de to ye ek **finding** hai, chhupana
nahi hai.
**Status button par kya dikhna chahiye:** "jud gaya".
**Pass (is case ke liye):** Doosri taraf neural awaaz saaf sunai
di (speaker aur/ya earpiece — jo bhi hua, dono alag-alag likho).
**Fail / finding:** Doosri taraf kuch sunai na de, ya sirf
speaker par sunai de earpiece par nahi (ya ulta), ya Lily sunai
de neural ki jagah — jo bhi hua, wahi likho.
**Note:** Agar in-call jawab trigger hi na ho (call uthane ka
rasta phone par na mile), to "trigger nahi hua" likho — use pass
mat likho.

**Evidence template — Case 5:**
```
Case 5: in-call
Build (commit short id + CI run): ________
Doosre phone par kya suna (neural / Lily / kuch nahi): ________
Speaker par: ________  Earpiece par: ________
Apne phone par awaaz kahan se aayi: ________
PASS / FINDING / INCONCLUSIVE (kaaran): ________
```

### Case 6 — Inference speed (pehli inference + per-chunk waqt)

**Uddeshya:** On-device inference speed ka number note karna —
aaj code me koi timing log nahi hai, isliye ye number sirf phone
par stopwatch se milega (pre-audit §6.5 case 6, §7.3).

**Steps:**
1. App ko poori tarah band karke dobara kholo (taaki pehli
   inference ka waqt saaf mile), status "jud gaya" confirm karo.
2. Chhota sawaal bhejo aur stopwatch chalao: **bhejne se pehli
   awaaz tak** ka waqt note karo (ye pehli inference + warm-up
   hai — sabse dheemi ho sakti hai, ye expected hai).
3. Phir 3-4 aam sawaal puchho aur har baar bhejne se pehli awaaz
   tak ka waqt note karo (ye warm inference hai).
4. Case 3 ki lambi reply me chunk ke beech koi gap dikha to uska
   waqt bhi note karo.

**Kya note karna hai (koi pass/fail number pehle se tay nahi
hai):** Pehli inference sabse dheemi ho sakti hai — "der se aayi"
ko turant fail mat likho; **number likho**, faisla number dekhkar
hoga. Agar pehli awaaz me 10 sec se zyada lage to use alag se
highlight karo (user experience ka sawal hai).
**Status button par kya dikhna chahiye:** "jud gaya".
**Pass (is case ke liye):** Saare waqt imaandaari se note hue
(pehli + warm + chunk gap) — number hi is case ka result hai.
**Fail:** Awaaz kabhi na aaye, ya app pehli inference par atak
jaye / crash ho.

**Evidence template — Case 6:**
```
Case 6: inference timing
Build (commit short id + CI run): ________
Pehli inference (bhejne se pehli awaaz tak, sec): ________
Warm inference 1 / 2 / 3 (sec): ________ / ________ / ________
Lambi reply me chunk gap (sec, ya "koi gap nahi"): ________
App atki / crash hui? ________
PASS (numbers noted) / FAIL (kaaran): ________
```

### Case 7 — In-app neural sample approval checkpoint (binding gate ka checkpoint)

**Uddeshya:** §0 ke binding gate ka faisla isi checkpoint par
hoga — user ne phone par installed debug APK ke andar neural
sample khud suna aur confirm kiya.

**Steps:**
1. Case 1–6 ke evidence poore hone ke baad hi ye checkpoint
   chalao (pehle nahi).
2. Flora chat me neural sample suno — koi saaf, chhota Hindi
   vaakya (jaise Case 1A wala jawab) — aur dhyaan se suno.
3. Apne shabdon me jawab do: (a) ye awaaz Kokoro hf_alpha
   (neural) lagi ya Lily (system TTS)? (b) Lily baseline (Case 2)
   se farq kya tha? (c) kya ye awaaz Flora ki awaaz ke roop me
   manzoor hai?
4. Neeche wali approval line ko **user apne shabdon me** bhare —
   agent user ki taraf se approval nahi likhega.

**Kya sunna / confirm karna hai:**
- Awaaz phone par installed debug APK ke andar se aayi ho (kisi
  file / recording se nahi).
- Awaaz system TTS (Lily) nahi, Kokoro hf_alpha neural awaaz ho.
- Status button ne "jud gaya" dikhaya ho jab ye sample suna gaya
  ho.
- Case 1–6 me koi aisa fail na ho jo is approval ko rokta ho
  (koi bhi fail ho to approval **rukegi**, pehle fail ka hal
  hoga).

**Approval kaise darj hogi (evidence template — user bharega):**
```
Case 7: in-app neural sample approval
Build (commit short id + CI run): ________
Sample kya suna (vaakya): ________
Ye awaaz kaun si lagi (Kokoro hf_alpha / Lily / pata nahi): ________
Lily baseline se farq (apne shabdon me): ________
Status button par kya tha jab sample suna: ________
Case 1-6 ka status (koi fail to nahi): ________
USER APPROVAL: "Maine phone par Flora app ke andar neural sample
khud suna hai. Ye awaaz Kokoro hf_alpha lagi / nahi lagi (jo sahi
ho, wahi likho). Main is awaaz ko manzoor karta hun / nahi karta
(jo sahi ho, wahi likho)."
Naam + tareekh: ________
```

**Is checkpoint ka result tabhi "APPROVED" hai jab:**
- Upar ki line user ne khud bhari ho, aur
- Usme "Kokoro hf_alpha lagi" + "manzoor karta hun" dono hon, aur
- Case 1–6 me koi blocking fail na ho.
- Inme se koi bhi shart poori na ho to result **NOT-APPROVED**
  hai aur §0 ka gate **band** rehta hai — Lily hatane ka koi
  faisla nahi hoga.

---

## 3. In-app neural sample approval checkpoint — summary (Case 7 ka hi vistaar)

Ye section Case 7 ko dohrata nahi hai, balki uska niyam ek jagah
saaf karta hai taaki koi galatfahmi na rahe:

1. **Kya sunna hai:** Phone par installed debug APK ke andar
   Flora ki neural awaaz (Kokoro hf_alpha) — chat jawab ya wake
   ack jaisa live sample, koi file nahi.
2. **Kya confirm karna hai:** (a) awaaz neural hai, system TTS
   nahi; (b) Lily baseline se farq saaf hai; (c) status button
   "jud gaya" dikhata hai; (d) Case 1–6 me blocking fail nahi hai.
3. **Approval kaise darj hogi:** User Case 7 ka template apne
   shabdon me bharega. Bina user ke bhare hue template ke koi
   approval claim nahi hoga.
4. **Kya approval NAHI hai:** Reference sample files sunna,
   kisi aur ki recording sunna, code padhkar "neural hi hoga"
   kehna, ya CI green hona — ye sab approval nahi hain. CI green
   sirf ye kehta hai ki build bani; awaaz kaisi hai, ye sirf
   phone par pata chalta hai.

---

## 4. Summary table — protocol khatm hone par bharo (abhi sab PENDING)

| Case | Kya check hota hai | Status (abhi) |
|---|---|---|
| 1 (1A–1D) | Download → attach → pehla neural utterance, 4 speak paths | PENDING-PHONE-EVIDENCE |
| 2 | Attach-fail rasta (bina model, Lily + imaandaar status) | PENDING-PHONE-EVIDENCE |
| 3 | >509-token reply (chunk na chhoote, onDone ek baar) | PENDING-PHONE-EVIDENCE |
| 4 | Interrupt / stop (naya speak + Stop button) | PENDING-PHONE-EVIDENCE |
| 5 | In-call audibility (doosri taraf + earpiece/speaker) | PENDING-PHONE-EVIDENCE |
| 6 | Inference timing (pehli + warm + chunk gap, numbers) | PENDING-PHONE-EVIDENCE |
| 7 | In-app neural sample user approval checkpoint | PENDING-PHONE-EVIDENCE |
| Gate (§0) | Lily hatana — sirf Case 7 approval + Phase 10 pass par | **BAND (NOT-APPROVED jab tak evidence na aaye)** |

---

## 5. Rukne / dobara chalane ke niyam

- **Crash / atakna:** Jis case me crash ho, us case ko wahin
  roko, crash ka waqt + kya kar rahe the, likho, aur agle case
  par jaane se pehle coordinator ko batayo. Crash ko chhupakar
  "pass" mat likho.
- **Status mismatch:** Awaaz neural lage par status "juda nahi"
  dikhaye (ya ulta) — to use mismatch likho, andaze se pass/fail
  mat likho. Ye pre-audit GAP-2 se juda hua observation hai.
- **Dobara chalana:** Koi case fail ho to fix ke baad **poora
  case** dobara chalao (sirf fail hua hissa nahi), aur naya
  evidence naye build number ke saath likho. Purana evidence
  mitao mat — dono rakho.
- **Ek case ka fail doosre case ka fail nahi hai:** Har case ka
  faisla uske apne evidence par hoga. Sirf Case 7 + Phase 10 ka
  jod hi §0 gate kholta hai.

## Appendix — jaane hue limits jo phone test me dhyaan me rakhne hain (fail nahi, note karo)

- Lambi reply me chunk boundary par sur badal sakta hai (D2,
  sentence-split Phase 10 ke baad aayega) — sur badalna fail nahi,
  hissa chhootna fail hai.
- Download resume nahi hai; ~354 MB dobara shuru se hoga agar
  beech me toota (§7.2).
- In-call AudioTrack routing UNVERIFIED hai — Case 5 isi liye hai.
- Worker-thread callbacks (barge-in / continuous-mic transition)
  phone par dekhna hai — agar neural path par mic wapas lene me
  farq dikhe to note karo (pre-audit §5.3).
- `.ok` marker ka content app-start par validate nahi hota
  (pre-audit GAP-1) — phone test isko pakad nahi sakta; ye yahan
  sirf isliye darj hai taaki is protocol ko is gap ka hal na
  samjha jaye.
- APK size delta (onnxruntime AAR) is protocol ka hissa nahi hai;
  wo CI artifact size se alag note kiya jayega.
