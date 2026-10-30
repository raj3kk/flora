# Flora Call + Notification Announce — SPEC (v1, Pehle-Jaanch)

**Order:** user order 2026-10-30 ~12:08 IST — (1) call aaye to Flora bole
"Boss, [caller] calling you" (caller naam padh kar); (2) notification aaye to
Flora use samajh kar bole, SIRF important/kaam ki notifications; (3) research
→ plan → production-ready WITH audit cycle → implement WITH audit cycle.
**Status:** SPEC ONLY abhi — implement pre-audit PASS ke baad batches me hoga.
Phone verification Phase 10 me hogi; gates code+CI+tip-content ke hain,
phone-verified ka daava kabhi nahi.

## §0. IMAANDAAR CONSTRAINT (spec me saaf, chhupaya nahi)

Bina background ke ye NAHI ho sakta. Android me call/notification sunne ke
liye background component chahiye hi chahiye:
- **Calls:** manifest `PHONE_STATE` broadcast receiver (exempt broadcast —
  background restriction list se bahar hai, isliye receiver bina foreground
  service ke bhi fire hota hai).
- **Notifications:** system-bound `NotificationListenerService` (user ke grant
  par system khud bind karta hai; app UI khuli ho zaroori nahi).
- **Visibility:** Batch 3 me ek halki foreground service (AnnounceService)
  sirf isliye — taaki user ko hamesha dikhe ki announce chalu hai, aur ek
  tap par band ho sake. Detection ka kaam receiver/listener ka hi rahega.
- **App force-stop ho ya service band ho to announce NAHI hoga.** Ye limit
  spec me likhi hai, user se chhupayi nahi jayegi. Flora Notification Access
  ya Phone State khud ON nahi kar sakti — sirf settings page khol sakti
  hai, ON user khud karega. Koi permission kabhi auto-ON nahi hogi.

## §1. RESEARCH DECISIONS (verify kiya gaya, guess nahi)

### 1.1 Call path — receiver hi sahi raasta hai (doosre raaste reject)
- **PHONE_STATE broadcast receiver (CHOSEN):** `EXTRA_STATE == RINGING` par
  fire hota hai; number `EXTRA_INCOMING_NUMBER` se milta hai. Exempt
  broadcast hai, isliye background me bhi chalta hai.
- **TelephonyCallback (REJECTED for number):** API 31+ ka modern raasta hai,
  par `CallStateListener.onCallStateChanged(state)` me number MILTA HI NAHI
  (privacy design) — sirf state pata chalta hai, naam bolna impossible hai.
- **CallScreeningService (REJECTED):** number deta hai, par iske liye
  `ROLE_CALL_SCREENING` role chahiye — ye dialer-role wale raaste se
  takrayega aur user ko ek aur system role dena padega. Announce ke liye
  overkill hai; bol-kar-receive/reject is scope me NAHI hai (neeche §6).
- **Number ke liye permission (CRITICAL GAP, Batch 1 fix karega):** Android
  9 (API 28) se `EXTRA_INCOMING_NUMBER` ke liye `READ_PHONE_STATE` ke
  saath `READ_CALL_LOG` bhi chahiye. Manifest me aaj `READ_CALL_LOG` HAI
  HI NAHI — isliye maujooda receiver naye Android par number null pa kar
  chupchaap return kar deta hai (announce hota hi nahi). Batch 1 me
  manifest + runtime request + PermissionsScreen row teeno judenge.

### 1.2 Caller name — contacts reverse lookup
- `ContactsContract.PhoneLookup.CONTENT_FILTER_URI` + `Uri.encode(number)`
  se number → display name. Sirf tab jab `READ_CONTACTS` user ne di ho
  (manifest me pehle se hai; wizard + PermissionsScreen me row pehle se
  hai). Na mile to naam nahi, number bhi NAHI bola jayega (neeche §2).
- **ContactLookup me aaj sirf naam → number hai** (`findAll`); number →
  naam ka function Batch 1 me judenga. Koi contact data kabhi save, log,
  backup ya server par nahi jayega — naam sirf usi waqt ke announce me
  use hoga, phir bhool jaya jayega.

