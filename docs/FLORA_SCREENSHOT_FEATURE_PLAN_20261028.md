# Flora — Screenshot Feature Plan (production-ready, full pipeline)

Date: 2026-10-28. Source: user ke bheje hue 2 screenshots (original app ke
features page ke) + original APK ka behaviour + Flora ke code ki live haalat.
Ye plan ANALYSIS hai — isme original site ka marketing text, UI, character,
logo ya trademark copy nahi kiya gaya hai. Har feature ko Flora ke apne
design se, apne shabdon me likha gaya hai. Original ke daam (₹699/₹999
jaise) Flora par lagu nahi hote — Flora ka daam ₹199/month FINAL hai.
Romantic persona (jo original ke 3 personalities me ek tha) Flora me
KABHI nahi banega — ye standing exclusion hai, sirf user ke saaf shabdon
se dobara khul sakta hai, is plan se nahi.

## Audit ka tareeka (guess par kuch nahi)
1. Dono screenshots se har feature card nikala gaya (22 features + 4
   update-only points; "Aapka order" card feature nahi hai, wo purchase
   card hai — uske characters copy nahi honge).
2. Har feature ko Flora ke 178-audit se milaya gaya.
3. Har shaq wali haalat ko code me grep karke verify kiya gaya
   (LiveScreen vision, notification listener, watchlist, website starter,
   routines teach, docs creation, game, whiteboard, PC link, torch/
   brightness, sleep phrase, email, call voice-answer, Tavily lane,
   auto-memory).
4. Har feature ke liye frontend + backend + database + workflow +
   gates + phone-test, paanchon likhe gaye — jahan ek bhi missing hai,
   wahan status NAHI/AADHA hai, HAI nahi.

## Summary counts (25 items)
- Poora functional (HAI): 0 — koi bhi screenshot feature 100% nahi hai.
- Aadha (AADHA): 16 — kaam karta hissa hai, poora pipeline nahi.
- Nahi bana (NAHI): 9 — code zero ya sirf stub text.
- Hamesha excluded: 1 hissa (romantic persona) + silent auto-post /
  silent auto-reply / silent recording (ye kabhi nahi banenge, plan me
  inki jagah one-confirmation rule hai).

## Status table
| # | Feature (mere shabdon me) | Flora me abhi | Plan phase |
|---|---|---|---|
| F1 | Real-time voice, beech me tokna (barge-in) | AADHA | P1 |
| F2 | Call ka naam bolna + awaaz se uthana/kaatna | AADHA | P4 |
| F3 | WhatsApp + Business: padhna, photo/file, call | NAHI | P4 |
| F4 | Poora phone control: tap/type/swipe + settings | AADHA | P2 |
| F5 | Wake word + sleep phrase, reboot ke baad ready | AADHA | P1 |
| F6 | Teen personas (tone) | AADHA tone; romantic EXCLUDED | P1 (tone/voice only) |
| F7 | Screen + camera vision (error/purchase/dekhna) | AADHA | P2 |
| F8 | Live screen share par sawaal-jawab | AADHA | P2 |
| F9 | Live game commentary | NAHI | P5 |
| F10 | Notifications padhna + WhatsApp jawab | NAHI | P2 |
| F11 | Khud code likhna + galti sudharna | NAHI (stub text) | P3 |
| F12 | Deep research, pages padh kar source ke saath | AADHA | P3 |
| F13 | Picture banana + post karna | AADHA (banana v2.15) | P3 (post) |
| F14 | Voice se documents + email | NAHI | P3 |
| F15 | Live market price + apni watchlist | AADHA (price yes) | P3 |
| F16 | Whiteboard par likh kar samjhana | NAHI | P3 |
| F17 | Voice Guardian (awaaz se pehchan) | AADHA (validation PASS) | P1 |
| F18 | Lambi memory (log, pasand, tareeka) | AADHA (manual) | P1 |
| F19 | Background me lamba kaam + baat jaari | AADHA (bg mic v2.15) | P1 |
| F20 | Routines sikhana (ek baar karke macro) | AADHA (manual) | P4 |
| F21 | PC-phone link (ek jagah shuru, doosri par khatam) | NAHI | P4 |
| F22 | Home dashboard (light/fan ek screen par) | NAHI | P5 |
| U1 | Ek hi cheez do baar na bhejna (dedup) | AADHA | P5 |
| U2 | Kamzor network par bhi stable (retry/queue) | AADHA | P5 |
| U3 | Jo zyada use ho, usme tez hona (seekhna) | NAHI | P5 |

