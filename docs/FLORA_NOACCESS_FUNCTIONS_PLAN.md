# Flora no-accessibility functions — PLAN (Phase 0 spec, 2026-10-30)

**Order:** user order 2026-10-30 ~11:27 IST — bina AccessibilityService ke jo
function ho sake, Flora me add karo (torch, volume, time, battery, etc.).
**Status:** SPEC ONLY — implementation Phase 1 pre-audit PASS ke baad hi hogi.
**Domain:** app/ + docs/ only. web/, supabase/, workflows kabhi nahi.
**Gates:** code + CI + tip-content only. Phone verification Phase 10 me hogi —
is plan par kabhi "phone-verified" claim nahi hoga.

## 0. Design rules (sab functions par lagu)

- **Trigger sirf user ka chat/voice utterance hai** — koi background trigger,
  koi silent execution kabhi nahi. Utterance hi intention + confirmation hai.
- **Pipeline mapping (har function):** intention (phrase match) → plan
  (single-step, chat me dikhta hai) → execute (DeviceActions) → verify
  (jahaan possible, state dobara padh kar) → report (chat + TTS, chhota)
  + Auto Log event `[device:<name>] ...`. Ye macros/app-open wale maujooda
  chat-skill pattern jaisa hai — PlannerScreen multi-step plans ka raasta
  nahi (usme PlanTool enum + PlannerScreen UI chahiye hota; v1 me guess
  wiring nahi hogi). ToolRegistry me descriptors darj honge taaki planner
  audit inhe dekh sake (kind `device-local`, licenceKey null — heartbeat /
  clipboard ki tarah, device-local par koi plan licence nahi lagti).
- **4 stops (har function ke andar):**
  - S1 permission/availability gate — fail-CLOSED, imaandaar message.
  - S2 forbidden-action guard — password typing, payment auto-confirm,
    silent third-party reply, gestures/taps is raaste se kabhi nahi.
  - S3 verify-after-execute — jahaan re-read possible hai, wahi karo.
  - S4 visible report + Auto Log — har action chat + TTS + log me.
- **Fail-closed, fail-honest:** koi exception aaye to user ko saaf wajah
  (permission nahi / hardware nahi / OS ne roka), kabhi jhootha "ho gaya"
  nahi. Reply style: warm normal talk, technical talk nahi (v2.20 rule).
- **Battery read rule (binding):** BatteryManager / sticky intent sirf
  `assistant/BatteryState.kt` me hai (re-audit grep proof). Battery % skill
  `BatteryStateReader.read()` ko reuse karegi — doosri file me battery
  read kabhi nahi jodegi.
- **Parsing modesty:** alarm/timer/dial me sirf ASCII digits parse honge
  (alarm: "N baje" + subah/shaam; timer: "N minute/second"; dial: 6–15
  digits). Parse na ho to intent bina extras ke khulega + reply me user
  ko khud bharne ko kaha jayega — galat time/number guess kabhi nahi.
- **Intent flag:** appContext se startActivity par FLAG_ACTIVITY_NEW_TASK
  lagega (application context rule).

## 1. Function table

### Batch A — read-only info + intent actions (koi state change nahi)

| # | Function | API (minSdk 26 / targetSdk 34 par verified) | Permission class | Grant flow | Verify | Auto Log |
|---|---|---|---|---|---|---|
| A1 | Time/date/day batao | java.time LocalDateTime.now + day-name map (Somvaar..Ravivaar) | none | — | self (system clock) | `[device:time]` |
| A2 | Battery % + charging | `BatteryStateReader.read()` (EXISTING) | none | — | sticky intent se hi | `[device:battery]` |
| A3 | Device info (model, Android version) | Build.MODEL / MANUFACTURER / VERSION.RELEASE / SDK_INT | none | — | self | `[device:info]` |
| A4 | Storage free/total | StatFs(filesDir.path) | none | — | self | `[device:storage]` |
| A5 | RAM free/total | ActivityManager.getMemoryInfo | none | — | self | `[device:ram]` |
| A6 | Network type (Wi-Fi / mobile / none) | ConnectivityManager.getNetworkCapabilities | normal: ACCESS_NETWORK_STATE (manifest add) | install-time, koi runtime UI nahi | self | `[device:network]` |
| A7 | Wi-Fi ON/OFF state read (sirf read, toggle nahi) | WifiManager.isWifiEnabled (deprecated par state deta hai) | normal: ACCESS_WIFI_STATE (manifest add) | install-time | self | `[device:wifi-state]` |
| A8 | Alarm lagao (clock app me prefill) | AlarmClock.ACTION_SET_ALARM + hour/minute/message | none (intent) | user clock app me confirm karega | n/a (intent khula) | `[device:alarm-intent]` |
| A9 | Timer lagao (clock app me prefill) | AlarmClock.ACTION_SET_TIMER + seconds | none (intent) | user clock app me confirm karega | n/a | `[device:timer-intent]` |
| A10 | Dialer kholo (number ke saath ya khaali) | ACTION_DIAL + tel: URI (sirf ASCII digits + +) | none (intent, CALL_PHONE nahi) | user khud call button dabayega | n/a | `[device:dial-intent]` |
| A11 | Settings pages kholo (Wi-Fi, Sound) | ACTION_WIFI_SETTINGS / ACTION_SOUND_SETTINGS | none (intent) | user khud setting badlega | n/a | `[device:settings-intent]` |