### 1.3 Announcement format + audio route
- **Exact format (user order se):** known caller → `Boss, {name} calling
  you`; unknown → `Boss, unknown number calling you`. User ki language
  Hinglish hai, par order ke shabd English me hain — isliye ye phrase
  English me hi bola jayega (TTS locale hi-IN par bhi ye shabd samajh
  aate hain; phrase badalna user ke alag order par hoga).
- **Unknown par digits NAHI bole jayenge.** Purana code poora number bolta
  tha (`Call aa raha hai, number X se`) — naya behaviour jaanboojh kar
  digits nahi bolega (paas khade log number sun lete hain; aur naam na
  hone par digits ka koi kaam ka matlab nahi). Auto Log me bhi sirf
  aakhri 4 digits (masked), poora number kabhi nahi.
- **Audio route:** maujooda shared path `NeuralSpeaker.speakFixed` hi
  use hoga (neural engine ho to wahi, warna system TTS hi-IN, QUEUE_FLUSH,
  utterance id `flora-announce`). Ye default TTS stream par bajta hai —
  ringtone (STREAM_RING) ke upar overlay hoga, ringtone band nahi hogi.
  Ye acceptable hai: ek announcement per ringing event, 30-second dedup
  window (dobara broadcast aaye to dobara nahi bolega).
- **FloraInCallService se takraav NAHI:** wo service sirf tab call dekhti
  hai jab user khud Flora ko default dialer banaye (RoleManager dialog);
  announce ringing se pehle ka kaam hai, answer ke baad ka nahi.
  Auto-answer pehle hi HATA hua hai — is kaam se wapas KABHI nahi aayega.

### 1.4 Notification path — listener pehle se hai, announce nahi hai
- `FloraNotificationListener` aaj sirf READ-ONLY store karta hai
  (title+text ≤200 chars, `NotificationStore`, sirf phone par, Flora ki
  apni + ongoing notifications ignore). **Bolna, filter, OTP-exclude —
  kuch nahi hai.** Batch 2 ye teeno jodega; store path waisa hi rahega
  (P2 notification-read feature us par nirbhar hai).
- **Grant manual hai:** `BIND_NOTIFICATION_LISTENER_SERVICE` system
  permission hai — app khud ON nahi kar sakti. Flora sirf
  `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` intent se settings
  page kholegi; status `Settings.Secure enabled_notification_listeners`
  se check hoga. Grant ke bina listener bind hi nahi hoga — announce
  chupchaap skip hoga, crash nahi.
- **Important ka matlab (spec decision):** importance = USER KI PASAND.
  Koi app apne aap "important" nahi mana jayega. Gate: (a) notification
  announce toggle ON ho, (b) uska package user ke important-apps set me
  ho, (c) OTP/sensitive hard-exclude se na guzra ho. Category
  (CALL/MESSAGE/ALARM) gate ke roop me use NAHI hogi — har WhatsApp
  message CATEGORY_MESSAGE hota hai, category gate se har message bolna
  padta, jo user ke "sirf kaam ki" order ke khilaaf hai.
- **Picker seeding (QUERY_ALL_PACKAGES ke bina):** default SMS app
  (`Telephony.Sms.getDefaultSmsPackage`) + default dialer app
  (`TelecomManager.getDefaultDialerPackage`) seed honge; baaki list
  `NotificationStore.load()` se mile recent packages se banegi (jo apps
  notifications bhej chuki hain). Koi restricted package-query permission
  nahi li jayegi.
- **Kya bola jayega:** app ka naam + notification TITLE hi (body text
  KABHI NAHI bola jayega — paas khade log poora message sun lete).
  Format: `Boss, {appLabel} se notification: {title}`. Title khaali ho to
  skip (bolne layak kuch nahi).
- **DND / quiet hours:** default = DND ka sammaan (interruption filter
  ALL nahi ho to announce skip; padhne ke liye `getCurrentInterruptionFilter`
  par koi special permission nahi chahiye). Toggle `DND me bhi announce
  karo` default OFF — ON ho to DND me bhi bolega (TTS music stream DND
  se chupta nahi, isliye ye toggle imaandaari se kaam karega). Time-window
  quiet hours (raat 10–subah 7 jaisa) v1 me NAHI — scope control ke liye
  DEFERRED hai (§6 me darj).
