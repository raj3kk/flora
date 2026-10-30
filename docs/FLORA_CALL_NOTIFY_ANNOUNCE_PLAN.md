# Flora Call + Notification Announce — SPEC (v1, Pehle-Jaanch)

**Order:** user order 2026-10-30 ~12:08 IST — (1) call aaye to Flora bole
"Boss, [caller] calling you" (caller naam padh kar); (2) notification aaye to
Flora use samajh kar bole, SIRF important/kaam ki notifications; (3) research
→ plan → production-ready WITH audit cycle → implement WITH audit cycle.
**Status:** Code landed — call/notification announce batches implemented hain (AnnounceService, CallAnnouncementReceiver, FloraNotificationListener manifest me wired); phone verification Phase 10 me hogi.
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

## §7. MAYA-PARITY: SELF-ACTIVATION (user order 2026-10-30 ~12:13 IST)

Maya call aate hi KHUD ACTIVE ho jati thi — user ko app kholna ya tap
karna nahi padta tha. Flora me wahi behavior chahiye: ringing par Flora
SELF-ACTIVATE hogi (bina user tap ke): foreground service announcement
bolega + call UI saamne aayega. Background se seedha `startActivity`
Android 10+ par silently drop hota hai (background activity launch ban)
— isliye koi hidden trick nahi, sirf sanctioned raasta:

### 7.1 PRIMARY raasta — Full-Screen Intent notification (sanctioned)
- Ringing par `CallAnnouncementReceiver` ek HIGH-importance notification
  post karega (channel importance HIGH, category CATEGORY_CALL) jisme
  `setFullScreenIntent(pendingIntent, true)` hoga — PendingIntent nayi
  `IncomingCallActivity` ka (showWhenLocked + turnScreenOn, CallActivity
  jaisa pattern — CallActivity reuse NAHI hogi, wo dialer-role
  CallHolder se bandhi hai).
- **OS behavior (AOSP FSI limits, verify kiya gaya):** permission granted
  ho to locked/off/AOD screen par FSI activity LAUNCH hoti hai; unlocked
  screen par persistent heads-up notification pill buttons ke saath
  dikhta hai (tap par activity khulti hai). Permission denied ho to
  sirf 60-second heads-up milta hai — activity khud nahi khulegi.
- **Permission (API 34+):** `USE_FULL_SCREEN_INTENT` declare karna hoga
  (manifest me aaj NAHI hai — Batch 4 me judenga). Android 14+ par ye
  sirf calling/alarm apps ko default milta hai; baaki apps ko user se
  grant lena padta hai via `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`
  ("Manage full screen intents" setting). Check API:
  `NotificationManager.canUseFullScreenIntent()`. Flora sideload hai
  (Play policy lagu nahi), par OS default-grant rule phir bhi lagu hai
  — isliye spec me grant flow likha hai, auto-grant ka daava nahi.
  Grant na ho to fallback §7.2 chalega, feature chupchaap fail nahi hoga.
- **POST_NOTIFICATIONS bhi chahiye** (API 33+, manifest me pehle se hai):
  grant na ho to FSI notification dikhega hi nahi — announce (voice)
  phir bhi chalega, UI nahi. Ye bhi honest limit hai.
- **IncomingCallActivity UI (honest, koi jhootha button nahi):** bada text
  "Boss, {name} calling you" (unknown par "Boss, unknown number calling
  you"), neeche do button — [Dismiss] (activity band, announce rukega
  nahi kyunki one-shot hai) aur [Wapas call karo] (sirf ACTION_DIAL
  kholega number ke saath — user khud call button dabayega, auto-dial
  KABHI nahi). Answer/reject button NAHI honge — answer ke liye dialer
  role chahiye, jo announce ka kaam nahi hai; auto-answer kabhi wapas
  nahi aayega.

### 7.2 FALLBACK raasta — Overlay banner (SYSTEM_ALERT_WINDOW)
- Manifest me `SYSTEM_ALERT_WINDOW` pehle se hai; user settings se grant
  kare (Flora khud ON nahi kar sakti). FSI denied ho aur overlay grant
  ho to ringing par ek chhota overlay banner (TYPE_APPLICATION_OVERLAY,
  OverlayMicService pattern) dikhega — naam + Dismiss + Dialer-kholo.
  Dono grants na hon to sirf voice announce chalega (UI nahi) — ye
  honest fallback chain hai, koi teesra hidden raasta nahi banega.