---

## F1 — Real-time voice + beech me tokna (AADHA → P1)
- Hai: continuous mic (v2.14 foreground, v2.15 background service),
  turn-taking indicators, Stop par "Ruk, kuch bol rahe the?".
- Nahi: ASLI barge-in — user ke bolte hi Flora ka TTS turant rukna.
  System SpeechRecognizer se ye nahi hota (wo ek baar me ek hi kaam
  karta hai); isliye Stop button hi interrupt path hai.
- Pipeline: AudioRecord (VOICE_COMMUNICATION, AEC) → streaming STT
  (Deepgram, key chahiye — user se key ka faisla pending) → partial
  transcript par TTS stop + naya pipeline → jawab TTS. Agar Deepgram
  key nahi mili to system recognizer + Stop hi rahega (imandari se).
- Frontend: Baat view (orb pulse = sunna/bolna, koi text nahi).
- Backend: koi server audio nahi (on-device STT stream sirf Deepgram
  ko jayega, wo bhi user ki key se; recording kabhi save nahi).
- Database: kuch nahi (transcript memory me hi, save sirf manual
  Yaadein me).
- Gates: mic hamesha notification ke saath; mute se band; background
  service ka notification kabhi nahi chhupenga.

## F2 — Call: naam bolna + awaaz se uthana/kaatna (AADHA → P4)
- Hai: call announce (kaun call kar raha hai, naam bolna),
  InCallService greeting (speaker se), dialer role screen.
- Nahi: "uthao"/"kaato" bol kar call uthana/kaatna; remote party ko
  Flora ki awaaz saaf sunna (Android public API se uplink me TTS
  inject nahi hota — ye guarantee nahi di ja sakti).
- Pipeline: PHONE_STATE → announce (TTS) → user voice command
  ("uthao") → TelecomManager accept/reject (dialer role hone par) →
  speaker greeting → call khatam par summary (manual Yaadein).
- Frontend: Connect screen (dialer role) + call-talk toggle (OFF default).
- Backend: kuch nahi (calls server par nahi jati).
- Database: call_log_local (sirf on-device, 30 din retention).
- Gates: dialer role user khud dega; call record SILENT kabhi nahi;
  call-talk ka "done" sirf phone test ke baad.

## F3 — WhatsApp + Business (NAHI → P4)
- Hai: kuch nahi (koi WhatsApp code nahi hai).
- Pipeline: NotificationListener (WhatsApp notifications padhna) →
  chat kholo (accessibility se, user ke saamne) → jawab user se
  confirm karwa kar bhejna (one-confirmation, kabhi silent nahi) →
  photo/file: user ke chune hue file ko vision se padhna, phir jawab.
  Voice/video call: accessibility se answer button tak pahunchna;
  baat speaker+mic loop se hogi (echo possible — saaf bataya jayega).
  Official API nahi hai, isliye ye raasta hi ek raasta hai.
- Frontend: WhatsApp screen (chats list nahi — sirf notification se
  aaye message + confirm-send UI).
- Backend: kuch nahi (WhatsApp data server par kabhi nahi).
- Database: wa_templates_local (user ke saved jawab, on-device).
- Gates: silent auto-reply KABHI NAHI; har bheja gaya message pehle
  user ko dikhega + confirm hoga; Business + personal dono ke liye
  wahi rule.