### Batch B — torch (state change, runtime permission)

| # | Function | API | Permission class | Grant flow | Verify | Auto Log |
|---|---|---|---|---|---|---|
| B1 | Torch on/off | CameraManager.setTorchMode + cameraId (back camera, FLASH_INFO_AVAILABLE true) | runtime: CAMERA (manifest me hai; wizard row hai) | Na mile to reply + PermissionsScreen par bhejo (Screen.PERMISSIONS maujood hai); Flora kabhi zabardasti nahi maangegi | in-memory lastTorchState (process-local; caveat neeche) | `[device:torch]` |

- **Torch caveats (imaandaari se):** CameraManager me torch state ka getter
  nahi hai — isliye "torch on hai ya off" ka jawab sirf is process me
  Flora ke aakhri action se diya jayega; user ne bahar se badla ho to
  jawab "pata nahi, dobara karke dekhta hun" hoga, guess nahi.
- **Fail-closed:** koi camera nahi / flash nahi / CameraAccessException /
  SecurityException → "Torch is phone me nahi hai" ya "Camera permission
  chahiye" — kabhi jhootha "jal gaya" nahi.
- **PermissionsScreen label:** CAMERA row ka label "Scan/photo ke liye" se
  "Scan/photo/torch ke liye" hoga (ek line ka sachha label sudhaar).

### Batch C — volume + ringer (state change, normal + grant-gated)

| # | Function | API | Permission class | Grant flow | Verify | Auto Log |
|---|---|---|---|---|---|---|
| C1 | Volume % padho (media/alarm/ring) | AudioManager.getStreamVolume/maxVolume | normal: MODIFY_AUDIO_SETTINGS (manifest add) | install-time | re-read % | `[device:volume]` |
| C2 | Volume set karo (% se) / badhao / kam karo | setStreamVolume / adjustStreamVolume(RAISE/LOWER) | normal: MODIFY_AUDIO_SETTINGS | install-time | set ke baad re-read % | `[device:volume]` |
| C3 | Ringer mode padho + normal/vibrate set karo | getRingerMode / setRingerMode | normal: MODIFY_AUDIO_SETTINGS | install-time | re-read mode | `[device:ringer]` |
| C4 | Silent (DND) — GRANT-GATED | setRingerMode(SILENT) sirf tab jab NotificationManager.isNotificationPolicyAccessGranted true ho | special: Notification Policy Access (system settings page) | Na mile to imaandaar message + ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS page kholo (user khud allow karega, phir dobara bole) — bypass kabhi nahi | re-read mode | `[device:dnd]` |

- **Number parsing:** sirf ASCII digits + `%`/`percent` shabda; number na mile
  to poochho ("Kitne percent kar dun?"), guess nahi. Badhao/kam karo =
  adjustStreamVolume step (OS step, % guess nahi).
- **Ring volume note:** DND ON ho to ring set OS rok sakta hai — catch karke
  imaandaar message, silent claim nahi.

## 2. EXCLUDED — wajah ke saath (is plan me kabhi nahi banenge)

- **Screen taps/swipes/gestures** — Accessibility-gated, SD-5 domain hai;
  yahaan banana duplicate + guard-bypass hota.
- **Screen viewing/screenshots** — item 37 Accessibility-only, pehle se
  built hai; dobara raasta nahi banega.
- **Silent auto-replies / silent third-party actions** — NEVER (G19).
- **Payment auto-confirm** — NEVER.
- **Web password typing/autofill** — B1 guard intact, kabhi nahi.
- **Wi-Fi/Bluetooth direct toggles** — Android 10+ (Wi-Fi) / 31+
  (Bluetooth) par app toggle OS-blocked hai; Settings Panel intent bhi
  v1 me nahi (scope) — sirf state read + settings page intent hai.
- **Bluetooth state read** — DEFERRED v2 candidate: BLUETOOTH_CONNECT
  runtime permission + wizard row chahiye hota; v1 ko saaf rakhne ke
  liye nahi joda gaya (reason likhit hai, bhoola nahi gaya).
