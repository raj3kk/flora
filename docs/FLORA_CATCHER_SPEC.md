# FLORA CATCHER SPEC — in-app diagnostics reporter (FULL ADVANCED)

User order 2026-10-30 ~14:21 IST (CEO order, register Section G2): jab koi
problem ho to user use copy karke chat me de sake — har type ki problem
catch kare, full copy usse aaye, full advanced. Ye spec us order ka
implementation plan hai. Pehle-Jaanch cycle: spec → pre-audit → implement
→ CI green → re-audit gate → DONE-CODE.

## §0. Goal (ek line me)

Settings me "Problem report karo (Catcher)" row — kholte hi ek hi full
copy-able report bane (crash + error events + Auto Log tail + system
state + licence state + user note), jise user Copy/Share karke chat me de
sake. Koi auto-upload kabhi nahi — report sirf user ke tap par clipboard
/ chooser / file me jayegi.

## §1. Nayi files (Phase B — FloraApp.kt chhue bina)

| File | Kaam |
|---|---|
| `data/CatcherEvents.kt` | File-backed error-event store (pure File IO, koi Android import nahi — isliye unit test me seedha chalta hai). File: `filesDir/catcher_events.log`, append-only lines `ISO-time \| CATEGORY \| detail`. `init(filesDir)` (FloraApplication se), `record(category, detail)` (safe: init na ho ya error ho to no-op, kabhi throw nahi), `recordTo/readTail/clear(filesDir, ...)` (testable core). Detail: single-line (newline → space), max 500 chars, sanitize ke saath likha jata hai. Rotation: file > 100KB ho to aadhi purani lines hata do. |
| `data/CatcherSanitize.kt` | Pure sanitizer (unit-testable). Rules §4. Events likhte waqt + report banate waqt (poori report par aakhri pass) — dono jagah lagta hai. Idempotent hai (masked output dobara mask hone par nahi badalta). |
| `data/CatcherReport.kt` | Pure report builder: `buildReport(header, crashText, events, autoLogLines, systemLines, licenceLines, note)` → ek hi String (§3 format). Koi Android import nahi. |
| `ui/CatcherScreen.kt` | Standalone composable: params `autoLogStore, licence, autoMode, fcmStatus` (sab caller-supplied, koi naya server call nahi). LaunchedEffect se IO par padhta hai: crash (CrashLogReader), events tail (CatcherEvents, aakhri 50), Auto Log tail (store.items, aakhri 30), system state (Context se §3). UI: note ke liye OutlinedTextField ("Ye kar raha tha tab hua…"), report preview (scrollable Text), 3 buttons — "Report copy karo" (ClipboardManager, LicenceScreen pattern), "Report share karo" (ACTION_SEND chooser, AutoLogScreen pattern), "Report save karo" (`filesDir/catcher-report.txt`, dobara milne ke liye) + save confirmation line + "koi auto-upload nahi" note. |
| Tests (`app/src/test/.../data/`) | `CatcherEventsTest.kt` (record/readTail/rotation/clear/missing-dir), `CatcherSanitizeTest.kt` (har mask rule + idempotency), `CatcherReportTest.kt` (sections maujood, khaali sections ka imaandaar text, note shamil). Style: CrashLogReaderTest jaisa (temp dir, JUnit, koi Android call nahi). |

## §2. Hooks (Phase B — har hook sirf 1 line, kabhi throw nahi)

`CatcherEvents.record()` andar se try/catch hai + init-na-ho to no-op —
isliye hook wali jagah ka behaviour kabhi nahi badalta, chahe record
fail bhi ho jaye. Crash handler (FloraApplication) ko chhua NAHI jayega
— crash.log writer pehle se hai, Catcher use sirf padhta hai.