## F4 — Poora phone control (AADHA → P2)
- Hai: app kholna (naam se), website kholna, call/SMS (confirm se),
  accessibility service ka dhancha.
- Nahi: tap/type/swipe automation, torch, brightness, volume,
  hotspot, silent mode, lock.
- Pipeline: Voice command → intent samajhna (brain ya rule) →
  confirmation (ek baar) → AccessibilityService action (tap/type/
  swipe, sirf screen par dikhne wale element par) → device setting
  action (torch: CameraManager; brightness: Settings; volume:
  AudioManager; hotspot/silent/lock: system API + permission) →
  bol kar confirm.
- Frontend: Auto screen (actions list + consent toggle OFF default) +
  Permissions screen (accessibility status).
- Backend: kuch nahi (control on-device hi).
- Database: auto_actions_log (on-device, 30 din).
- Gates: consent OFF default; har action se pehle confirmation jab
  tak user ne us action ko "hamesha" na kaha ho; payment/destructive
  kabhi auto nahi; screen content server par kabhi nahi.

## F5 — Wake word + sleep phrase (AADHA → P1)
- Hai: "Hey Flora" v0 template (MFCC mean + cosine, threshold 0.60
  starting guess), enrolment 3 samples, BootReceiver se reboot par
  wapas, foreground notification.
- Nahi: neural wake model (PyTorch→ONNX→TFLite, 10,000+ clips wala
  pipeline abhi nahi chala — VM me torch nahi, clips sirf 6 hain);
  sleep phrase ("Bye Flora" bol kar sunna band) nahi hai; false
  trigger test phone par nahi hua.
- Pipeline: AudioRecord 2s window → MFCC → template cosine →
  threshold → greeting + app aage → sleep phrase par service
  standby (notification rahega, sunna rukega) → dobara wake par
  wapas. Neural model ke liye: negative speech clips jama karo →
  training (user ke phone/PC par ya baad me) → TFLite asset →
  phone par false-trigger test → threshold tune.
- Frontend: Face/Wake enrolment screen + Wake toggle (OFF default).
- Backend: kuch nahi (template on-device encrypted, KeyStore).
- Database: kuch nahi (template BiometricTemplates me, vector only).
- Gates: enrolment consent se; audio sample kabhi save/server nahi;
  wake se koi unlock/action/posting nahi (sirf baat shuru).

## F6 — Personas: tone + awaaz (AADHA tone; romantic EXCLUDED → P1)
- Hai: 3 tone (Flora/Saathi/Guru) — sirf tone badalta hai.
- Nahi: har persona ki alag awaaz (system TTS ek hi hai);
  ek sentence me persona switch (abhi Settings se hota hai).
- Pipeline: "Flora, guru mode" → persona pref badlo → brain system
  prompt prefix badlo → jawab us tone me. Alag awaaz ke liye:
  ElevenLabs key (user ka faisla pending) → per-persona voice id
  map → TTS lane switch (key ho to ElevenLabs, warna system TTS).
- Frontend: Baat view me persona switch command + Settings persona row.
- Backend: kuch nahi (voice id sirf key ke saath use).
- Database: persona pref (DataStore, pehle se hai).
- Gates: ROMANTIC PERSONA KABHI NAHI (standing exclusion); koi
  persona user ko dhoka nahi dega ki wo insaan hai.

## F7 — Screen + camera vision (AADHA → P2)
- Hai: Scan (photo lo → Gallery save), attachments vision
  (image → VisionClient), LiveScreen (frame capture + sawaal).
- Nahi: camera se live dekh kar error fix/purchase judge karna
  (continuous vision loop nahi); screen ke text ko accessibility
  se padh kar samajhna (partial — LiveScreen frame-based hai).
- Pipeline: User ka sawaal → frame lo (screen: MediaProjection
  consent dialog se; camera: ImageCapture) → frame sirf us sawaal
  ke liye vision ko (user ke chune hue frame ke alawa screen
  kabhi server par nahi) → jawab bol kar → frame delete (memory
  se, save nahi).