- **Own-window brightness** — DEFERRED: Activity window plumbing
  chahiye hoti (FloraApp composable se seedha nahi); v1 me nahi.
- **Device-admin lock** — heavy + out of scope; kabhi silent lock nahi.

## 3. Phrase matching (IntentRouter.Route.Device, route() me SABSE PEHLE)

Device phrases specific multi-word patterns hain — isliye device match
OpenApp ("kholo" suffix), Research, Plan se PEHLE check hoga, warna
"wifi settings kholo" / "dialer kholo" OpenApp me galat gir jayenge.
Match sirf poori phrase patterns par hoga (akele "torch" / "volume"
shabd par nahi — ambiguous hai, brain par girne do).

- Time: time batao / time kya hai / kitne baje / samay batao / date
  batao / aaj kaun sa din / what is the time / tell me the time.
- Battery: battery kitni / battery percentage / battery kitna / battery
  level / battery status.
- Info: phone ka model / device info / android version / storage kitni /
  kitni jagah / ram kitni / memory kitni / network kaun sa / internet
  kaun sa / wifi on hai (state).
- Torch: torch on / torch jalao / flashlight on / torch off / torch
  band / flashlight off.
- Volume: volume kitna / volume badhao / volume kam karo / volume
  ghatao / volume <digits> / volume <digits> percent / media volume /
  alarm volume / ring volume / silent karo / vibrate karo / ringer normal.
- Intents: alarm lagao / timer lagao / dial karo / dialer kholo / wifi
  settings kholo / sound settings kholo.

## 4. Files (ek push chain, serialized batches)

- **Batch A (naya + 3 edits):** `tools/DeviceActions.kt` (NAYA — sab
  functions, SdResult<String> replies), `tools/IntentRouter.kt` (Route.Device
  + match, route() me pehle), `planner/ToolRegistry.kt` (device-local
  descriptors, licenceKey null), `ui/FloraApp.kt` (Device branch —
  route() ke turant baad, Research se pehle; messages + voice.speak +
  autoLogStore.add pattern maujooda branches jaisa), `AndroidManifest.xml`
  (3 normal permissions: MODIFY_AUDIO_SETTINGS, ACCESS_NETWORK_STATE,
  ACCESS_WIFI_STATE), `tools/DeviceMatchTest.kt` (NAYA unit test —
  match() pure logic par).
- **Batch B:** DeviceActions me torch + FloraApp torch phrases (branch
  wahi Device branch hai, naya branch nahi) + PermissionsScreen CAMERA
  label sudhaar.
- **Batch C:** DeviceActions me volume/ringer/DND + FloraApp volume
  phrases (wahi Device branch).

## 5. Kotlin traps checklist (pre-audit me verify, push se pehle)

- FloraApp me `voice` param hai (branch FloraApp ke andar hai — theek);
  SettingsScreen ko chhua nahi jayega (PermissionsScreen label edit alag
  composable hai, uske params nahi badlenge).
- Declaration order: branch me use hone wale vars (autoLogStore line 315,
  voice, appContext) branch se PEHLE declare hain — grep line numbers
  se verify.
- Nullable state copies: `val x = nullable ?: return` pattern; sirf
  if-check par bharosa nahi.
- Koi var + same-name fun clash nahi (DeviceActions object me private
  var lastTorchState, funs alag naam).
- Koi greedy sed nahi — sirf edit tool; har edit ke baad brace/paren
  balance grep se verify.
- FloraApp.kt poori file push hoti hai — edit se pehle live-tip match
  (collision protocol) har batch me dobara verify.

## 6. Phone-test steps (Phase 10 — is plan par abhi claim nahi)

1. "Time batao" → sahi time/day jawab + Auto Log entry.
2. "Battery kitni hai" → % + charging state sahi.
3. "Storage kitni khaali hai" / "RAM kitni" → numbers device Settings
   se match karein.
4. "Wi-Fi on hai?" → sahi state; "Wi-Fi settings kholo" → settings
   page khule.
5. "Torch on karo" (CAMERA grant ke baad) → flash jale + Auto Log;
   permission deny par imaandaar message + PermissionsScreen raasta.
6. "Volume 50 percent karo" → media 50% + re-read confirm; "Volume
   badhao" → ek OS step up.
7. "Silent karo" (DND grant ke baad) → silent + re-read; grant na ho
   to settings page + dobara bolne ka message (bypass nahi).
8. "Alarm lagao subah 7 baje" → clock app prefill ke saath khule.
9. "Dialer kholo" → dialer khule, call khud na lage.
10. Har action ke baad Auto Log me `[device:...]` entry maujood ho.
