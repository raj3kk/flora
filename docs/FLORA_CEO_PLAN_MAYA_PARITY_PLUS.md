# Flora CEO Plan — Maya se achha banao, kuch miss na ho

CEO mandate (user order 2026-10-28 ~16:36 IST): "Muse Flora ka
CEO hai." Kaam ka tareeqa: poora audit (2-3 baar) → plan →
one-by-one implementation → har item par audit gate → issue
0 → agla item. Kaam rukega nahi.

## CEO boundaries (permanent, CEO title se bhi waive nahi)
- Paisa/kharch: hamesha alag se explicit approval (koi paid
  service, model hosting bill, API spend bina bole nahi).
- Creator copy: kabhi nahi — code, UI text, assets, character,
  logo, trademark, purani keys, purana backend/data. Sirf
  behaviour samajh kar dobara likhna.
- Privacy: screen content kabhi server par nahi; personal
  data (keys, face/voice templates, memories, chat) phone par
  hi; payment kabhi auto nahi; delete/irreversible par hi
  confirmation poochha jayega.
- Law list (Table A universal) har build par lagu rahega.
- Plan ke bahar ka naya feature / naya paid raasta / data
  model badalna — uske liye owner ka alag word chahiye.
  Is plan ke andar ka sab kuch implement karne ka order
  is CEO appointment se mila hua hai.

## Audit rounds (2-3 baar, jaisa order hai)
- R1 — Mapping audit (YE DOC): Maya ki har capability ka
  Flora status + action + phase. Evidence: GAP_AUDIT,
  GAP5_ENDPOINTS, BACKEND_LICENCE_AUDIT, RE_FEATURES
  (124 + 22 + 32 = 178), screenshot plan, layer audit.
- R2 — Har item par: code audit (order/balance/permission/
  privacy/copy-check) → CI PASS → APK verify → test entry.
- R3 — Final full audit: phone tests + server live checks +
  security + privacy + law list + Maya-parity re-check.
  R3 me issue 0 hone par hi "Maya se achha" bola jayega.

## A. Server parity — Maya server par kya tha → Flora server par kya banega

| # | Maya server item (evidence) | Flora server (naya, copy nahi) | Status / Phase |
|---|---|---|---|
| S1 | Model hosting: guardian/STT/turn/addressee manifests + files, 10-min cache, SHA check, .ok marker, assets fallback | Supabase Storage bucket flora-models (public read) + manifest.json + ModelDownloader client (v2.21) + SHA verify + .part/.ok + 10-min cache + bundled fallback | M1: client + screen v2.21; bucket host dashboard se (owner-side browser task, service key ke bina API se nahi banega) |
| S2 | Avatar catalog + posters (getmaya.online/avatar/) | Flora avatar catalog: Flora ke APNE avatars (creator character kabhi nahi) — catalog.json + posters, Flora website/Storage par | S2: catalog design + pehle 3 Flora avatars (original art, app me nahi, server se) |
| S3 | Hunter TTS server (api.getmaya.cloud/tts/v1) | Flora TTS proxy (Edge Function, provider keys server par, user key se nahi) — tabhi jab owner provider key de; tab tak system TTS + provider TTS app se | S3: proxy code ready, deploy + keys owner ke baad |
| S4 | Home sync: join / genuine / WebSocket (sync.hunterislive.online) | Supabase tables home_devices + home_share + Realtime (WebSocket ki jagah) + genuine check = licence-verify jaisa | P5 (Home phase) |
| S5 | Checkout + licence dashboard (website) | DONE: website signup/login/pricing/pay (UTR) + admin approve + admin list + licences table | LIVE (v2.8–v2.11 se) |
| S6 | Licence genuine check | DONE: licence-verify (1-phone/15-din server-enforced) | LIVE |
| S7 | Agentic backend (toolbaaz.online — sirf browser view tha, app API nahi) | Flora brain-proxy + automation engine (app se) — usse behtar, kyunki Flora ka automation phone par + server brain dono hain | Brain-proxy step 2 pending (keys + JWT + quota + wiring) |
| S8 | OAuth redirect (com.getmaya.android.oauth:/callback) | Supabase Auth (email/password + link) — DONE v2.8; OAuth provider baad me (owner faisla) | LIVE (basic), OAuth pending |