- Frontend: Live Screen + Scan (donon pehle se hain, continuous
  mode jodna hai).
- Backend: VisionClient (user ki Gemini key se, wahi key jo brain
  me hai) — koi alag server nahi.
- Database: kuch nahi (frame save nahi hota).
- Gates: MediaProjection consent har session me; screen content
  ka koi log/server nahi; camera sirf user ke kehne par.

## F8 — Live screen share par sawaal (AADHA → P2)
- Hai: frame par sawaal-jawab (LiveScreen ke preset + custom
  sawaal, VisionClient se).
- Nahi: CONTINUOUS live — screen badalte hi Flora khud bol kar
  bataye (abhi har sawaal par naya frame + naya sawaal chahiye).
- Pipeline: Live session start (consent) → har 3s frame →
  pichhle frame se badlaav ho to vision ko bhejo → badlaav ho to
  chhota bol kar jawab (warna chup) → session stop par sab khatam.
- Frontend: Live Screen (continuous toggle jodna hai).
- Backend: VisionClient (wahi key).
- Database: kuch nahi.
- Gates: session ke baahar koi capture nahi; notification me
  "live dekh raha hun" hamesha dikhega.

## F9 — Live game commentary (NAHI → P5)
- Hai: kuch nahi.
- Pipeline: Game chalu (user batayega kaunsa game) → screen frames
  (MediaProjection, consent) → har frame par game state samajhna
  (vision, thoda delay ke saath) → commentary TTS (co-caster tone,
  chhota, beech me nahi tokna) → user "bas karo" bole to turant stop.
- Frontend: Game mode toggle (Live Screen ke andar).
- Backend: VisionClient (wahi key).
- Database: game_profiles_local (user ke pasandeeda games, on-device).
- Gates: game account me koi login/data nahi liya jayega; commentary
  sirf sunne ke liye, game me koi input nahi (cheating nahi).

## F10 — Notifications padhna + jawab (NAHI → P2)
- Hai: apni FCM push (Flora ki apni notifications) + WakeWord
  notification.
- Nahi: doosri apps ki notifications padhna (NotificationListener
  code zero hai); WhatsApp jawab (F3 me hai, yahan sirf padhna).
- Pipeline: NotificationListenerService (user se permission) →
  notification aaye to bol kar padhna (sirf user ke chune hue apps
  ki) → jawab chahiye to F3 ka confirm-send flow → notification
  dismiss kabhi auto nahi.
- Frontend: Notifications screen (kaunsi apps ki padhni hai, toggle
  list) + Permissions screen me listener status.
- Backend: kuch nahi (notifications server par kabhi nahi).
- Database: notif_rules_local (app toggles, on-device).
- Gates: listener permission user khud dega; notification ka text
  kabhi log/server/backup par nahi; silent reply KABHI NAHI.

## F11 — Khud code likhna + galti sudharna (NAHI, sirf stub → P3)
- Hai: websiteStarter — sirf starter HTML text, file save nahi hoti
  (imandari se likha hua hai).
- Nahi: plan → likhna → chalana → galti sudharna loop; file save;
  project banana; deploy.
- Pipeline: Spoken brief → plan (brain, steps me) → files likhna
  (app ke apne folder me, user ko dikhate hue) → local preview
  (WebView me kholo) → galti mile to brain se sudhar (max 3 round)
  → user ko preview dikhao → user "theek hai" bole tabhi aage
  (publish/deploy kabhi auto nahi, alag approval).
- Frontend: Code screen (brief → plan → files → preview → fix loop).
- Backend: brain (wahi key); koi alag code server nahi (v1 me).
- Database: code_projects_local (project meta, on-device) + files
  app folder me.
- Gates: file sirf app folder me; system folder chhedna nahi; deploy/
  publish hamesha alag explicit approval; koi key/secret code me
  kabhi nahi likha jayega.

