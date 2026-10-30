# FLORA — MAYA PARITY GAP LIST (parity cycle 2026-10-31)

**Tip jiske khilaaf verify hua:** `366be9f4` (CEO cycle plan v2) — file
existence live git-tree se, content local working copy se (jo tip se
mila — ScanScreen/gradle diff me tip-move nahi tha). **States:**
HAVE = code tip par hai (phone-verified NAHI — Phase 10 baaki hai);
PARTIAL = aadha hai; MISSING = code-doable, plan row chahiye;
GATED = user/server/phone decision ke bina band nahi hoga;
EXCLUDED = standing rule se kabhi nahi banega;
DIVERGENT = Flora jaanboojh kar alag hai, ye gap nahi hai.

**Sources:** `MAYA_FULL_AUDIT_20261028.md` (Maya v5.2.44 static audit),
`REBUILD_SPEC_178.md` (124 + 22 + 32), 23:42 IST Maya-vs-Flora
comparison list (`Flora-Pura-Audit-Maya-Comparison.md`), plan v2 §3K
(K11 mapping row — is doc se K11 MAPPED hota hai, confirmation
parent ke paas), `FLORA_FIX_CYCLE_REGISTER.md`.

> Clean-room note: ye doc sirf functionality apne shabdon me likhta
> hai — creator code, UI text, assets, character, logo, trademark
> kuch copy nahi hua hai.
>
> Direction update 2026-10-31 ~01:05 IST (user order, brain ke liye
> 00:48 wale server-shift order ko palta hai): brain APP ME rahega
> (app→providers direct PRIMARY, server brain-proxy sirf FALLBACK);
> Maya ke APK se WHOLESALE COPY authorized NAHI hai (user ke khud
> ke clean-room law + copyright — decompiled code ko dobara working
> banana waise bhi poora re-engineering hai, creator servers dead
> hain, signing key lost hai); UI frontend = behavior-matched
> rebuild, Flora ke apne code/text/layout se. Pacing (step-by-step
> wait) top parity item hai. Urgency note: paying ex-Maya user ko
> Flora jald dene ka vaada hua hai — candidate build di ja sakti
> hai, par "phone-verified / publish-ready" ka daava bina Phase 10
> phone evidence ke kabhi nahi hoga (K12 list/terms abhi bhi gated
> hai — kaun user hai, ye naam se bataya nahi gaya, isliye kisi
> vyakti ko delivery ka vaada invent nahi kiya gaya).

## A. HAVE — Flora me code tip par hai (phone test Phase 10 me baaki)