- **Dedup:** ek hi package + title 60 second me dobara aaye to dobara
  nahi bolega (progress-update reposts se bachne ke liye; best-effort
  in-memory map — process marne par reset, ye acceptable hai).

### 1.5 OTP / sensitive — HARD LINES (kabhi nahi tootenge)
- OTP/verification code/bank code/password wali notification **KABHI
  NAHI boli jayegi, KABHI log nahi hogi, KABHI server/backup me nahi
  jayegi.** Filter: case-insensitive keywords (otp, one-time, verification
  code, verification, password reset code) + 4–8 digit code pattern jab
  code-keyword paas ho. Match hote hi item DROP — na store, na speak, na
  Auto Log body. (Aaj ka listener OTP text STORE kar leta hai — Batch 2
  me ye privacy gap band hoga.)
- Notification body kabhi server par nahi jayegi (on-device only). Auto
  Log me sirf masked event: `Notification announce — {appLabel}` (title/
  text kabhi nahi). Call Auto Log: `Call announce — {name|unknown}
  (…{last4})` (poora number kabhi nahi).
- Silent action kabhi nahi: har announce visible (AnnounceService
  notification + Auto Log) + ek tap par band. Web password typing guard
  intact. SIM-call me TTS injection (G19) kabhi promise nahi — announcement
  sirf ringing ke dauraan playback hai, call ke andar injection nahi.

### 1.6 Background reliability
- **Foreground notification IDs (collision check):** 1001 wake, 1002
  overlay-mic, 1003 charm, 2002 background-mic, 2003 auto-mode, 2004
  planner, 2007 live-screen — AnnounceService ko **2008** milega (free).
- **Boot:** BootReceiver aaj sirf wake-word / continuous-mic / jobs
  restore karta hai. Batch 3 me announce restore judenga: boot par agar
  call ya notify toggle ON ho to AnnounceService start (try/catch —
  kuch phones par boot se FGS start block hota hai, fail ho to agle
  app-open par toggle wapas service laayega). PHONE_STATE receiver aur
  NotificationListenerService ko boot action nahi chahiye (manifest /
  system-bound hain).
- **Vivo/Chinese-ROM kill:** auto-start + battery-unrestricted user-side
  steps hain (Flora khud ON nahi kar sakti). Batch 3 me Settings me
  announce section ke neeche Vivo guidance text judenga (PermissionsScreen
  ke background section jaisa, announce-specific).
- **Stop control:** Settings me "Announce band karo" ek-tap button (dono
  toggles OFF + service stop) + service notification par "Band karo"
  action (AnnounceStopReceiver). Band karna hamesha chalega, chahe koi
  bhi toggle state ho.

## §2. PER-FUNCTION TABLE

| Function | API | Permission class | Grant flow | Planner/Auto Log | Fail-closed |
|---|---|---|---|---|---|
| Call ringing detect | PHONE_STATE broadcast | READ_PHONE_STATE (runtime) + READ_CALL_LOG (runtime, Batch 1 NEW) | Toggle ON → launcher chain (phone state, phir call log); deny → toggle OFF + note | Announce = plan ka ek step nahi, event-driven announce; Auto Log masked (name/unknown + last4) | Permission na ho / number null → chupchaap skip, crash nahi |
| Caller name | PhoneLookup CONTENT_FILTER_URI | READ_CONTACTS (runtime, pehle se) | Wizard/PermissionsScreen row pehle se | Naam sirf announce me, kabhi store nahi | Na mile → "unknown number" format |
| Call speak | NeuralSpeaker.speakFixed | koi nahi (TTS) | — | QUEUE_FLUSH, dedup 30s | TTS fail → kuch nahi, crash nahi |
| Notification listen | NotificationListenerService | BIND_NOTIFICATION_LISTENER_SERVICE (special, manual) | Settings page kholo → user khud ON kare | System-bound; grant na ho → skip | Grant na ho → skip, crash nahi |
| Notification speak | speakFixed (title only) | — (listener grant par nirbhar) | — | Auto Log masked (appLabel hi) | Toggle OFF / app not picked / OTP / DND → skip |
| Important-apps pick | DefaultSms/Dialer + NotificationStore history | koi nahi | Settings checkboxes | Set DataStore me (string set) | Khaali set → kuch announce nahi (safe default) |
| DND respect | getCurrentInterruptionFilter | koi nahi (read) | Toggle "DND me bhi" default OFF | — | Filter read fail → announce karo (fail-open sirf is read par; baaki sab fail-closed) |
| AnnounceService (visible) | ForegroundService specialUse, id 2008 | FOREGROUND_SERVICE + SPECIAL_USE (pehle se) | Toggle ON → start; dono OFF → stop | Persistent notification + stop action | Start fail (boot block) → try/catch, agle app-open par retry |
| Boot restore | BootReceiver | koi nahi | — | Toggle ON ho to service start | Fail → agle app-open par |