## F12 — Deep research, source ke saath (AADHA → P3)
- Hai: Tavily lane (key ho to) + DuckDuckGo fallback, ek sawaal ka
  ek jawab.
- Nahi: multi-page loop — sawaal → 3-5 pages padhna → tulna →
  source ke saath jawab; "kahan se mila" ka list.
- Pipeline: Sawaal → search (Tavily/DDG) → top pages kholo (text
  nikalo) → brain se tulna + summary → jawab + sources list
  (har source ka naam + link, bol kar bhi) → user "aur detail"
  bole to us page par gehra jao.
- Frontend: Chat me hi (research mode command: "research karo …").
- Backend: Tavily (key ho to) / DDG (bina key); brain (wahi key).
- Database: research_cache_local (24 ghante, on-device; stale
  listings wala sabak: page ki andaruni date bhi check hogi).
- Gates: aggregator ki stale date ko fresh mat batana (FormMitra
  digest wala rule yahan bhi); source ke bina daava nahi.

## F13 — Picture banana + post karna (AADHA → P3)
- Hai: banana (v2.15, Pollinations bina key, Gallery > Flora save).
- Nahi: Instagram/Facebook par post karna (koi post code nahi).
- Pipeline: "image banao …" → generate → Gallery save → user ko
  dikhao → user "post karo" bole to ek confirmation (kaunsi app,
  kaunsa caption) → app kholo (Instagram/Facebook, user ke
  account se) → post user ke haath me final step (auto-post
  kabhi silent nahi; app khud user ke naam se post nahi karegi
  bina dikhaye).
- Frontend: Chat command + Gallery confirm UI.
- Backend: Pollinations (bina key); post ke liye koi server nahi.
- Database: kuch nahi (image file hi hai).
- Gates: silent posting KABHI NAHI; caption user dekhega phir
  post hoga; koi creator asset/character use nahi hoga.

## F14 — Voice se documents + email (NAHI → P3)
- Hai: DocsStore/Docs screen (notes jaisa dhancha), Scan se text.
- Nahi: Word/Excel/PowerPoint/PDF banana; email bhejna/padhna.
- Pipeline: "ek letter banao …" → brain se draft → file banao
  (local: .md/.txt pehle, phir .docx/.pdf library se) → user ko
  dikhao → user "theek hai" bole tabhi save/share → email:
  user ka email account (user khud jodega, OAuth) → draft dikhao
  → confirm par bhejo (kabhi auto nahi).
- Frontend: Docs screen (create → preview → save/share) + Email
  screen (draft → confirm → send).
- Backend: brain (draft ke liye); email provider (user ka account,
  OAuth — koi password app me save nahi).
- Database: docs_meta_local (on-device) + files app folder me.
- Gates: email kabhi auto nahi; attachment user chune; draft bina
  confirm ke nahi jayega.

## F15 — Market price + watchlist (AADHA → P3)
- Hai: stockPrice (Yahoo Finance, ek symbol ka live price).
- Nahi: apni watchlist (symbols save + list) + price alert
  (target par bol kar batana).
- Pipeline: "Tata ka price" → Yahoo → jawab + calculation (khud
  ka hisaab, andaza nahi) → "watchlist me daalo" → save →
  background check (har 30 min, battery ka dhyan) → target hit
  par notification + bol kar (app kholo to sunao).
- Frontend: Markets screen (price + watchlist + alert set).
- Backend: Yahoo Finance (bina key); koi paid market API nahi.
- Database: watchlist (symbol, target, on-device; Supabase me
  nahi — paisa/finance data server par nahi).
- Gates: price "live" sirf fetch waqt ka hai, guarantee nahi;
  investment advice nahi di jayegi (sirf price + hisaab).

## F16 — Whiteboard teaching (NAHI → P3)
- Hai: kuch nahi.
- Pipeline: "whiteboard par samjhao …" → full-screen board
  (Canvas) → brain se steps → board par likhna/draw karna (ek
  step, phir bol kar samjhana, phir agla step) → user "aage"
  bole to aage, "dobara" bole to dobara → board save (image)
  user chahe to.