| Maya cheez | Flora evidence (tip) |
|---|---|
| Voice: system TTS + personas | `assistant/VoiceEngine.kt` (persona profile, hi-IN/EN fallback); Lily pitch 0.95/rate 0.88 profile |
| Voice: brain chain | `assistant/BrainClient.kt` Gemini→Groq + `planner/PlannerBrainProxyClient.kt` server-prefer + local fallback |
| Voice: STT lanes (system + Deepgram client) | `voice/ContinuousListener.kt` (MAX_RESULTS=5, pickBestResult), `voice/DeepgramClient.kt`, `voice/SttPick.kt`, `voice/SttLanes.kt` |
| Voice: neural TTS code | `assistant/NeuralSpeaker.kt`, `models/KokoroOrtEngine.kt`, `models/KokoroModelManager.kt` (runtime download, SHA-verify, `.ok` marker; APK me bundle kabhi nahi) — Gate 6 HELD, phone evidence baaki |
| Voice: wakeword + enrol + guardian | `wakeword/WakeWordMFCC.kt` (template v0) + `WakeWordService.kt` + `WakeWordRecorder.kt`; `voice/VoiceMatcher.kt` + `ui/VoiceEnrolScreen.kt` + `voice/VoiceVault.kt`; `security/GuardianGate.kt` + `security/PinVault.kt` (voice+PIN, 15-min idle → re-PIN; face alone kabhi unlock nahi) |
| Voice: barge-in / energy gating | `voice/BargeInController.kt` (RMS → NotchState) — model-based VAD nahi, energy gating hai |
| Voice: offline basic talk | `assistant/BasicTalk.kt` (bina AI ke local) |
| Voice: assistant role (long-press home) | `voice/FloraVoiceInteraction.kt`, `voice/FloraRecognitionService.kt`, `voice/FloraAssistActivity.kt` + manifest entries — user ka system-dialog pick GATED hai |
| Chat / Home / Memories / Scan / Mic | `ui/HomeScreen.kt`, `ui/FloraApp.kt` chat, `data/MemoriesStore.kt`, `ui/ScanScreen.kt` (CameraX photo + ML Kit OCR Latin/Devanagari), `voice/BackgroundMicService.kt` |
| Whiteboard | `ui/WhiteboardScreen.kt` (drag-draw + rang + Undo/Saaf + voice shape-text) |
| Documents (notes) | `ui/DocsScreen.kt` + `data/DocsStore.kt` (add/edit/delete, on-device) |
| Journal / routines / tasks | `ui/JournalScreen.kt`, `ui/RoutinesScreen.kt` + `data/RoutineStore.kt`, `ui/TaskListScreen.kt` |
| Planner + skills engine | `planner/PlannerEngine.kt`, `planner/PlannerBrain.kt:62 generate`, `planner/GoalOrchestrator.kt`, `planner/LearnedEngine.kt` (learned patterns + skill engine, LLM-retraining ka daava kabhi nahi), `ui/TeachOnceScreen.kt` (learned skills list) |
| Builder | `ui/BuilderScreen.kt` (editor surface) + PlannerBrain AI path + `data/ProjectStore.kt` + `ui/DoctorScreen.kt` |
| Teach-once / vault | `ui/TeachOnceScreen.kt`, `ui/VaultScreen.kt`, `tools/ClipboardCapture.kt` (consent-gated) |
| WhatsApp alert + group report (consent-gated) | `ui/CommConsentScreen.kt` (alert composer — user Send dabata hai; group report — user group chunega + Send dabayega); `data/PostHistoryStore.kt` (post history) |
| Connectors (app-direct) | `tools/SdConnectors.kt` (Telegram bot, Todoist, Linear) + `ui/SdConnectorsScreen.kt`, `ui/ConnectServicesScreen.kt` |
| Call / notify announce (Maya se aage) | `calls/CallActivity.kt` (3-option) + `calls/FloraInCallService.kt` + `calls/IncomingCallActivity.kt` + `automation/AnnounceService.kt` + `automation/FloraNotificationListener.kt` + `automation/AnnounceFilters.kt` (OTP hard-exclude) |
| In-app browser (G7) | `ui/BrowserScreen.kt` (WebView, SSL-warning dialog, download-confirm, password-save nahi) + `tools/BrowserController.kt`, Screen.BROWSER wiring (`ui/FloraApp.kt:103,142,1097,1656,4363,4392,4715+`) |
| Live wallpaper (G9) | `automation/FloraWallpaperService.kt` (orb drawing engine) + `res/xml/wallpaper_service.xml` |
| Floating charm + edge glow + orb | `automation/FloraCharmService.kt` (overlay), `ui/EdgeGlow.kt` (EdgeGlowBorder), `ui/FloraOrb.kt` (canvas, on-device art) |
| Lock (soft, PIN-escapable) | `ui/LockScreen.kt` (PIN gate, app-wide lock nahi — Back se Chat wapas) + `security/PinVault.kt` (failed-count + exponential lockout + imaandaar lockout message) |
| Face presence + enrol (face kabhi unlock nahi) | `ui/FaceScreen.kt` + `core/FaceMatcher.kt` (template encrypted on-device) |
| Avatar catalog (apna art) | `ui/AvatarScreen.kt` (on-device Compose, koi download nahi, koi creator asset nahi) |
| SOS | `ui/SosScreen.kt` + `data/SosStore.kt` (CALL_PHONE + SEND_SMS user grants ke baad) |
| Driving mode | `ui/DrivingScreen.kt` (toggle; bina confirmation ke koi message/call nahi) |
| Permissions wizard | `ui/SetupWizard.kt` (mic/notification/camera/call/contacts/SMS/phone-state/location rows, user khud Allow karega) |
| Camera look | `ui/CameraLookScreen.kt` + `viewing/LiveScreenViewing.kt` (Accessibility screenshots, indicator + status) |
| Catcher (full copy-able report) | `ui/CatcherScreen.kt` + `data/CatcherEvents.kt` + `data/CatcherSanitize.kt` + `data/CatcherReport.kt` (koi auto-upload nahi, secrets masked) |
| Auto Log + ongoing notification (Maya se aage) | `data/AutoLogStore.kt` + `ui/AutoLogScreen.kt` + `automation/AutoModeNotifier.kt` |
| Encrypted memory (Maya se aage) | `data/MemoryCrypto.kt` (KeyStore AES/GCM, memories chunk 1) |
| Backup + backup exclusion (G0) | `backup/FloraBackupManager.kt` + `backup/DriveClient.kt` + `backup/FloraBackupCrypto.kt`; manifest `allowBackup="false"` |
| Accessibility config (G0) | `res/xml/accessibility_service_config.xml`: `canPerformGestures="true"` + `canTakeScreenshot="true"` |
| Licence rule: 1 phone / 15 din me 1 change | `supabase/functions/licence-verify/index.ts` header + v2 logic (15 din poore na ho → device_locked + free; 15 din poore → TRANSFER) — server code; deploy session #2 se live hoga (is cycle me supabase/ chhua nahi) |
| Free cap 600s/day | `data/LicenceGate.kt` + brain/tts-proxy code gates (cap paar par 429) |
| Update client + server | `update/AppUpdate.kt` (version.json, versionCode compare) + `web/updates/version.json` (vCode 45) — channel K2/K3/K4 par pending |
| Cert pinning (apne domains) | `core/PinnedTrust.kt` (sirf Flora ke Supabase + Vercel domains) |
| OAuth (GitHub) | `tools/BuilderOAuth.kt` (authorization-code + PKCE, GitHub repo scope) |
| In-app search / one-tap fix / form filler | `automation/InAppSearch.kt`, `automation/OneTapFix.kt`, `automation/FormFiller.kt` |
| No-access + thermal | `tools/DeviceActions.kt`, `tools/ThermalThrottle.kt` |
| Boot restart | `automation/BootReceiver.kt` |
| Study (basic) | `ui/ExtraScreens.kt` (topic → short summary + sources, DuckDuckGo, bina key) |
| Music control | `tools/MusicControl.kt` + `ui/FavSongsScreen.kt` + `data/FavSongStore.kt` |
| Image gen (Flora route) | `automation/ImageGenFlow.kt` (verified app-open → prompt → generate; user-key/app route, Pro-Max-only service nahi — §F dekho) |
| Notifications read-only | `automation/FloraNotificationListener.kt` + `ui/NotificationsScreen.kt` + `data/NotificationStore.kt` |
| Incognito | SD-8 8.6: `data/FloraPreferences.kt` (`sd8_incognito`) + `ui/FloraApp.kt:428` + `ui/Sd8SettingsScreen.kt` (session turns memory me nahi) |
| Earpiece mode | `calls/FloraInCallService.kt` (ROUTE_EARPIECE + ROUTE_SPEAKER) |
| Shizuku bridge (Termux NAHI hai) | `automation/ShizukuBridge.kt` (user ki Shizuku app ke through, bina root ke) — §E me Termux exclusion se alag hai |

