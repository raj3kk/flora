# Flora CEO Full Plan — Maya se aage (2026-10-28, user order ~20:55 IST)

**Vision:** Flora sirf Maya ki copy nahi — har capability par Maya se
ek level upar, aur jahan Maya chhupi hui cheezon par chalti hai,
wahan Flora khuli hui (visible log, confirmation, ek tap me stop).

**CEO rule (standing):** koi naya feature / koi removal bina user ke
confirmed detail ke nahi. Har item par audit gate: (a) code audit
(declaration order, brace/paren, permissions, privacy, no copy, no
old key), (b) CI build SUCCESS + artifact verify, (c) phone-visible
item par test-plan entry. In_progress/fail = NOT done.

## 0. Is chunk me DONE (2026-10-28 raat)

- **Camera dekhna (CameraLookScreen, naya):** CameraX preview +
  capture (ScanScreen pattern) → JPEG bytes → user ki Gemini key se
  VisionClient → "camera me kya dikh raha hai" ka jawab bol kar +
  screen par. Voice: "camera me kya dikh raha hai / camera dekho /
  camera se dekho / camera me kya hai" → screen khulti hai. Menu me
  "Camera dekho" entry bhi. Permission user khud Allow karega
  (Android rule). Photo Pictures/Flora me save hoti hai; vision ko
  sirf frame milta hai, photo server par upload nahi hoti.
- **Romantic gahri girl voice — samples (style selection ke liye):**
  `your_files/flora-voice-samples/` me 3 samples (Isabella, Lily,
  Myrtle — dheemi gati, Hindi). IMAANDARI: ye samples Muse TTS ke
  hain, Flora ki awaaz nahi. Flora me ye style tab aayegi jab G1
  (Deepgram voice selection) hoga — user jis sample ko chune, usi
  style ki Deepgram voice dhoondhi jayegi. System TTS par aisi
  awaaz nahi milti.

## 1. Action system — Flora me action kaise kaam karta hai

1. User bolta hai → command samjha jata hai (compound split:
   "aur/phir" se toota, app-open direct).
2. **Confirmation rule:** pehli baar ek confirmation, phir poora
   execution + bol kar commentary (har step TTS se).
3. **Auto Mode ON = exception:** background execution bina har
   baar poochhe, par har action Auto Log me + ongoing
   notification hamesha dikhti hai + "auto mode band" ek baar me
   sab rok deta hai.
4. **Background rule:** execution commands Flora ko aage laate
   hain (bring-forward) — chhup kar koi action nahi; sirf screen
   sawaal background me jawab de sakte hain.
5. **Har action ka saboot:** Auto Log + spoken commentary.
   Maya ke automation me aisi transparency surface code me nahi
   mili — yahan Flora aage hai.

## 2. Maya se aage ke systems (Flora-only, plan)

- **CEO hisaab (memory + plan):** Flora yaad rakhe — kya kiya,
  kya karna hai, cheezein kahan hain ("kya kiya, kya karna hai"
  branch — chunk 2 me). Maya ke paas yaadein hain, par hisaab
  branch Flora ka apna hoga.
- **Screen + camera + action ek saath:** screen dekho (accessibility
  screenshot, bina dialog) + camera dekho (is chunk) + action
  (tap/type/swipe, Auto Mode gated) — teeno ek hi voice flow me.
- **Visible safety:** har automation step screen par + Auto Log
  me; Maya ke cross-app macros ki tarah chhupa automation nahi.
- **Encrypted memory:** memories KeyStore AES/GCM (chunk 1 done);
  journal/docs/routines/jobs = G18.

## 3. Production plan G1–G18 — implementation order + audit gate

| # | Kaam | Status / next step |
|---|---|---|
| G1 | Voice quality: Deepgram TTS proxy deploy + key + wiring; tab tak system-TTS prosody | Proxy code repo-ready; deploy + keys user ke haath (G21-2) |
| G2 | Trained wakeword ("Hey Flora") | Code: wake service + threshold; training recordings se (G21-1) |
| G3 | Default-assistant role | Services + assist activity; user system dialog me pick karega (G21-3) |
| G4 | Guardian sensitive actions par | VoiceMatcher + enrol + KeyStore done; extension baaki |
| G5 | Macros, user-approved only | Teach-by-doing done; library + per-macro approval baaki |
| G6 | Notification auto-reply | Aaj read-only; auto-reply sirf faisle par (G21-8 me option) |
| G7 | In-app browser | External browser aaj; in-app screen low priority |
| G8 | Lock screens | Sirf agar chaho, PIN-escapable hi (G21-9) |
| G9 | Live wallpaper | Baad me, G1–G6 ke baad |
| G10 | Home sync server half | Phone half done; server design + faisla (G21-9) |
| G11 | Termux bridge | Faisla + security review pehle (G21-9) |
| G12 | Sub-agent orchestrator | Job runner done; orchestrator baaki |
| G13 | PC link | Design + pairing (G21-9) |
| G14 | OAuth providers | Faisla: kaunse provider (G21-9) |
| G15 | Code/website loop | "code likho" branch + HTML preview + share; public link = hosting faisla (G21-8) |
| G16 | Chrome/GitHub/Vercel voice | Login user khud ek baar; phir voice se operate (confirm har step) |
| G17 | Apne domains pin | Proxy wiring ke saath |
| G18 | Encryption poora | Memories done; journal/docs/routines/jobs baaki |