| Jagah | Hook |
|---|---|
| `FloraApplication.onCreate` (tools-startup ke baad, crash-handler se pehle) | `CatcherEvents.init(filesDir)` — 1 line. Ye file Round 3 ke scope me nahi hai. |
| `BrainClient.ask` — saare provider lanes fail hone par (BasicTalk fallback return se pehle) | `record("API", "brain lanes fail codes=[...]")` — sirf HTTP codes, na model naam (chat rule se alag, ye diagnostics file hai — par model naam bhi zaroori nahi, isliye codes hi kaafi), na key, na body. |
| `VoicePlan` executor — step FAIL site (`host.log("VoicePlan FAIL...")` ke saath) | `record("PLANNER", "goal -> step title")` — isse TAP fail / app-open fail / search fail sab cover hote hain (ye sab plan steps hain), isliye FloraAutomationService me alag hook nahi chahiye. |
| `ContinuousListener` — RecognitionListener.onError + start() catch + recognition-unavailable pre-check | `record("STT", "system STT error code=N")` / `"STT start fail"` / `"STT unavailable"`. |
| `VoiceEngine` — UtteranceProgressListener.onError (sirf "flora-reply" utterance) | `record("TTS", "utterance error")`. Stop/interrupt onError nahi deta (wo onStop hai), isliye noise nahi aayega. |
| `FloraMessagingService.onNewToken` — catch block + `synced == false` | `record("FCM", "token save/sync fail")` / `"token sync false"`. Token VALUE kabhi record nahi hogi. |

JAANBOOJH KAR NAHI CHHUA (Phase B me): `LicenceClient`, `LicenceGate`
(Round 3 B2 domain), `FloraPreferences` (Round 3 B5 ne chhua), `AutoLogStore`,
`CrashLogReader`, `FloraApp.kt` (Round 3 B1/B6 + Design Batch 2 domain).
Licence/cap state report me caller-supplied `licence` param se aayegi —
koi naya server round-trip nahi.

## §3. Report format (exact sections, Hinglish headers)

```
FLORA PROBLEM REPORT
---------------------
App: Flora <versionName> (<versionCode>)
Android: <release> (API <sdk>), Device: <manufacturer> <model>
Time: <yyyy-MM-dd HH:mm:ss>
Licence: <plan> / <status> / valid=<bool> [+ honestMessage agar ho]
Auto Mode: ON/OFF
FCM: <fcmStatus ya "—">
User note: <note ya "—">

[CRASH — aakhri record]
<crash text (max 4000 chars, CrashLogReader default) ya "Koi crash record nahi hai.">

[ERROR EVENTS — aakhri 50]
<ts | CATEGORY | detail lines ya "Koi error event nahi hai.">

[AUTO LOG — aakhri 30]
<ts — text lines ya "Koi auto kaam nahi hua hai.">

[SYSTEM STATE]
Mic permission: haan/na
Notification permission: haan/na
Accessibility (Flora service): ON/OFF (PermissionsScreen wala pattern)
Battery optimization ignored: haan/na (PowerManager pattern)
Network: connected/na (ConnectivityManager; ACCESS_NETWORK_STATE manifest me hai)
```

System state sirf Context se padha jata hai (koi server call nahi).
Auto-start (Vivo) reliably padha nahi ja sakta — isliye report me uska
jhootha "ON/OFF" kabhi nahi aayega; uski jagah line hogi:
`Auto-start: phone settings se check karein (Vivo: auto-start + battery unrestricted)`.
Version: PackageManager se (`getPackageInfo`, try/catch, fail ho to
"unknown") — BuildConfig par nirbhar nahi (app me BuildConfig use
nahi hota).

## §4. Sanitizer rules (CatcherSanitize — KABHI NAHI list ka implementation)

Report me ye kabhi poora nahi aayega (mask lagne ke baad hi likha/share/
copy hoga):
- 12-digit standalone number (UTR shape) → `********` + aakhri 4 digits.
- `FLORA-XXXX-XXXX-XXXX` shape → `FLORA-****-****-****`.
- Lamba token (`[A-Za-z0-9_-]{32,}`) → `[token-masked]`.
- Email address → `[email-masked]`.
- URL query string (`?` ke baad sab) → hata do, base URL hi rakhe.
- `key|token|password|secret|otp = value` shape → `key=[masked]`.
- Chat ka poora text report me aata hi nahi (sirf crash text, events,
  Auto Log lines — Auto Log me chat text kabhi nahi hota, sirf action lines).