## B. PARTIAL — aadha hai, baaki gate/sequence par

| Cheez | Hai | Baaki / gate |
|---|---|---|
| Wakeword neural | MFCC template v0 + recorder + service | Trained 5-model neural — user ki "Hey Flora" recordings pending (bina recordings ke "done" kabhi nahi) |
| Hunter on-device STT | System STT + Deepgram client + stt-proxy repo-only | On-device JNI STT engine absent — stt-proxy deploy session #2 me, Deepgram key user-gated |
| Pocket on-device TTS | Kokoro code-landed | Gate 6 HELD — 7 phone cases + Case 7 approval + Step 1 UI check + Phase 10 pass ke bina gate nahi khulega |
| Turn / addressee | BargeIn energy gating | Turn-detector + addressee models absent (neural track, recordings/models ke saath) |
| VAD model | RMS energy gating | Silero-jaisa VAD model absent |
| Languages | Hindi primary (hi-IN/EN fallback); Sd8 8.3 me baaki bhashayen DISABLED + imaandaar note | Doosri bhashayen — G2P/voice support records me UNVERIFIED hai, isliye force nahi (HindiG2P clean-room hai; har bhasha ka G2P alag verify hoga) |
| Personas | Flora + romantic (18+ gated) | Creator personas (Friday/Venom/Jarvis) kabhi copy nahi honge — Flora ke apne personas hi scope hain |
| 3D orb / characters | 2D Compose orb + avatar catalog | 3D paid characters creator assets the — copy nahi honge; Flora ka 3D (agar chahiye to) apne art se, alag spec |
| Docs | Notes add/edit/delete | Excel/PDF export — M8 (scope decision: format + share) |
| Face suite | Presence + enrol + failed-count/lockout | Watchman / morning welcome / gallery face scan / dedicated face-attempts log screen — M3 + plan rows |
| Email | — | Email read/reply consent-gated — M9 (Gmail OAuth decision K10 par gated) |
| Sub-agents UI | GoalOrchestrator + learned skills | Named `settings_subagents` screen absent — helper choice user se (G12) |
| Home hardware | Phone half + `tools/HomeSyncClient.kt` + home-sync function | Server half (websocket sync + genuine checks) — user decision pending (SD-P5 LAST) |
| PC link | `ui/PcLinkScreen.kt` phone half (pairing code taiyaari) | PC client — phone publish ke baad |
| Update channel | Client + version.json vCode 45 | K2 bucket + K3 workflow step + K4 apkUrl/sha256 fill + K5 live demo |
| Encryption extend | Memories encrypted | Journal/docs/routines/jobs plain app-private — G18 follow-up (builder work, user-visible flow nahi badlega) |
| Multi-key | Gemini→Groq live | OpenRouter third leg — SD-P2 server track, alag deploy |
| Image gen service | App-open route | Pro-Max-only image service Maya me tha — Flora me licence tiers Bundle B me pending, isliye service-gate abhi tay nahi |
| QR/barcode scan | Photo + OCR (Latin/Devanagari) | **M1 — is cycle me implement hua (neeche §H)** |

## C. MISSING — code-doable, kram se (plan rows)