## 4. G19 — "Android par sach me nahi ho sakta" par research

Har item par imaandari se: kya nahi ho sakta, uske sabse kareeb
kaun sa LEGITIMATE raasta hai, aur uski keemat/limit kya hai.
Koi bhi raasta Android ke niyam tode bina, bina root kiye
(agar likha nahi hai), aur bina user ko andhere me rakhe.

### 4.1 Khud se permission grant

- **Nahi ho sakta:** koi app apne aap ko runtime/special/role
  permission nahi de sakti — ye Android ka core security rule hai.
- **Sabse kareeb legitimate raaste:**
  1. **Shizuku (recommended research direction):** user ek baar
     wireless debugging se Shizuku chalata hai (Android 11+ par
     bina PC ke, phone se hi). Uske baad Flora Shizuku ke zariye
     ADB-level commands chala sakti hai — `pm grant` tak. Ye
     Maya ke ADB-command raaste ka automated version hai, par
     farq ye hai: Shizuku user ne khud chalaya, user kabhi bhi
     band kar sakta hai, aur reboot par dobara chalana padta
     hai. Koi chhupa grant nahi.
  2. **Device owner provisioning:** factory reset + QR se Flora
     ko device owner banana — phir silent grant/policy possible.
     Keemat bahut badi hai (poora phone reset), isliye sirf
     tabhi jab user khud chahe; default kabhi nahi.
  3. **Aaj ka raasta (sabse seedha):** guided setup — Flora
     sahi screen khol deti hai, user ek tap karta hai. Yehi
     aaj Flora karti hai, aur yehi sabse surakshit default hai.
- **Faisla:** Shizuku ko G11 (Termux bridge) ke saath ek hi
  security review me laaya jaye — user ki haan ke bina ek line
  code bhi nahi.

### 4.2 MediaProjection grant process marne ke baad

- **Nahi ho sakta:** capture token process ke saath marta hai;
  har naye process me user ka Allow tap phir chahiye.
- **Asli solution (already built):** accessibility screenshot
  path — service ON ho to bina dialog ke screenshot. Yehi
  woh raasta hai jo MediaProjection ki zaroorat ko khatam
  karta hai. Plus foreground service process ko zinda rakhta
  hai (mitigation, guarantee nahi).

### 4.3 Call uplink me TTS ki guarantee

- **Nahi ho sakta:** Android public APIs se TTS ko call audio
  me reliably inject nahi kar sakte. Koi app ye guarantee
  nahi de sakti — jo de, wo jhooth hai.
- **Sabse kareeb:** speaker + mic loop (aaj ka raasta) +
  saamne wale se confirm karwana ("sunai de raha hai?").
  Doosra raasta: Flora ki apni internet-call (VoIP) — jahan
  Flora khud call ka ek taraf ho, wahan TTS seedha ja sakta
  hai. Ye naya product hissa hai, SIM call ka fix nahi.

### 4.4 WhatsApp call API

- **Nahi ho sakta:** WhatsApp ki koi official personal-call
  API nahi hai. WhatsApp Business API sirf business messaging
  ke liye hai, call ke liye nahi.
- **Sabse kareeb:** incoming WhatsApp call par accessibility
  swipe-answer (at most) + speaker loop. Voice path ki
  guarantee kabhi nahi.

### 4.5 Bina hosting ke public link

- **Nahi ho sakta:** link ke liye server chahiye; phone par
  HTML se local preview/share hi milta hai.
- **Asli solutions (faisla user ka):** (a) Flora ka apna
  Supabase public bucket (G15 hosting faisla) — sabse saaf;
  (b) user ke apne account par static host (GitHub Pages /
  Netlify) — user ek baar login kare, Flora upload kare.
  Dono me content rules zaroori (galat content public nahi).

### 4.6 Play-Store-safe cross-app automation

- **Nahi ho sakta:** store-distributed assistant jo doosri
  apps chalaye, policy rejection ka saamna karta hai.
- **Solution (already scope):** Flora sideloaded private
  build hai, isliye ye viable hai. Play Store par jaana ho
  to automation ka scope kaatna padega — isliye Flora
  private hi rahegi.

## 5. Audit trail

- Is chunk ka audit log: docs/FLORA_PER_ITEM_AUDIT_LOG.md
  (CameraLook + voice samples + ye plan).
- Phone test entry: camera dekhna — v2.22 final ke test list
  me jodega (back camera preview, capture, vision jawab,
  key-empty message, permission-deny message).
