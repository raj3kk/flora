# Flora Feature Flags + Heartbeat (Phase 8, app client)

## Contract (FINAL — panel phase 2 server isi par bana hai)

### Feature flags
- **Endpoint:** `POST {SUPABASE_URL}/functions/v1/feature-flags`
- **Headers:** `apikey: <anon key>`, `Authorization: Bearer <anon key>`
- **Body:** `{}` — ya, linked account ho to, `{"user_id": "<linked uid>"}`
- **Response:** `{"flags": {"talk": bool, "calls": bool, "sms": bool,
  "whatsapp": bool, "browser": bool, "macros": bool,
  "image_gen": bool, "screen_vision": bool, "camera": bool,
  "overlay_mic": bool, "wakeword": bool, "home": bool,
  "routines": bool, "form_fill": bool}}`
- **Server:** global rows base + per-user row per-key override
  (server function panel phase 2 me bana hai; deploy dashboard se hoga).

### FAIL-OPEN (sakht niyam)
Flags na mile, koi key response me na ho, ya network fail ho —
har case me us feature ko **ON** mana jayega. App panel ke bina,
ya server function deploy hone se pehle, kabhi nahi tootegi.
Aakhri mili hui flags phone par cache hoti hain
(`feature_flags_json` pref) — offline me wahi chalti hain.
App-start par `FeatureFlags.refresh(prefs)` chalta hai (silent fail).

### Enforcement points (app code, `FloraApp.kt` + `HomeScreen.kt`)
| Flag | Gate kahan |
|---|---|
| `talk` | Main chat fall-through brain call (sendChat) + routine ka brain step |
| `calls` | `autoExecuteCall` (chat auto call + confirm-dialog call buttons isi se chalte hain) + routine `call` step |
| `sms` | `autoExecuteSms` + routine `sms` step |
| `whatsapp` | `runContactAction` ka whatsapp action (`whatsapp karo` branch isi se chalta hai) |
| `browser` | `browser kholo` + `browser me X kholo` branches + routine `chrome me`/`website` step |
| `macros` | `runMacroFrom` (chat `macro chalao` + Routines screen + confirm-resume sab isi se) |
| `image_gen` | `image generate karo` branch + `image banao` branch (purana ChatGPT/Pollinations raasta bhi) |
| `screen_vision` | `lookAtScreen` + `lookAtScreenLive` (chat + `kholo aur dekho` dono isi se) |
| `camera` | Main chat `photo lo`/`scan karo`/`camera kholo` + routine camera steps |
| `overlay_mic` | `overlay mic chalu karo` branch (band karna kabhi gate nahi hota) |
| `wakeword` | Wake Word Switch ON + mic-permission launcher ka service-start (band karna hamesha chalega) |
| `home` | `HomeScreen` ka Server-sync button + `HomeSyncClient.sync` ke andar bhi |
| `routines` | Poori routine queue (OFF ho to queue saaf + imaandaar message, koi step nahi chalega) |
| `form_fill` | `form bharo` + `field … me … likho` + `form submit karo` branches |

OFF ho to har jagah ek hi imaandaar message bolta hai —
**"Ye feature abhi band hai."** — aur koi action nahi hota.
(Gate helper: `gateFeature()` FloraApp me; SettingsScreen/HomeScreen
seedha `FeatureFlags.isEnabled()` dekhte hain.)

### Kabhi gate nahi hota
- **Settings screen** (recovery path): admin flag galat bhi kar de to
  user app ke andar se sab dekh/sambhal sakta hai. Panel toggles
  sirf features ko gate karte hain, Settings UI ko nahi.
- Feature **band karna** (overlay mic band, wakeword band, unlink):
  OFF karne ka raasta hamesha khula hai.
- File Q&A (`attachedUri` + brain) aur `code likho`: ye explicit
  feature commands hain, free-talk nahi — `talk` flag inhe nahi rokta.

### Heartbeat (presence, panel ka last_seen column)
- **Endpoint:** `POST {SUPABASE_URL}/functions/v1/heartbeat`
- **Headers:** `apikey: <anon key>`, `Authorization: Bearer <user JWT>`
- **Body:** `{"user_id": "<JWT user se match karta uid>"}`
- **Kab:** app foreground par, licence check ke baad, sirf linked
  accounts ke liye. JWT refresh-token flow se memory me liya jata
  hai (`LicenceClient.refreshAccessToken`) — access token kabhi
  save nahi hota.
- **Sirf last_seen timestamp jata hai** — koi content, location
  ya screen data kabhi nahi.
- **Silent-skip:** function 404 de, network fail ho, ya JWT na mile —
  chupchaap skip, app na rukegi, na crash hogi, na user ko koi
  message dikhega. Unlinked accounts heartbeat bhejte hi nahi.

## Honest limits
- Panel toggles **sirf is build (Phase 8) se aage wale APK** par
  asar karenge — purana v2.22 APK flags ko jaanta hi nahi, wo
  toggles ko ignore karega (Phase 10 publish ke baad hi asar).
- Server function deploy hone tak client fail-open chalega (sab ON)
  — panel se OFF karne ka asar deploy ke baad hi dikhega.
- Screen taps se navigation (menu se Scan/Camera screen kholna)
  gate nahi hai — gate voice-triggered feature actions par hai.