| # | Cheez | Sequence / note |
|---|---|---|
| M1 | QR / barcode live scan (Maya `qr_scan` route) | **IS CYCLE ME KIYA GAYA** — ScanScreen me QR mode + ML Kit barcode dep (§H) |
| M2 | Reminders UI (Maya ReminderReceiver + alarms) | **PRODUCTION BATCH ME KIYA GAYA** — alag ReminderStore (JobStore overwrite risk se bachav) + ReminderScheduler + ReminderAlarmReceiver + RemindersScreen + BootReceiver reschedule + Manifest entry + FloraApp route. Fire par sirf notification, koi routine/action kabhi nahi. Commit `e93bd58a`. CI gate pending. |
| M3 | Face-attempts log screen (Maya `settings_face_attempts`) | **PRODUCTION BATCH ME KIYA GAYA** — PinVault me public `failedCount()` + FaceScreen me guard-status section (failed count + lockout status + refresh). Commit `b34f47d8`. CI gate pending. |
| M4 | My pictures screen (Maya `my_pictures`/`photos_of`) | **PRODUCTION BATCH ME KIYA GAYA** — PicturesScreen, MediaStore Pictures/Flora read-only listing (Scan/Whiteboard/ImageGen ki confirmed save location); tap par system viewer; koi Coil dep nahi. Commit `b34f47d8`. CI gate pending. |
| M5 | Event triggers (Maya `event_triggers`) | **PRODUCTION BATCH ME KIYA GAYA — Flora-new time-only v1** — Maya semantics UNVERIFIED the, isliye spec pehle (`docs/FLORA_EVENT_TRIGGERS_SPEC.md`), phir TriggerStore + TriggerScheduler + TriggerAlarmReceiver + TriggersScreen + FloraApp/BootReceiver/Manifest wiring. Action `routine` (due-flag, app-open rule) ya `message` (notification) hi; call/SMS/payment/delete trigger kabhi nahi. Commit `d0155ad9`. CI gate pending. |
| M6 | Marketing 8.9 (SD-8) | **SPEC ONLY — build NAHI** — marketing ka content vyavhaar records me UNVERIFIED hai (sirf menu naam + MarketingAlarmReceiver naam mila); bina definition ke marketing tool banana silent-action risk hota. Spec `docs/FLORA_MARKETING_8_9_SPEC.md`. Build tabhi jab verified vyavhaar mile ya user definition de. |
| M7 | Markets / trading (Maya `markets`, ~32 tools DAAVA) | **PRODUCTION BATCH ME KIYA GAYA — read-only watchlist** — spec `docs/FLORA_MARKETS_SPEC.md` + WatchlistStore + MarketsScreen (maujooda Yahoo Finance lane, bina key). "~32 tools" daava kabhi fact nahi — screen par bhi nahi. Koi trading/buy/sell/advice kabhi nahi. Commit `26c8c637`. CI gate pending. |
| M8 | Docs Excel/PDF export | **PRODUCTION BATCH ME KIYA GAYA — scope decided + implement** — scope: CSV (Excel-compatible) + PDF via Android PdfDocument (built-in, koi POI dep nahi), Downloads/Flora me MediaStore se save, user-tap buttons DocsScreen me. Koi auto-share/server upload nahi. Commit `26c8c637`. CI gate pending. |
| M9 | Email read/reply (consent-gated) | **GATED — implement NAHI** — Gmail OAuth decision K10 par gated; report me gated hi rahega jab tak decision na aaye. |
| M10 | Typing mode / media mode (Maya settings) | **PRODUCTION BATCH ME KIYA GAYA — Flora-new definitions** — Maya definition nahi mili, isliye spec `docs/FLORA_TYPING_MEDIA_MODES_SPEC.md` pehle, phir prefs + Settings toggles + chat Mic gate (typing) + system-TTS stream (media: ON = media stream default, OFF = voice-call stream; neural path par nahi). Commit `d716f0d2`. CI gate pending. |
| M11 | Driving-mode SMS | **PRODUCTION BATCH ME KIYA GAYA** — DrivingScreen me SMS section, ACTION_SENDTO draft (Send user khud apne SMS app me dabayega; silent send kabhi nahi, SEND_SMS permission nahi). Commit `1ce9a315`. CI gate pending. |
| M12 | Notices / vibes routes (Maya cosmetic routes) | **PRODUCTION BATCH ME KIYA GAYA** — Screen.NOTICES (= NotificationsScreen alias route) + AvatarScreen me Vibes section (mood naam → wahi avatar rang; naya art nahi). Commit `1ce9a315`. CI gate pending. |

## D. GATED — cycle force nahi karega

- Wakeword recordings (user) · Deepgram key + security code (user) ·
  Gmail OAuth decision (user) · Home server-half decision (user) ·
  PC companion decision (user) · Auto-reply per-app opt-in decision
  (G6 — recommended default: read-only hi rahe) · Sub-agent helpers
  choice (G12) · OAuth providers beyond GitHub (G14) · Public-link
  hosting decision (G15) · Grievance officer asli naam (post-launch
  deferred, fake kabhi nahi) · Phase 10 phone tests + publish sign-off
  (har HAVE item ka phone-verified isi par) · Deploy session #2
  (parent-browser, is cycle me chhua nahi) · Thin RELEASE APK
  (user-attention session, unsigned) · Update channel K2/K3/K4 ·
  Ex-Maya users commercial terms K12 · R4-1/R4-9 design decisions ·
  Licence tiers/prices Bundle B (prices kabhi invent nahi) · Dialer
  call-talk decision (default dialer user bataye) · S1-20 cap number.