## B. App parity — Maya app me kya tha → Flora me kya

| Area | Maya (evidence) | Flora aaj | Action / Phase |
|---|---|---|---|
| Wake word | Neural (10,000+ clips, PyTorch→ONNX→TFLite) | MFCC template v0 + 6 clips | Neural training (recordings milne ke baad) + tune — P1 |
| STT | On-device neural (Hunter STT ONNX) + turn + addressee | System STT + ContinuousListener | Deepgram streaming barge-in (key owner dega) — P1; on-device STT baad me |
| TTS | Hunter cloud TTS + Google TTS, character voices | System TTS, warm prompt + pitch/rate | S3 proxy + voice-per-persona — P1 |
| Brain | Gemini + Anthropic + aur lanes, streaming | Gemini→Groq + auto-discovery + BasicTalk fallback | Brain-proxy wiring + quota — step 2; aur lanes baad me |
| Vision | Screen/frame vision | LiveScreen (persistent session v2.20) + askAboutImage | Continuous vision — P2 |
| Personas | Kai personas + romantic (tone) | Normal + romantic (v2.17) | Voice-per-persona — P1 |
| Memory | Memories (schema unknown) | Manual MemoriesStore + JournalStore | Auto-learn memory — P1; server sync NAHI (privacy) |
| Routines/macros | Builtin macros (5) + routines | Routines (manual, auto call/SMS v2.18) | Teach-by-doing + job runner — P1/P4 |
| App control | Open + gestures (swipe) | Open/call/SMS/install/ChatGPT automation | Tap/type/swipe general — P2 |
| Screen automation | Screen dekhna + kaam | LiveScreen + App kholo aur dekho (v2.20) | Screen automation expand — P2 |
| Notifications | Listener (evidence se) | Announce receivers (power/battery/call) | Notification listener (read-only) — P2 |
| Research | Multi-source search | Tavily/DDG single | Multi-page research loop — P3 |
| Markets | Price + watchlist (evidence) | Yahoo price only | Watchlist — P3 |
| Docs/email | Docs + email tools | DocsStore (local docs) only | Docs+email — P3 |
| Image | Banao + post | Banao (Pollinations + ChatGPT) | Post (confirm ke saath) — P3 |
| Code | Website banata tha (template script) | websiteStarter stub (save nahi karta) | Code-writer v1 (save + preview) — P3 |
| Whiteboard | Whiteboard feature | NAHI | P3 |
| Call voice-answer | Announce + pick (evidence) | Announce only; dialer greeting (remote hearing unproven) | Voice answer — P4 (dialer user set karega) |
| WhatsApp | Bot/API calls (evidence) | NAHI | P4 (confirm; Auto Mode rule lagu) |
| PC link | PC hub (route tha) | NAHI | P4 (tables pc_links, sync_jobs) |
| Home | Relay box + app + share | NAHI | P5 (tables + safety: electrician, MCB OFF, 5A/channel) |
| Game | Game commentary | NAHI | P5 |
| Guardian | Model validation + on-device | Validation PASS, Kotlin port pending | P1 (port + UI) |
| Face | Face presence/match | FaceMatcher (0.55) + FaceScreen | Phone test + tune — R3 |
| SOS | SOS flow | SOS screen + Auto SOS | Phone test — R3 |
| Scan | Scanner | ML Kit Latin + Devanagari | Phone test — R3 |
| Driving | Driving mode (route) | DrivingScreen | Phone test — R3 |
| Licence/pay | Website checkout | UPI + UTR + admin approve | Phone test (asli Pro ke baad block/transfer proof) — R3 |