- Frontend: Whiteboard screen (Canvas + TTS sync + step buttons).
- Backend: brain (steps ke liye, wahi key).
- Database: whiteboard_saves_local (images, on-device).
- Gates: board par sirf samjhane ka content; koi personal data
  board par nahi aayega.

## F17 — Voice Guardian (AADHA → P1)
- Hai: validation PASS (real encoder par: same-speaker cosine
  0.558–0.770, noise max 0.380, silence VAD reject; threshold
  starting guess 0.45–0.50) + face-match v2.3 + GuardianGate PIN +
  BiometricTemplates (KeyStore, vector only).
- Nahi: Kotlin port (exact frontend + TF mel matrix asset + VAD +
  encoder streaming) + enrol/match UI + phone test.
- Pipeline: Consent enrolment (3 samples, on-device) → vector
  (KeyStore encrypted, kabhi server/backup nahi) → sensitive
  action par voice-match → match ho to Guardian check (PIN) →
  tabhi action; fail ho to "pehchana nahi", koi guess-unlock nahi.
- Frontend: Voice enrol/match screen (Face screen ke saath).
- Backend: kuch nahi (voice kabhi server par nahi).
- Database: kuch nahi (vector KeyStore me).
- Gates: default OFF; voice-match AKELA kabhi unlock nahi karega
  (voice + Guardian dono); liveness ka daava nahi.

## F18 — Lambi memory (AADHA → P1)
- Hai: MemoriesStore (manual yaadein) + Journal (manual) +
  30-din retention pruning.
- Nahi: AUTO memory — log, pasand, tareeka khud seekh kar save
  karna (abhi sirf user ke kehne par save hota hai).
- Pipeline: Baat-cheet me fact mile (naam, pasand, tareeka) →
  Flora poochhe ("ye yaad rakhun?") → user haan bole tabhi save
  (kabhi chhupke nahi) → agle baar wahi pasand/tareeka apne aap
  lagu → user "bhool jao" bole to delete (confirm par).
- Frontend: Yaadein screen (list + delete + auto-suggest queue).
- Backend: kuch nahi (memory on-device; server sync nahi — privacy).
- Database: memories (pehle se hai) + memory_suggest_queue_local.
- Gates: auto-save kabhi silent nahi; delete hamesha user ke kehne
  par; memory kabhi server/backup par nahi.

## F19 — Background me lamba kaam (AADHA → P1)
- Hai: background mic service (v2.15, baat-cheet jaari) + BootReceiver.
- Nahi: lamba job runner — job do, baat karte raho, job poora ho
  to bataye (routines queue foreground UI me chalti hai).
- Pipeline: "Ye kaam chalu rakho, main baat karta hun" → job
  WorkManager/background scope me → progress notification (kabhi
  chhupke nahi) → poora ho to bol kar + notification → user
  "dikhao" bole to result screen.
- Frontend: Jobs screen (chal rahe + poore jobs, cancel button).
- Backend: kuch nahi (job on-device; server job baad me, alag plan).
- Database: jobs_local (id, kaam, status, result, on-device).
- Gates: job cancel hamesha possible; battery saver par job ruk
  sakta hai (Vivo limit imandari se batayi jayegi); payment wale
  job kabhi auto nahi.

## F20 — Routines sikhana (AADHA → P4)
- Hai: Routines screen + queue (manual banayi routines, ek
  confirmation me chalti hain, call/SMS steps queue me nahi).
- Nahi: TEACH-BY-DOING — ek baar kaam karke dikhao, Flora use
  macro bana kar save kare.
- Pipeline: "Seekho" mode ON → user kaam karta hai (app kholna,
  steps) → Flora steps record kare (sirf user ke bataye hue steps,
  password kabhi record nahi) → macro banao + naam do → user
  "chalao" bole to ek confirmation me poora macro → fail step
  par ruko + batao (chhupke skip nahi).