- **Vivo kill par kya hoga:** force-stop / auto-start OFF / battery
  restriction ho to receiver bhi nahi fire hoga — na voice, na UI. Ye
  Batch 3 ki Vivo guidance se hi theek hoga (user-side steps); Flora
  khud Vivo ki kill-policy bypass nahi kar sakti, aur aisa daava kabhi
  nahi karegi.

## §8. SCOPE EXPANSION — adjacent helpful features (verify karke, sirf jo pass kare)

Sirf wahi liya gaya hai jo privacy + permission + no-silent rules pass
karta hai. Baaki deferred/out hai (§6 ke saath padho).

- **Missed-call summary (LIYA GAYA — Batch 4):** receiver state machine
  (RINGING → IDLE bina OFFHOOK ke = missed) par, naye toggle
  `MISSED_ANNOUNCE` (default OFF) ON ho to turant bole: "Boss, {name}
  ki missed call hai" (unknown par "Boss, ek unknown number ki missed
  call hai"). Permission wahi Batch 1 wali (Phone State + Call Log);
  naam contacts se; Auto Log masked (last4). Poochhne par missed list
  ("missed calls batao") planner skill ke roop me Batch 4 me attempt
  hoga — planner-tool pattern fit na ho to deferred likha jayega, jabardasti
  wiring nahi hogi.
- **Important sender ka SMS announce (LIYA GAYA — Batch 2 se covered):**
  SMS app ki notifications NotificationListener se aati hain; user SMS app
  ko important-apps me chune to uski notifications boli jayengi (title
  hi). SMS database kabhi nahi padha jayega — `READ_SMS` permission
  kabhi nahi li jayegi. SMS OTP `AnnounceFilters` se hard-exclude hai
  (OTP SMS kabhi announce/store nahi hoga).
- **Awaaz se "call wapas karo" (LIYA GAYA — Batch 4, sirf dialer):**
  voice intent "X ko call wapas karo" → contact resolve → sirf
  ACTION_DIAL kholega (number bhara hua, user khud call dabayega).
  Direct call (CALL_PHONE se auto-dial) is raaste me KABHI nahi hoga.
  Planner-tool pattern fit na ho to deferred with reason.
- **Driving-mode me announce (LIYA GAYA — Batch 2 ka chhota rule):**
  `DrivingScreen` pehle se driving ON par CALL_ANNOUNCE ON kar deta hai
  (OFF par wapas nahi badalta — maujooda behaviour barkaraar). Is par
  ek rule judenga: driving mode ON ho to notification announce DND
  toggle ko override karke bolega (car context me user sunna chahta
  hai) — sirf DND override, koi naya permission nahi, koi silent
  action nahi (driving mode user khud ON karta hai).
- **Call UI par answer/reject (NAHI LIYA):** dialer role ke bina answer
  possible nahi; CallScreening role alag raasta hai — out of scope (§6).

## §9. REVISED BATCH LIST (Batch 4 jud gaya)

- Batch 1 — Call announce upgrade (commit ec583832, CI 41015668729 SUCCESS, re-audit gate PASS).
- Batch 2 — Notification announce (+ driving DND-override rule). Commit 45023aff + fix d0af98ff (android.provider.Telephony package fix — Sms class android.provider me hai, android.telephony me nahi; isi galti se CI me Unresolved reference aaya tha), CI 41017483287 SUCCESS, re-audit gate PASS (fix diff sirf 1 line, tip-content verified).
- Batch 3 — Reliability + visibility (AnnounceService id 2008, stop,
  boot restore, Vivo guidance). Commit 6686fff7, CI 41018848962
  SUCCESS, re-audit gate PASS (6 files, tip-content verified).
- **Batch 4 — Maya-parity self-activation + missed-call:** manifest
  USE_FULL_SCREEN_INTENT + IncomingCallActivity (FSI) + overlay
  fallback + receiver FSI post + MISSED_ANNOUNCE toggle + missed state
  machine + voice callback-dial + missed-on-ask (attempt; fit na ho to
  deferred with reason). Commit da897bcd + fix 265ec828
  (overlay shadowing trap — §11 addendum), CI 41020768416
  SUCCESS, re-audit gate PASS (tip-content verified). Har batch:
  CI green wait karke agla batch.
- Phone-test me §5 ke saath judenge: locked screen par FSI activity,
  unlocked par heads-up, FSI denied par overlay fallback, FSI + overlay
  dono denied par sirf voice; missed call ke baad summary; driving mode
  me DND override; "call wapas karo" par dialer khulna (auto-dial nahi).

