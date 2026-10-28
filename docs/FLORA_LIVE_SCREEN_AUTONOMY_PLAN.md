# Flora Live Screen Autonomy — Audit, Research, Class Plan (2026-10-28)

User order: Live screen par Flora apne aap dekh sake — kaam ke hisaab se
khud session banaye, poori entire screen ka permission le, user ke
"dekho kya hai" bolte hi frame dekh kar poora jawab de aur poora kaam
kare. Ye doc batata hai: abhi kya hai, Android kya karne deta hai aur
kya kabhi nahi dega, kaise karega, kya-kya lagega, aur class-wise plan.

## 1. Audit — abhi kya hai (code-verified, 2026-10-28)

- ScreenCapture.Session: MediaProjection se frame leta hai (poori screen,
  kisi bhi app ki). v2.22 me session app-level LiveSessionHolder me hai
  (screen badalne par nahi marta), asli maut MediaProjection.Callback se
  pata chalti hai, metrics construction par capture hote hain.
- VisionClient: frame JPEG -> Gemini vision -> Hindi/English mix jawab.
  v2.22 se retired 1.5-flash hata, auto-discovery chain hai (verified
  working models: gemini-flash-lite-latest, gemini-3.5-flash).
- LiveScreen: permission button, sawaal buttons, continuous (ROKO tak,
  har 10 sec ek frame). Kami thi: app ka naam TYPE karna padta tha.
- Chat (FloraApp): screen ka koi voice raasta NAHI tha — "screen dekho"
  bolne par Flora Live screen par jaane ko kehti thi, khud nahi dekhti thi.
- Call: FloraInCallService tha par koi call UI nahi tha — default dialer
  hone par bhi user ko koi option nahi dikhta tha.

## 2. Research — Android ke niyam (ye nahi badal sakte)

1. **Permission bina tap ke KABHI nahi milegi.** MediaProjection ka
   system dialog har nayi projection ke liye user ka Allow tap maangta
   hai. Koi app, koi setting, koi tareeqa is tap ko bypass nahi kar
   sakta. "Khud permission le" ka matlab sirf ye hai: Flora bolne par
   dialog KHUD kholegi, aur ek baar Allow milne ke baad session ko
   zinda rakh kar dobara tap nahi maangegi.
2. **Session process ke saath marta hai.** App ka process mar jaye
   (Vivo battery optimization aggressive hai) to dobara Allow tap
   chahiye hi chahiye. Isliye battery-unrestricted + auto-start ON
   rakhna zaroori hai — ye phone ki setting hai, code se nahi hoti.
3. **Secure windows black aati hain.** Banking apps, password screens,
   DRM video (FLAG_SECURE) par MediaProjection black frame deta hai —
   ye Android ki security hai, Flora iska kuch nahi kar sakti.
4. **Har frame ek Gemini call hai** = user ki key par kharcha. Isliye
   continuous me 10 sec ka gap rakha hai (cap hataya ja chuka hai,
   ROKO dabane tak chalega).
5. **Indicator hamesha dikhega.** Capture chalte waqt system indicator
   (notification/icon) dikhta hai — chhup kar screen dekhna possible
   nahi, aur karna bhi nahi hai (privacy rule).

## 3. Kaise karega — poora flow (bolne se lekar jawab tak)

1. User Chat me (ya background mic se) bolta hai: "screen dekho" /
   "is screen par kya hai" / "screen share chalu karo" /
   "<app> kholo aur dekho".
2. Flora pehle LiveSessionHolder me zinda session dhoondhti hai.
   Mila -> turant frame (3 retry, har baar naya virtual display).
3. Session nahi mila aur permission token hai -> token se naya session.
4. Token bhi nahi -> Flora permission dialog KHUD kholti hai aur bolti
   hai "Allow dabao". User ek tap karta hai -> token milte hi Flora
   apne aap frame lekar jawab deti hai (dobara bolne ki zaroorat nahi
   — launcher callback me session ban kar pending sawaal chalega).
   NOTE: is turn me step 4 ka aadha hissa hai — dialog khulta hai aur
   Allow ke baad "screen dekho" bolne par turant jawab milta hai;
   Allow dabate hi bina dobara bole jawab dena (pending-question
   auto-resume) agle chunk me aayega, kyunki isme launcher callback
   se Chat pipeline tak pending sawaal pahunchana hai.