## E. EXCLUDED — standing rule, dobara litigate nahi karna

| Cheez | Wajah (ek line) |
|---|---|
| Maya cross-app JSON macros (5 built-in) | ToS risk — copy nahi kiye; Flora ke routines teach-by-doing + Auto Log wale hain |
| WRITE_SECURE_SETTINGS self-enable / self-repair | Copy karna MANA hai — Flora rule: user khud toggle karega + visible stop |
| Creator voice cloning | Kabhi nahi — Flora ki awaaz apne engine/profile se hai |
| Auto-answer | Hataya gaya, wapas NAHI |
| SIM-call TTS injection | G19 impossible — Android public API se reliable uplink injection nahi hota |
| WhatsApp call API | G19 impossible — koi official API nahi hai |
| Silent auto-reply / silent auto-posting / silent impersonation | NEVER by decision — sirf consent-gated, user-Send wale forms |
| Play Store update | Signing key LOST = fact — Flora hamesha sideload-only |
| Purani creator keys / backend / data / signing key reuse | Access gone + clean-room — kabhi reuse nahi |
| Termux bridge (RUN_COMMAND + result receiver) | N11 user-approved cleanup CLOSED — dobara add nahi hoga (ShizukuBridge alag cheez hai, wo rehta hai) |
| Hard God-Mode lock | Standing exclusion — sirf soft, PIN-escapable lock |
| Creator code / UI text / assets / character / logo / trademark copy | Clean-room rule — function seekh kar dobara banao |
| Silent recording / silent screen viewing | Consent + indicator + Auto Log ke bina kabhi nahi (item 37) |
| Web password autofill / typing | B1 guard — Flora password type/autofill kabhi nahi karegi |
| Locked-phone par post daalna (video claim) | Risky claim — copy nahi kiya |
| "Green wire = phase" | Khatarnak galti — India me green aam taur par earth hai, kabhi copy nahi |
| Creator daave fact ki tarah (700+ tools, 89%+ accuracy, $200 credit, ₹29 hosting, bill accuracy) | Unverified claims — Flora ke apne metrics honge, daave nahi |

## F. DIVERGENT BY DESIGN — gap nahi hai, Flora jaanboojh kar alag hai

| Cheez | Flora ka raasta |
|---|---|
| Screen dekhna (Maya: MediaProjection + Accessibility dono) | Item 37 v2 spec FROZEN: Accessibility-screenshot only (consent + indicator + Auto Log) — poore-app MediaProjection nahi aayega |
| Licence model (Maya: ₹699 lifetime + Pro Max ₹99/mo) | Flora: ₹199/month FINAL, apna Supabase + UPI + admin UTR-match — Maya ka system kabhi reuse nahi |
| Baat-cheet ka lock (Maya: talk NOT licence-locked, sirf agentic gated) | Flora: free 600s/day cap + Pro unlimited — apna model, code-gate ke saath |
| Face unlock (Maya: Face Lock) | Flora sakht hai: face kabhi unlock nahi, sirf PIN — presence/enrol sirf pehchaan ka test hai |
| Personas / 3D characters (Maya: Friday/Venom/Jarvis, paid 3D) | Flora ke apne personas + on-device Compose avatars — creator character kabhi nahi |
| Update (Maya: in-app auto-update + Check button) | Flora: version.json explicit channel (koi auto-check/download/install nahi — har kadam user ke tap par) |
| Data backup (Maya: allowBackup=false + saare domains excluded) | Flora: allowBackup=false — yahan barabar hai (§A) |
| Memory encryption | Flora aage hai: KeyStore AES/GCM (Maya me at-rest encryption ka recovered proof nahi mila) |
| Transparency | Flora aage hai: Auto Log + ongoing notification har action ke liye (Maya me equivalent surface nahi mila) |
| In-call UI | Flora aage hai: 3-option CallActivity + InCallService (Maya ke paas in-call UI nahi tha) |

## G. K11 MAPPING — comparison-list (Section 5) ke har row ka pata