## §3. PRE-AUDIT (live tip grep se — PASS ke liye zaroori)

### 3.1 Maujooda cheezein (dobara mat banao)
- `automation/CallAnnouncementReceiver.kt` — number-only announce, toggle
  `call_announce` default OFF, `NeuralSpeaker.speakFixed` path. Header
  kehta hai "naam batana v1.2 ka kaam" — ye kaam ab Batch 1 hai.
- `automation/EventAnnounceReceiver.kt` — charger/battery announce pattern
  (toggle OFF) — iska pattern mirror karna hai, duplicate nahi banana.
- `automation/FloraNotificationListener.kt` — read-only store, koi speak/
  filter/OTP-exclude nahi. Header "v2.22, P2-F10" — Batch 2 isi file me
  judenga, nayi listener file nahi banegi.
- `automation/ContactLookup.kt` — sirf naam → number (`findAll`).
  Number → naam nahi hai — Batch 1 me judenga.
- `calls/FloraInCallService.kt` — sirf default-dialer role par; auto-answer
  hata hua hai; announce se takraav nahi.
- `data/FloraPreferences.kt` — `CALL_ANNOUNCE`/`EVENT_ANNOUNCE` keys +
  suspend getters/setters maujood; nayi keys isi style me judengi.
  Receiver apna alag `flora_prefs` delegate use karta hai — nayi keys ke
  liye KOI teesra DataStore instance NAHI banega (multi-instance
  anti-pattern); listener/service `FloraPreferences` se padhenge, call
  receiver apna maujooda read path rakhega.
- `data/NotificationStore.kt` — `load()` maujood (picker seeding ke liye
  kaafi, naya list function nahi chahiye); `add()` cap 20.
- `data/AutoLogStore.kt` — `add(text)` suspend, on-device encrypted,
  cap 500, 30-din prune.
- `ui/FloraApp.kt` — toggles `SettingsScreen` (line ~5798) me hain;
  `callAnnounce` state 5830, `eventAnnounce` 5831 (nayi state vars inke
  BAAD declare hongi — declaration-order trap se bachne ke liye);
  `phonePermissionLauncher` 5930 (grant par toggle ON karta hai — Batch 1
  me isme call-log chain judengi, launcher dobara nahi banega);
  `prefs` + `scope` + `context` SettingsScreen params me maujood hain.
- **STALE NOTE CORRECTION:** purani briefing kehti thi SettingsScreen ke
  paas `voice` param NAHI hai — live tip me `voice: VoiceEngine` param
  HAI (line ~5804). Isliye voice-param trap is file me lagu nahi hota;
  phir bhi har edit se pehle signature grep se verify hoga.
- Manifest: READ_PHONE_STATE ✓, READ_CONTACTS ✓, POST_NOTIFICATIONS ✓,
  FOREGROUND_SERVICE + SPECIAL_USE ✓, NotificationListener entry ✓,
  PHONE_STATE receiver entry ✓. **READ_CALL_LOG ✗ ABSENT** — yehi Batch 1
  ka sabse bada fix hai.