## §10. BATCH 3 — IMPLEMENTED (reliability + visibility)

- **AnnounceService** (`automation/AnnounceService.kt`, NOTIF_ID 2008,
  channel `flora_announce`, specialUse — FloraCharmService mirror):
  foreground notification "Flora announce chalu hai" (tap → Flora
  chat) + "Band karo" action (AnnounceStopReceiver). Khud kuch
  announce NAHI karti — announce ab bhi CallAnnouncementReceiver /
  FloraNotificationListener se hota hai; ye sirf visible presence +
  stop control hai. START_NOT_STICKY: system kill par khud wapas
  nahi aayegi.
- **AnnounceStopReceiver** (`automation/AnnounceStopReceiver.kt`,
  exported=false): sirf service rokta hai, toggles nahi badalta.
  **Semantics (honest, UI me bhi likhi):** "Band karo" = session
  stop; toggle ON rehne par toggle badalne ya phone restart par
  service wapas chalu ho jayegi; poori tarah band karne ke liye
  Call announce + Notification announce dono OFF karo. Stop
  hamesha kaam karega, chahe toggle koi bhi ho.
- **Manifest:** service + receiver entries (specialUse subtype
  "Flora call and notification announce"). Koi naya permission
  nahi — FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE
  pehle se hain. NOTIF_ID 2008 free tha (1001/1002/1003/2002/
  2003/2004/2007 use me hain).
- **BootReceiver restore:** boot par call ya notify toggle ON ho to
  AnnounceService start (try/catch — kuch phones par boot FGS
  block hota hai, fail ho to toggle dobara layega). PHONE_STATE
  receiver aur NotificationListenerService ko boot action nahi
  chahiye (manifest / system-bound).
- **Settings wiring:** call toggle (grant chain ke aakhri step +
  OFF branch) aur notify toggle dono `syncFromPrefs` bulate hain
  (koi announce ON → start, dono OFF → stop). Koi naya state var
  nahi — declaration-order trap nahi. Koi naya DataStore instance
  nahi — FloraPreferences reuse.
- **Settings UI:** "Announce band karo" button (hamesha stop) +
  Vivo guidance text (Auto-start ON + Battery unrestricted,
  Permissions screen buttons ka hawala) + force-stop limit text.
  Vivo steps user khud karega — Flora khud ON nahi kar sakti.
- **Phone-test (Batch 3, §5 me judenge):** toggle ON → notification
  dikhe; "Band karo" → notification gayab, announce ruka; dono
  OFF → service ruki; phone restart (toggle ON) → service wapas;
  service kill (Settings > Apps > Force stop nahi, system kill)
  → announce ruka jab tak boot/toggle na ho; Vivo auto-start OFF
  par announce rukna (expected, user step se wapas).

## §11. BATCH 4 — IMPLEMENTED (Maya-parity self-activation + missed-call)

- **Maya-parity behaviour (user order):** ringing par Flora KHUD
  saamne aati hai — user ko app kholna ya tap karna nahi padta.
  Raasta (sanctioned, koi hidden trick nahi): HIGH-importance
  CATEGORY_CALL notification (id 2009, channel
  `flora_incoming_call`) + `setFullScreenIntent` jab grant ho.
  Android 14+ par FSI grant user Settings se deta hai
  (`canUseFullScreenIntent` check; Settings me "Settings kholo"
  button → ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT); 14 se
  neeche declare karna hi kaafi hai. Lock/off/AOD par FSI
  activity LAUNCH hoti hai; unlocked par heads-up dikhta hai.
  Background se `startActivity` ka raasta istemal NAHI hua
  (Android 10+ par silently drop hota hai) — FSI notification
  hi sanctioned raasta hai.
- **IncomingCallActivity** (`calls/` — CallActivity se ALAG,
  CallHolder reuse NAHI): showWhenLocked + turnScreenOn
  (manifest attrs + code, CallActivity mirror). UI: bada text
  "Boss, {name} calling you" / unknown variant (digits kabhi
  display nahi) + [Wapas call karo] (sirf ACTION_DIAL, number
  bhara hua, user khud call dabayega — auto-dial kabhi nahi)
  + [Dismiss]. Answer/reject buttons NAHI (dialer role ke bina
  answer possible nahi). Number sirf dial ke liye extra me —
  kabhi display/log/server par nahi. Call khatam hote hi
  receiver IncomingCallHolder.onCallEnded se ise band karta hai.