- FCM token, licence key poori, API key values: hooks inhe record hi
  nahi karte (sirf success/fail + codes), aur sanitizer aakhri pass me
  bhi pakad lega.

## §5. UI entry (Phase C — wiring, Round 3 ke B1/B6 DONE-CODE ke baad)

- `Screen` enum me `CATCHER` + `screenTitle` me `"Problem report"`.
- SettingsScreen me naya param `onShowCatcher: () -> Unit` (onShowAutoLog
  ke baad), aur Auto Log row ke paas hi row: Button
  `"Problem report karo (Catcher)"` — Settings me aasani se milegi,
  chhupi hui nahi (design rule: koi option hidden nahi).
- Render branch AutoLogScreen ke baad:
  `else if (screen == Screen.CATCHER) { CatcherScreen(autoLogStore, licence, autoMode, fcmStatus) }`
  (fcmStatus SettingsScreen ke maujooda state se wiring waqt pass hoga;
  Phase B screen me ye param default `""` hai — khaali ho to section
  me "—" dikhega).
- Menu (▾) me Auto Log row ke paas `"Problem report"` row (optional,
  wiring waqt line numbers se verify karke) + voice map me
  `"catcher" / "problem report" -> Screen.CATCHER`.
- Wiring push se pehle fresh-fetch + diff-verify (sirf wiring lines);
  Round 3 ke B1/B6 hunks se takkar ho to wiring rukegi, dobara
  fresh-fetch ke baad hi chalegi — overwrite kabhi nahi.

## §6. Gates + NEVER

- Gate: spec PASS → pre-audit PASS → Phase B push (CI green) →
  re-audit (tip-content + scope anchors) → Phase C wiring push (CI green)
  → re-audit → DONE-CODE. Har gate ke bina "landed" ka daava nahi.
- NEVER: `[skip ci]` nahi; `.github/workflows/` API se nahi (web UI
  only); koi spend nahi; koi nayi API key nahi; koi silent action nahi
  (Copy/Share/Save sirf user tap par; event recording on-device log hai
  — Auto Log + crash.log ka precedent, koi upload nahi); 4 stops upar;
  creator code/UI/assets copy nahi; koi secret file me nahi; chat me
  technical talk bolne ka v2.20 rule barkaraar (Catcher report
  diagnostics file hai, chat jawab nahi — Flora chat me is report ka
  technical text kabhi nahi bolegi).
- Daava sirf code + CI + tip-content ka hoga — phone-verified NAHI
  (Phase 10 me: Catcher kholo → note likho → Copy karo → chat me
  paste karo → report poori aayi ya nahi; crash ke baad report me
  crash section aaya ya nahi).

## §7. Phase plan + scope notes (imaandaari se)

- Phase A (is push): spec. Phase A2: pre-audit append (PASS ke baad hi
  implement). Phase B: §1 files + §2 hooks + tests (FloraApp.kt nahi).
  Phase C: §5 wiring (Round 3 B1/B6 DONE-CODE ke baad).
- Phase B me jo abhi COVER NAHI hoga (jhootha daava nahi): DB/storage
  errors ka alag hook nahi hai (stores apne catch me chupchaap nigal
  jaate hain — unka central hook alag spec maangta hai; crash/events
  se jo pakda jayega wo report me aayega); FCM delivery fail (message
  na aana) on-device pakda nahi ja sakta — sirf token sync fail
  pakda jayega (§2); network state sirf snapshot hai, history nahi.
- Design Batch 2 domain (menu renames) se takkar nahi hai — Catcher
  row/menu entry nayi lines hain, rename wali lines nahi.