- NoAccess coordinator bhi isi waqt app/ par batches push kar raha hai
  (Batch C in-flight) — isliye har batch push se pehle live tip check
  hoga; tip move hua to touched files fresh fetch karke edits dobara
  lagayi jayengi (stale local kabhi push nahi hoga).

### 3.2 Conflicts (pehle fix, tabhi PASS)
- (a) READ_CALL_LOG absent → Batch 1 me manifest + PermissionsScreen row
  + toggle chain. (b) OTP store hona → Batch 2 me store se pehle drop.
  (c) Purana Settings note ("naam batana v1.3 me") stale → Batch 1 me
  text update. (d) Notification IDs — 2008 free hai (upar list). In
  chaaron ke bina pre-audit PASS nahi.

**PRE-AUDIT VERDICT: PASS (upar §3.2 ke chaar points Batch 1/2 me fix
honge — ye fixes spec ka hissa hain, spec se bahar ka koi conflict nahi
mila).** Implement ab batches me hoga.

## §4. IMPLEMENTATION BATCHES (serialized, ek push chain)

- **Batch 1 — Call announce upgrade:** manifest READ_CALL_LOG +
  PermissionsScreen row + toggle chain (phone state → call log) +
  ContactLookup.lookupNameByNumber + CallAnnouncementReceiver rewrite
  (Boss format, unknown handling, 30s dedup, masked Auto Log) + stale
  Settings note update. Files: AndroidManifest.xml, PermissionsScreen.kt,
  ContactLookup.kt, CallAnnouncementReceiver.kt, FloraApp.kt (toggle
  chain + note text).
- **Batch 2 — Notification announce:** FloraPreferences nayi keys
  (NOTIFY_ANNOUNCE, NOTIFY_IMPORTANT_APPS, NOTIFY_DND_ANNOUNCE) +
  AnnounceFilters (OTP hard-exclude + DND check + listener-enabled
  check) + FloraNotificationListener speak path (toggle + important set
  + OTP drop store se pehle + title-only speak + 60s dedup + masked Auto
  Log) + Settings UI (toggles + important-apps picker + Notification
  Access kholo button + status text).
- **Batch 3 — Reliability + visibility:** AnnounceService (foreground,
  id 2008, persistent notification + Band karo action) +
  AnnounceStopReceiver + manifest entries + BootReceiver restore +
  Settings wiring (toggle ON → start, dono OFF → stop, ek-tap band
  button) + Vivo guidance text.
- Har batch: CI green wait karke agla batch. Koi batch queued/in-progress
  run ke upar push nahi hoga.

## §5. PHONE-TEST STEPS (Phase 10 — code gate se alag)
1. Known contact se incoming call → "Boss, {name} calling you" ek baar,
   ringtone ke upar, Auto Log me masked entry.
2. Unknown number se call → "Boss, unknown number calling you",
   digits kabhi nahi.
3. Toggle OFF → koi announce nahi. Permission deny → toggle OFF + note,
   crash nahi.
4. Important app ki notification → title-only announce; non-important app
   ki → kuch nahi; OTP notification → na store, na speak, na log.
5. DND ON + toggle OFF → skip; DND toggle ON → announce.
6. Service kill/restart + reboot → toggle ON ho to AnnounceService wapas;
   force-stop → announce band (honest limit, user ko batana hai).
7. Vivo phone par auto-start OFF ho → guidance text ke steps se ON karne
   ke baad announce wapas.

## §6. HELD / DEFERRED (wajah ke saath, guess par nahi)
- Quiet-hours time window (raat ka fixed slot) — DEFERRED (DND toggle
  v1 me kaafi hai; time picker alag spec chahiye).
- Bol-kar receive/reject, CallScreening role — OUT OF SCOPE (dialer-role
  raasta alag hai; auto-answer kabhi wapas nahi).
- TelephonyCallback number path — REJECTED (§1.1, number milta hi nahi).
- Notification body speak — REJECTED (privacy: paas khade log sun lete).
- Category-based importance — REJECTED (§1.4, har message bolna padta).
- TTS call-uplink injection — NEVER (G19, public API guarantee nahi).