5. Frame -> JPEG (phone par hi) -> user ki Gemini key se vision ->
   chhota saaf jawab bol kar + chat me likh kar.
6. "App kholo aur dekho": app kholo -> 3 sec ruko -> usi session se
   frame -> detail jawab. Continuous: Chalu -> har 10 sec frame ->
   ROKO tak, session zinda rehta hai agle sawaal ke liye.
7. "Screen share band karo": session stop + token saaf -> agle
   istemal par phir dialog (ye user ka control hai).

## 4. Kya-kya lagega

Phone par (user ko karna hai, ek baar):
- Screen permission dialog par Allow (ek tap, pehli baar).
- Battery-unrestricted + Vivo auto-start ON (warna process marne par
  dobara Allow maangega).
- Settings me working Gemini key (verified chain wali).
- Notification permission ON (capture/service indicator ke liye).

Code me (is plan ke hisse):
- ScreenCapture: Context-based Session + openShared() (DONE).
- FloraApp: screen states + launcher upar (Chat se bhi chale).
- FloraApp sendChat: screen chalu/band/dekho, app kholo aur dekho,
  continuous mic chalu/band (voice settings).
- LiveScreen: app-naam TextField HATAO -> mic button se bolo, auto-run.
- Call: CallActivity (3 options) + InCallService UI-first (auto-answer
  hatao) + manifest entry + ConnectScreen text.
- Agle chunk (pending-question auto-resume, call voice option) — plan
  me darj, is turn ke baad.

## 5. Class-wise plan (kya banega, kis order me)

1. `automation/ScreenCapture.kt` — Session (Context), LiveSessionHolder,
   openShared(). DONE (v2.22).
2. `ui/FloraApp.kt` — screenGranted/screenResultCode/screenResultData
   states, screenLauncher, sendChat screen branches, continuous-mic
   branches, LiveScreen call-site update. IS TURN ME.
3. `ui/LiveScreen.kt` — params se session (apna launcher hatao), mic
   launcher se app-naam bolo + auto-run, TextField hatao. IS TURN ME.
4. `calls/CallActivity.kt` (NAYI) + `calls/CallHolder` — incoming call
   UI: Flora se baat karao / Khud baat karo / Kaato. IS TURN ME.
5. `calls/FloraInCallService.kt` — auto-answer hatao, CallActivity
   kholo, lambdas se answer/route/disconnect. IS TURN ME.
6. `AndroidManifest.xml` — CallActivity entry (showWhenLocked,
   turnScreenOn). IS TURN ME.
7. `ui/ConnectScreen.kt` — dialer text: option-UI wala sach. IS TURN ME.
8. Agla chunk — pending-question auto-resume (Allow ke baad bina
   dobara bole jawab), call par voice choice, home-sync server half,
   brain/tts-proxy deploy (dashboard owner kaam), P4-F21 PC link
   (design + user faisla chahiye), 178 NAHI list ke bade items
   (VoiceInteractionService default-assistant role, neural wakeword
   ke liye recordings, Home hardware) — ye sab alag faisle/cheezon
   par atke hain, isliye is turn me jhootha "sab ho gaya" nahi bola
   ja raha.

## 6. Kya kabhi nahi hoga (imaandari)

- Bina Allow tap ke screen capture — Android nahi deta.
- Banking/password/DRM screen dekhna — black frame hi milega.
- Key/password bol kar dena — security ke liye typed hi rahenge
  (bol kar key dena recording me leak ho sakta hai).
- Call par Flora ki awaaz doosri taraf saaf sunai degi — ye sirf
  dialer-role phone test ke baad kaha ja sakta hai (TTS uplink
  guarantee nahi hai).