- **Receiver state machine** (CallAnnouncementReceiver): RINGING
  → announce + UI post; OFFHOOK → answered=true + UI clear
  (notification cancel + overlay hide + holder finish); IDLE →
  UI clear, phir agar ring tha aur answered nahi aur
  MISSED_ANNOUNCE ON ho to "Boss, {name} ki missed call hai"
  / unknown variant + masked Auto Log. Uthayi call par missed
  kabhi nahi. 30s RINGING dedup barkaraar.
- **Overlay fallback** (AnnounceService overlay view — naya
  service NAHI): FSI na mile ya notification post na ho to
  overlay koshish (TYPE_APPLICATION_OVERLAY, charm pattern,
  SYSTEM_ALERT_WINDOW pehle se declared). Overlay me wahi
  message + [Wapas call karo] (ACTION_DIAL) + [Dismiss]; call
  khatam hote hi hide. Best-effort: Android 12+ background se
  FGS start block kar sakta hai — tab heads-up + awaaz hi
  rahegi (spec me chhupaya nahi gaya).
- **MISSED_ANNOUNCE toggle** (default OFF): prefs key +
  Settings row (call toggle ke neeche) + FSI grant row.
  POST_NOTIFICATIONS na ho to notification nahi — sirf awaaz
  + overlay koshish. Vivo kill par receiver fire hi nahi hoga
  (Batch 3 guidance hi bachaav — Flora OEM kill bypass ka
  daava kabhi nahi karti).
- **DEFERRED with reason — voice callback-dial + missed-on-ask
  planner skills:** ToolRegistry descriptor-only hai (asli
  execution PlannerTools.execute / FloraApp flows se hai); nayi
  voice intent ("call wapas karo", "missed call batao") ke liye
  intent routing + naya tool execution path chahiye — ye naya
  planner surface hai, is batch me force-wiring mana hai (spec
  §7). User-facing capability phir bhi maujood hai: UI buttons
  se ACTION_DIAL khulta hai. Planner skills ke liye alag
  spec + pre-audit chahiye (future batch, user order par).
- **Phone-test (Batch 4, §5 me judenge):** locked screen par
  FSI activity khulna; unlocked par heads-up; FSI denied par
  overlay (ya heads-up + awaaz agar FGS start block ho);
  POST_NOTIFICATIONS denied par sirf awaaz + overlay koshish;
  FSI + overlay dono denied par sirf awaaz; missed call ke
  baad summary (toggle ON/OFF dono); uthayi call par koi
  missed nahi; Dismiss + Wapas call karo (dialer khulna,
  auto-dial nahi); call khatam hote hi UI/notification/
  overlay ka gayab hona; Vivo kill par kuch na hona (expected)
  aur auto-start ON ke baad wapas chalna.

## §11 ADDENDUM — BATCH 4 FIX + GATE PASS

- **CI fail → fix (honest record):** Batch 4 ka pehla CI run
  41020258020 build step par fail hua — AnnounceService.kt
  overlay code me Kotlin shadowing trap: local `val text`
  (TextView) ne nested `apply` blocks ke andar receiver ki
  `text` property ko shadow kar diya, isliye `text = ...`
  assignments par "Val cannot be reassigned" + "String vs
  TextView" errors aaye (3 jagah). Fix commit 265ec828:
  local ka naam `messageView` + teeno jagah explicit
  `setText(...)` — diff sirf 5+/5- (AnnounceService.kt hi).
  Lesson (Batch 2 ke Telephony package trap jaisa): `apply`
  block ke andar property assignment se pehle check karo ki
  koi outer local us naam ko shadow to nahi kar raha.
- **Re-audit gate PASS (tip 265ec828, CI 41020768416 SUCCESS):**
  Batch 4 files 7 (manifest, receiver, activity, overlay,
  prefs, Settings UI, spec) + fix diff 5/5 — sab tip-content
  verified (FSI attrs + ACTION_DIAL-only + no answer button,
  state machine + FSI + missed key, setText fix, FSI perm +
  activity entry, missed toggle + FSI grant row).
- **Iske baad ka daava:** code + CI + gate PASS — phone-verified
  NAHI. Production-ready ka daava sirf Phase 10 phone tests
  (§5 + §9 + §10 + §11 phone-test lists) ke baad.