| Comparison row | Is doc me | Plan row / state |
|---|---|---|
| Overall product state, features chaudai | §A (HAVE code) + §D (Phase 10) | F15 Phase 10 + sign-off |
| Voice quality / latency | §A voice + §B Kokoro/Gate 6 | SD-P3 neural track; K9 TTS probe |
| Wake word | §B | Recordings user-gated |
| STT / TTS lanes | §A + §B | K8 STT verify, K9 TTS probe, K10 Gmail |
| Tools / automation count | §A planner/skills + §C M5 | 700+ daava §E me excluded-as-fact |
| Smart home | §B Home | SD-P5 LAST |
| Calling | §A (Flora aage) | Dialer decision §D |
| Messaging | §A WhatsApp consent + §C M9 email | P2 bundle; silent forms §E |
| Licence / pricing | §A licence rule + §F model | Bundle B tiers §D; K12 ex-Maya terms |
| Free limit | §A cap | Live JWT probe §D |
| Phone-change rule | §A licence-verify v2 | Deploy #2 se live (chhua nahi) |
| App size / performance | §B update channel + Phase 2 (arm64+R8) | K1 thin RELEASE APK |
| Update mechanism | §A update client + §B channel | K2/K3/K4/K5 |
| Server / control panel | §A functions + §D | Deploy #2; OPEN 13 server |
| Onboarding | §A SetupWizard | Phone visual Phase 10 |
| Settings / design | §A appearance/orb/charm/glow + §B 3D | Design pass DONE-CODE (Batch 1+2) |
| Security suite | §A guardian/face/PIN/SOS | G18 follow-up §B; watchman §C M3 |
| Memory / personalisation | §A encrypted memory + incognito + languages §B | SD-8 batches |
| Coding / website / documents / market | §A builder/docs/study + §C M6/M7/M8 | SD-P1/P4 |
| Transparency / audit | §A Auto Log + Catcher | Catcher Phase C DONE (#303) |

## H. IS CYCLE ME IMPLEMENT HUA

- **M1 — QR / barcode live scan** — `app/src/main/java/com/flora/app/ui/ScanScreen.kt`
  (QR mode toggle; CameraX ImageAnalysis + ML Kit Barcode Scanning,
  STRATEGY_KEEP_ONLY_LATEST; value milte hi freeze + "Dobara scan
  karo" tak rukna; Copy button; URL par "Browser me kholo" sirf tab
  jab caller `onOpenUrl` de — FloraApp call site default par hai,
  isliye is push me FloraApp chhua nahi gaya; browser-open wiring
  follow-up hai) + `app/build.gradle.kts`
  (`com.google.mlkit:barcode-scanning:17.3.0`, on-device, koi naya
  permission nahi). Photo mode + OCR bilkul untouched (diff-verify).
  Commit `ccab0b32` (tip 366be9f4 se).
- **Brain flip — brain in-app PRIMARY, proxy FALLBACK ONLY** —
  `app/src/main/java/com/flora/app/planner/PlannerBrain.kt`
  (direction 01:05 IST): `generate()` ab direct lane (BrainClient.ask,
  user keys) pehle chalata hai; valid plan mile to proxy call hoti
  hi nahi. Direct se valid plan na mile (no keys → BasicTalk, lanes
  fail → warm text, ya invalid JSON) to hi proxy fallback; proxy
  FreeLimit par Blocked (server cap genuine block). Direct lane
  khud LicenceGate.check se cap-gated hai (generate hook + ask ke
  andar), isliye free cap kisi lane se bypass nahi hota. Chat path
  pehle se hi BrainClient.ask direct tha — flip sirf planner brain
  par lagu hua (proxy-first ka ekmatra call site). Source
  reporting PROXY/DIRECT_LANE imaandaar barkaraar hai. TTS/STT
  lane order is cycle me NAHI badla (01:05 order brain-only scope
  ka hai; TTS neural Gate 6 + Deepgram key par gated hai).
  Commit `53bb694a` (tip 94643f04 se).
- **Pacing fix — verified-step-by-verified-step** —
  `app/src/main/java/com/flora/app/automation/VoicePlan.kt`
  (user report "step by step wait nahi karti" ki jad): pehle
  APP_OPEN ke baad sirf blind `delay(1000)` tha, phir agla step
  adhoori screen par chalta tha. Ab: APP_OPEN par foreground poll
  (3s tak, `waitForForeground`) — khula par foreground me na aaye
  to agla step nahi, imaandaar fail. IN_APP_SEARCH Done (= submit
  click, results dikhna baaki) ke baad results-settle poll (UI
  snapshot do baar ek jaisa = settle, max ~3s, soft — timeout par
  TAP step khud verify karega). TAP pehle se foreground + click +
  dobara foreground check karta tha (wahi raha). Har verified step
  Auto Log me darj hota hai. Builder-plan levels (PlannerEngine
  runLoop) parallel-by-design hain (dependsOn levels, M-spec) —
  unhe chheda nahi gaya; shikayat wala raasta voice-plan tha.
  Commit `53bb694a` (brain flip ke saath).
- **Phone-verified NAHI** — upar ke teeno sirf code + CI gate hain;
  live scan / brain lane / pacing Phase 10 me phone par prove
  honge. CI status docs-push waqt §J me darj hai.
- **Is cycle me chhua NAHI (jaanboojh kar):** `supabase/` (deploy
  session #2 chal rahi hai — paste-check pack bigadta; beech me
  aaya tip-move 94643f04 sirf SQL file tha, overlap zero),
  `ui/FloraApp.kt` (collision se bachav), workflow file, koi admin
  action, koi secret.

## H2. PRODUCTION BATCH (2026-10-31 ~01:13 IST order — M2–M12 + provider lanes + automation)

- **M2 — Reminders UI** — `data/ReminderStore.kt`,
  `automation/ReminderScheduler.kt`,
  `automation/ReminderAlarmReceiver.kt`, `ui/RemindersScreen.kt`,
  BootReceiver reschedule, Manifest receiver entry, FloraApp
  Screen.REMINDERS + menu. Design: reminders RoutineStore/
  JobStore se ALAG store me (JobStore upsert routineName se
  hota hai — wahi store use karte to reminder kisi routine ka
  alarm overwrite kar sakta tha). Fire par sirf notification.
  Commit `e93bd58a` (tip 731c2c6a se). CI gate pending.
- **M3+M4 — Face-attempts + My pictures** — PinVault me public
  `failedCount()` + FaceScreen guard-status section;
  `ui/PicturesScreen.kt` (MediaStore Pictures/Flora read-only,
  tap par system viewer, thumbnails ContentResolver se, koi
  Coil dep nahi); Manifest READ_MEDIA_IMAGES +
  READ_EXTERNAL_STORAGE (maxSdkVersion 32); FloraApp
  Screen.PICTURES + menu. Commit `b34f47d8` (tip e93bd58a se).
  CI gate pending.
- **M11+M12 — Driving SMS + Notices/Vibes** — DrivingScreen me
  SMS section (ACTION_SENDTO draft — Send user khud apne SMS
  app me dabayega, silent send kabhi nahi, SEND_SMS permission
  nahi); AvatarScreen me Vibes section (mood naam → wahi
  avatar rang; naya art nahi); FloraApp Screen.NOTICES (=
  NotificationsScreen alias route) + menu. Commit `1ce9a315`
  (tip b34f47d8 se). CI gate pending.
- **M5 — Event triggers (Flora-new time-only v1)** — spec
  `docs/FLORA_EVENT_TRIGGERS_SPEC.md` + `data/TriggerStore.kt`
  + `automation/TriggerScheduler.kt` +
  `automation/TriggerAlarmReceiver.kt` + `ui/TriggersScreen.kt`
  + FloraApp Screen.TRIGGERS + menu + BootReceiver reschedule
  + Manifest entry. Maya semantics UNVERIFIED the — sirf time
  trigger; action `routine` (due-flag, app-open Auto-Mode rule)
  ya `message` (notification); call/SMS/payment/delete trigger
  kabhi nahi. Commit `d0155ad9` (tip 1ce9a315 se). CI gate
  pending.
- **M6 — Marketing 8.9: SPEC ONLY, build NAHI** —
  `docs/FLORA_MARKETING_8_9_SPEC.md`: marketing vyavhaar
  records me UNVERIFIED (sirf menu naam + receiver naam) —
  bina definition ke marketing tool silent-action risk hota.
- **M7 — Markets read-only watchlist** — spec
  `docs/FLORA_MARKETS_SPEC.md` + `data/WatchlistStore.kt` +
  `ui/MarketsScreen.kt` (maujooda Yahoo Finance lane, bina
  key) + FloraApp Screen.MARKETS + menu. "~32 tools" daava
  kabhi fact nahi. Koi trading kabhi nahi.
- **M8 — Docs CSV/PDF export** — scope decided: CSV
  (Excel-compatible) + PDF via Android PdfDocument (built-in,
  koi POI dep nahi), Downloads/Flora me MediaStore se save,
  user-tap buttons DocsScreen me; DocsStore header comment bhi
  update (export ab maujood hai, auto-share nahi). M7+M8 commit
  `26c8c637` (tip d0155ad9 se). CI gate pending.
- **M10 — Typing/media modes (Flora-new definitions)** — spec
  `docs/FLORA_TYPING_MEDIA_MODES_SPEC.md` + FloraPreferences
  TYPING_MODE/MEDIA_MODE + VoiceEngine.mediaMode (system TTS
  stream: ON = media default, OFF = voice-call stream; neural
  path par nahi) + Settings toggles + chat Mic gate + main
  composable state sync (Settings toggle → chat gate turant).
  Commit `d716f0d2` (tip 26c8c637 se). CI gate pending.
- **M9 — Email read/reply: GATED, implement NAHI** — Gmail
  OAuth decision K10 par gated; is batch me sirf gated report.
- **Provider lanes (§I missing lanes)** —
  `assistant/ProviderLanes.kt` (8 OpenAI-compatible lanes +
  alag Anthropic client; default models starting points hain,
  galat naam par lane agle par girta hai, fake jawab nahi) +
  FloraPreferences 9 secret keys (KeyStore-encrypted) +
  `ui/ProviderKeysScreen.kt` (password-masked fields, save +
  saved/typed check — result me sirf status, value kabhi nahi)
  + BrainClient.ask chain (Gemini→Groq→provider lanes→
  BasicTalk; all-empty check me provider keys bhi) +
  checkKeys/checkKeysFor (9 naye optional params — purani call
  sites bina badle compile hoti hain; probe sirf /models GET,
  koi token kharch nahi) + FloraApp Screen.PROVIDER_KEYS +
  Settings se button. Notion + ElevenLabs gated hi rahe (user
  token/decision) — force nahi kiye. Commit `1f156e00` (tip
  d716f0d2 se). CI gate pending.
- **Automation completeness verify** —
  `docs/FLORA_AUTOMATION_CHAIN_VERIFY.md`: planner verified-step
  execution, routines, macros (teach-by-doing + Auto Log),
  Auto Mode ON, reboot-resume last VERIFIED step se — paanchon
  code paths par PASS mile, koi code fix zaroori nahi mila.
  Phone behaviour Phase 10-gated rahega.
- **Phone-verified NAHI** — is batch ka sab kuch sirf code +
  CI gate hai; M2–M12/provider-lane/automation phone proof
  Phase 10 me hoga. Koi bhi item DONE-CODE nahi jab tak uska
  CI run green na ho.

## I. MAYA BRAIN INVENTORY MAPPING (audit se, verify karke)

| Maya brain cheez | Flora status |
|---|---|
| LLM app→direct: Gemini, Groq | HAVE (`assistant/BrainClient.kt`: Gemini 6-model chain + Groq 2-model chain, user keys) |
| LLM app→direct: Anthropic, OpenAI, Mistral, Together, xAI, DeepSeek, DeepInfra, NVIDIA, OpenRouter | **PRODUCTION BATCH ME HAVE** — `assistant/ProviderLanes.kt` (8 OpenAI-compatible `/chat/completions` lanes + alag Anthropic client) + FloraPreferences 9 KeyStore-encrypted keys + `ui/ProviderKeysScreen.kt` + BrainClient ask-chain (Gemini→Groq→provider lanes→BasicTalk) + checkKeys probe (sirf /models GET). Commit `1f156e00`. CI gate pending; phone proof Phase 10 me. |
| Search/tools: Tavily, Brave, SerpAPI, DuckDuckGo, Yahoo Finance, Open-Meteo, Google Maps | HAVE (`tools/SearchTools.kt`: Tavily key lane, Brave lane, SerpAPI lane, DuckDuckGo free fallback, Yahoo Finance bina key, Open-Meteo, Google Maps geocode user key se) |
| Integrations: Notion, Todoist, Linear, Telegram | Todoist/Linear/Telegram HAVE (`tools/SdConnectors.kt`); Notion MISSING (SdConnectors me nahi — user token se, gated; is batch me force nahi kiya) |
| Integrations: Google TTS, Drive, Deepgram, ElevenLabs | Google/system TTS HAVE; Drive HAVE (backup); Deepgram HAVE (client + tts/stt-proxy); ElevenLabs MISSING (koi code nahi — provider decision chahiye, user key ke bina dead code hota; is batch me force nahi kiya) |
| Voice: Hunter on-device STT + Deepgram + system | Deepgram + system HAVE; Hunter JNI absent — Flora equivalent: Kokoro on-device (code-landed, Gate 6 held) + stt-proxy (deploy #2) |
| Voice: Pocket on-device TTS + Hunter cloud TTS + system | System HAVE; Pocket ki jagah Kokoro HAVE (code); Hunter cloud ki jagah tts-proxy (Deepgram) server lane — divergent-by-design, copy nahi |
| Memory: Room + DataStore, backup kabhi phone se bahar nahi | Room HAVE (`planner/PlanRoom.kt`) + DataStore HAVE (`data/FloraPreferences.kt`); platform backup HAVE (`allowBackup="false"`); user-initiated Drive backup opt-in hai (alag cheez, documented) |
| Automation: Accessibility + MediaProjection + JSON macros + Termux | Accessibility HAVE; MediaProjection §F divergent (item 37 Accessibility-only); JSON macros + Termux §E EXCLUDED |

## J. CI STATUS (docs-push waqt — watcher final green confirm karega)

- M1 QR push `ccab0b32` → run #316: in_progress (docs-push waqt)
- Brain+pacing push `53bb694a` → run #319: in_progress (docs-push waqt)
- Docs push (ye doc + K11 note + register entry) → run #DOCS: push ke baad number milega; watcher confirm karega
- Production batch pushes (gate pending — green se pehle DONE-CODE nahi):
  M2 `e93bd58a`, M3+M4 `b34f47d8`, M11+M12 `1ce9a315`,
  M5 `d0155ad9`, M7+M8 `26c8c637`, M10 `d716f0d2`,
  provider lanes `1f156e00`, docs (ye push) → run numbers watcher se
- Re-audit gate (tip re-fetch se edits verify) CI green ke baad hi
  PASS hoga — usse pehle ye items "pushed, gate pending" hain,
  DONE-CODE nahi.