## C. Automation parity+ — Maya se zyada

Maya automations (evidence): voice commands (45 destinations),
macros, routines, app open, call/SMS, search, web actions
(Telegram/Todoist/Linear/Vercel/Drive), swipe gestures,
screen dekhna. Flora me isse ZYADA hoga:
1. Auto Mode default ON (v2.18) + Auto Log + ongoing
   notification + "auto mode band" — Maya me ye khula
   system nahi tha.
2. One-confirmation-then-full-execution har action par
   (call/SMS/open/install/image/routine) — v2.17–v2.20.
3. Job runner (background jobs, P1): time par kaam khud.
4. Teach-by-doing (P4): ek baar dikhao, phir khud kare.
5. Screen automation (P2): dekhna + kholna + tap/type/
   swipe + form bharna — sab Auto Mode rule ke andar.
6. Model auto-download + auto-update (M1, v2.21 se).
7. Cross-device: PC link (P4) + Home (P5) — Maya ke
   barabar + Flora server par.

## D. Model download system (Flora ka apna, v2.21 se)

- Manifest: JSON (id, name, version, size, sha256, url,
  bundled?) — source of truth repo me web/models/
  manifest.json; host Supabase Storage public bucket.
- Client: ModelDownloader — manifest 10-min cache; download
  .part me; SHA-256 verify; pass par rename + .ok marker;
  fail par .part delete + warm message (koi technical nahi).
- Bundled models (phone me pehle se): face model
  (MobileFaceNet asset) + wakeword template (v0) — ye
  download nahi maangte, status "Phone me maujood".
- Neural models (STT/turn/addressee/guardian/wakeword):
  jab training/licence se milenge tabhi manifest me
  aayenge — Maya ke models kabhi nahi (copy mana hai).
- Models screen (Menu > Models): har model ka status +
  Download button + Refresh. Download ke baad Flora
  bol kar batayegi.
- Safety: sirf Flora ke apne manifest URL se download;
  SHA mismatch = kabhi use nahi; size cap; user rok
  sake (cancel); mobile data par badi file se pehle
  poochha jayega (ek confirmation — money/data rule).

## E. Phase order (koi phase skip nahi, har phase ke baad audit)

1. M1 (v2.21): ModelDownloader + Models screen + manifest.
2. S2: Flora avatar catalog (original art).
3. P1: Guardian port → wake tune → barge-in (Deepgram
   key owner dega) → job runner → auto-memory →
   voice-per-persona.
4. P2: tap/type/swipe + device settings → continuous
   vision → notification listener (read-only).
5. P3: research loop → watchlist → whiteboard →
   code-writer v1 → docs+email → post (confirm).
6. P4: voice answer → WhatsApp (confirm) → teach-by-
   doing → PC link.
7. P5: Home (safety-gated) → game → U1 → U2 → U3.
8. S3: TTS proxy (keys owner dega) + brain-proxy step 2.
9. R3: final full audit (phone + server + security +
   privacy + law) → issue 0 → "Maya se achha" ka daava.

## F. Maya se zyada — Flora ki jeet wali list (pehle se hai)

Auto Mode default ON, Auto Log, no-technical-errors rule,
BasicTalk, model auto-discovery, server-enforced licence
(1-phone/15-din), UPI+UTR+admin audit, 30-din retention,
screen-kabhi-server-par-nahi privacy, ₹199/month price,
har item par audit gate + CI verify + test entry.

## G. Owner se kya chahiye (standing list, naya kuch nahi)

Phone tests; pehla website signup→admin; app+dashboard
keys; apna UPI VPA; battery/auto-start/Accessibility/
dialer; Hey Flora recordings; Deepgram key faisla; rename
faisla; domain (jab khareedoge); Meta App Review; webhook
verify; Storage bucket dashboard step (M1 host ke liye —
browser task se kiya jayega, tumhe kuch karna nahi hai
jab tak dashboard login na maange).