- Frontend: Routines screen (record mode + macro list + run).
- Backend: kuch nahi (macro on-device).
- Database: routine_macros (steps JSON, on-device; passwords kabhi
  isme nahi).
- Gates: recorder password kabhi record nahi karega (pehle se
  rule); payment step macro me kabhi nahi; macro run se pehle
  steps user ko dikhenge.

## F21 — PC-phone link (NAHI → P4)
- Hai: kuch nahi (koi sync code nahi).
- Pipeline: Phone par link code dikhao → PC companion (alag app/
  web, baad me) par code daalo → Supabase se pairing (device id
  + code, expiry 10 min) → kaam sync: phone par shuru → PC par
  dikhe → PC par khatam → phone par result (dono taraf same
  account uid se) → unlink kabhi bhi ek tap par.
- Frontend: Link screen (code + linked devices + unlink).
- Backend: Supabase: pc_links table (uid, device, code hash,
  expiry) + Edge Function pair/unpair (service key, JWT gate).
- Database: pc_links (upar) + sync_jobs (id, uid, kaam, status).
- Gates: code 10 min me expire; pairing ke bina koi sync nahi;
  personal data sync me bhi encrypted rahega (baad me E2EE plan).

## F22 — Home dashboard (NAHI → P5, safety-gated)
- Hai: kuch nahi (hardware track alag hai, safety rule ke saath).
- Pipeline: Home Box (relay box, electrician se install, MCB OFF)
  → Bluetooth (ghar me) / WiFi (kahin se) pairing (ID + password,
  1 press pairing) → app dashboard (light/fan, har channel ka
  status) → command → box → status wapas → family share (ID +
  password se, user ki marzi se).
- Frontend: Home screen (dashboard + channel toggles + share).
- Backend: box ka apna control server (baad me, alag plan) +
  Supabase: home_devices (uid, box id, channels) — sirf meta.
- Database: home_devices + home_share (uid, box, member).
- Gates: MAINS KAAM SIRF TRAINED ELECTRICIAN, MCB OFF, 5A/channel,
  <10A board; "green wire = phase" wali galat baat kabhi copy
  nahi hogi (green = earth); box ke bina dashboard khaali dikhega,
  jhootha device nahi.

## U1 — Ek cheez do baar na bhejna (AADHA → P5)
- Hai: call/SMS par ek-confirmation rule (dobara confirm ke bina
  dobara send nahi hota).
- Nahi: har action par dedup id (jaldi me do baar bolne par bhi
  ek hi baar send ho — ye guarantee abhi code me nahi hai).
- Pipeline: Har send action ko dedup id (action + target + time
  window 30s) → wahi id dobara aaye to "ye abhi bheja hai" bol
  kar roko → user "phir bhi bhejo" bole tabhi dobara.
- Frontend: kuch naya nahi (confirm UI hi hai).
- Backend: kuch nahi.
- Database: sent_dedup_local (id, waqt, 1 din retention).

## U2 — Kamzor network par stable (AADHA → P5)
- Hai: v2.15 me asli error code (network vs 401 vs 404 vs quota
  alag batata hai) + "Keys check karo" (bina generation cost).
- Nahi: auto-retry (1 baar, backoff ke saath) + offline queue
  (message bhejna network aate hi) + weak-network mode (chhota
  model/lite lane pehle).
- Pipeline: Fail → code dekho → network fail ho to 1 retry (2s
  baad) → phir bhi fail to queue me daalo (user ko bata kar) →
  network wapas aaye to queue chalao (har item par confirm nahi,
  par user ko list dikhegi) → quota fail ho to lite lane try karo.
- Frontend: Queue screen (pending items + retry/cancel).
- Backend: kuch naya nahi (wahi lanes).
- Database: offline_queue_local (id, kaam, waqt).

## U3 — Jo zyada use ho, usme tez (NAHI → P5)
- Hai: kuch nahi (koi seekhne wali layer nahi).
- Pipeline: Use count (on-device, kis feature ko kitna use kiya)
  → zyada use hone wale ko pehle lane/cache me rakho → brain
  ko user ki pasand (tone, chhota/lamba jawab) yaad rahe (F18
  ke saath) → koi personal data server par nahi jayega.
- Frontend: kuch naya nahi (andar hi andar).
- Backend: kuch nahi.
- Database: usage_stats_local (feature, count, on-device).

## Cross-cutting (har phase par lagu)
- Server (Supabase Flora, pehle se live): 7 Edge Functions
  (licence-verify, usage-count, send-push, admin-approve,
  push-token-sync, admin-list, brain-proxy) + 8 tables + RLS.
  Naye tables is plan se: pc_links, sync_jobs, home_devices,
  home_share. Baaki sab on-device (privacy: screen, voice,
  notifications, WhatsApp, memory kabhi server par nahi).
- Brain-proxy step 2 (pehle se pending): owner keys set kare
  (dashboard) → JWT gate → quota → app wiring. Uske baad hi app
  ka brain server se chalega; tab tak user ki key seedha phone
  se provider ko jati hai (yehi abhi ka sach hai).
- Permissions: har naya feature apni permission ke saath
  Permissions screen me aayega (live status + button) — koi
  permission chhupke nahi.
- Battery/OEM: Vivo/iQOO background kill imandari se bataya
  jayega; battery unrestricted + auto-start + notification teeno
  ON rakhna har background feature ki shart hai.
- Har item ka gate (pehle se rule): code audit → CI PASS →
  phone test entry → tabhi "done". Phone test ke bina koi item
  done nahi (v2.15 ki test entry iska template hai).

## Banane ka order (one-by-one, rukna mana nahi)
- P1: F17 (port, validation pehle se PASS) → F5 sleep + wake tune
  → F1 barge-in v1 (Deepgram key ka faisla user se) → F19 job
  runner → F18 auto-memory suggest → F6 voice-per-persona.
- P2: F4 tap/type/swipe + device settings → F7/F8 continuous
  vision → F10 notification listener (padhna only).
- P3: F12 research loop → F15 watchlist → F16 whiteboard →
  F11 code-writer v1 → F14 docs + email → F13 post (confirm se).
- P4: F2 voice uthao/kaato → F3 WhatsApp (confirm se) →
  F20 teach-by-doing → F21 PC link.
- P5: F22 Home dashboard (safety-gated) → F9 game commentary →
  U1 dedup → U2 retry/queue → U3 learning layer.
- Har phase ke baad: full-layer audit → issue 0 → agla phase.
  Koi phase "100% complete" tabhi jab uske saare items ka phone
  test PASS ho.

## Kabhi nahi banega (is plan se bhi nahi badlega)
- Creator ka code/UI/text/assets/character/trademark copy.
- Silent screen dekhna/record karna, silent call record.
- Silent posting/replying (WhatsApp, Instagram, Facebook, email).
- Hard God-Mode lock, DISABLE_KEYGUARD unlock.
- Romantic/18+ persona.
- Payment auto-confirm (payment hamesha alag explicit approval).
- Screen content server par bhejna (opt-in chosen-photo ke alawa).
- Purani keys/backend/data/signing key reuse.

## Aakhri check (plan poora hai ya nahi)
- Screenshot ke 22 cards: sab upar hain (F1–F22).
- Latest-updates ke 10 points: sab map hain — Voice Guardian F17,
  Deep research F12, Live prices F15, Whiteboard F16, Sends-once U1,
  Website build F11, Pictures post F13, Screen share F8, Weak
  network U2, Quicker with use U3.
- "Aapka order" card: feature nahi, isliye plan me nahi — uske
  characters Flora me nahi aayenge.
- Original ke daam: Flora par lagu nahi (₹199/month FINAL).
- Har feature par: frontend + backend + database + workflow +
  gates — paanchon hain ya "kuch nahi" saaf likha hai.
