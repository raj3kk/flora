# Flora Automation Master Plan v2 — CEO perspective (2026-10-28)

User dream (user order 2026-10-28): Flora phone ka har kaam kar sake —
koi bhi app kholo, usme navigate karo, click/scroll karo, Chrome me
website kholo aur form bharo, bolte-bolte live command se, background
aur foreground dono me, server online rahe, wake ho — jo user bole wo
real me execute ho. Ye plan v1 (178 features) ko upgrade karta hai;
jo plan me nahi tha, wo yahan add hai, audit ke saath.

## CEO perspective — kyun jeetega, kahan risk hai
- User value: ek awaaz = poora phone. Typing, dhoondhna, repeat kaam
  khatam. Sabse zyada value un logon ko jo phone par slow hain ya
  haath busy rakhte hain (driving, kaam, buzurg).
- Moat: on-device automation + privacy. Screen ka content kabhi server
  par nahi jayega — jo assistant screen padh kar server bhejta hai, wo
  trust kabhi nahi jeet sakta. Flora ka automation sirf phone par chalega.
- Business fit: Free = 10 min/day, ~15 tools. Pro ₹199/month =
  unlimited baat + full automation. Automation hi Pro khareedne ki wajah
  banega — isliye automation ko kabhi half-baked mat chhodo.
- Risk (imaandaari se): (1) Har app ka UI alag hai aur badalta rehta
  hai — koi button na mile to Flora ko ye bolna hoga, guess-click nahi.
  (2) Accessibility permission powerful hai — isliye user khud ON karega,
  visible stop hoga, aur service ka kaam sirf phone par hoga.
  (3) Payment/checkout kabhi auto-confirm nahi hoga — paisa hamesha
  alag se explicit approval mangega (standing safety rule, is plan se
  bhi upar hai). (4) Sideload app hai — Play Store ki accessibility
  policy is par lagu nahi hoti, par user ka bharosa hi asli licence hai.

## Automation ka ek rule (sab jagah lagu)
Ek task = ek baar bolo + ek confirmation, phir poora execution bina
ruke, bolte hue commentary ke saath. Beech ke har click/scroll par
dobara poochna nahi. Sirf paisa, delete, aur kisi ko message bhejne
wale final step par dobara confirm hoga.

## Modules + detailed workflow (har ek ka poora flow)

### A1. Universal app launcher (v1.0 — FOUNDATION, is batch me)
Flow: user bole "open WhatsApp" → Flora installed launchable apps me
naam match kare (contains, case-insensitive) → ek confirmation dialog
("WhatsApp kholu?") → launch intent → spoken "WhatsApp khol diya".
Na mile to: "Ye app nahi mili, naam dobara boliye" — guess nahi.
Tech: PackageManager queryIntentActivities (MAIN/LAUNCHER) + manifest
<queries> block. Koi QUERY_ALL_PACKAGES nahi chahiye.

### A2. Accessibility automation service (v1.0 — FOUNDATION)
Flow: user Settings → Accessibility → Flora Automation ON kare (khud,
manual — kabhi silent ADB se nahi, na chhup kar). ON hone par Flora:
- clickText(text): screen ke nodes me text/contentDescription dhoondh
  kar ACTION_CLICK.
- setText(text): focused EditText par ACTION_SET_TEXT.
- scrollDown/scrollUp: scrollable node par ACTION_SCROLL_FORWARD/BACKWARD.
- goBack/goHome: GLOBAL_ACTION_BACK/HOME.
- Har action ka result bol kar bataya jayega ("Submit dab gaya" /
  "Ye button screen par nahi mila").
Tech: AccessibilityService + config XML (eventTypes all,
retrieveInteractiveWindows, reportViewIds). Screen content sirf phone
par process hoga, kabhi server par nahi jayega, kabhi screenshot save
nahi hoga. Visible stop: Accessibility OFF karte hi sab ruk jayega.

### A3. Chrome website + form fill (v1.0 foundation, v1.1 full workflow)
Flow: "chrome me example.com kholo" → browser intent (https) → Chrome
khulega → user bole "form bharo: naam Priyadarshi, email ..." →
Flora field par click (label text se) → setText → agla field → aakhir
me Submit button ka naam bol kar ek confirmation → click.
Honest limit: har website ka form alag hota hai; label na mile to
Flora ruk kar batayegi, galat field me galat data nahi bharegi.
Payment wale form par amount+recipient ka alag confirmation hoga.

### A4. Voice commands, baat karte-karte (v1.0 Chat me, v1.1 live)
Abhi: Chat me type/bol kar — "call <number>", "sms <number> <msg>",
"open <app>", "chrome me <site> kholo", "photo lo", "sos bhejo".
Har command par spoken commentary + ek confirmation (call/SMS/payment).
Agla: live conversation mode (Pro) — baat chalti rahe, command aate hi
background me execute ho, commentary bolte hue.

### A5. Background + foreground + wake (phased, jhootha daava nahi)
- Foreground: app khuli ho to sab kuch turant.
- Background: AccessibilityService system-bound hai, app band hone par
  bhi service zinda rehti hai (jab tak user ne OFF na kiya ho).
- Wake: server (Supabase) hamesha online hai. Phone ko bahar se
  jagane ke liye FCM push chahiye — Firebase project user se pending
  hai (google-services.json + server key). Uske bina "door se jagao"
  ka daava nahi.
- Wake word ("Flora suno" jaisa): on-device model + dataset + phrase
  decision user se pending hai. Jab tak model nahi, app khol kar bolna
  hi wake hai — isko wake-word bol kar becha nahi jayega.

### A6. Routines — logon ka asli dream (v1.1)
"Subah 8 baje: mausam batao, aaj ke kaam yaad dilao, pehla SOS
contact ko good morning bhejo" — routine = commands ki list, ek
confirmation se poori chalegi, har step bol kar. Routine sirf on-device
save hogi, server par nahi.

### A7. Har app ka kaam — kya sach me hoga, kya nahi (audit)
Hoga: app kholo, button/text dhoondh kar click, scroll, field me type,
dialer/SMS/share intents, browser kholo.
Nahi hoga (wajah ke saath): social app par silent post (unka login/SDK
chahiye); kisi app ka andar ka data nikaalna (API/login ke bina nahi);
payment auto-confirm (safety rule); screen ko chhup kar dekhna/record
karna (excluded, kabhi nahi); doosre ka phone control karna (sirf
apna phone, apni permission se).

## Missing / gaps jo is plan me add hue (pehle plan me nahi the)
1. AccessibilityService + config + enable flow + visible stop.
2. Launchable-apps discovery + name matching + confirmation.
3. Chrome open + label-based form-fill workflow.
4. Routines (multi-step, ek confirmation).
5. Background/wake ka saaf division: service vs FCM vs wake word.
6. Per-action spoken commentary (progress + result + failure).
7. Failure honesty: button/field na mile to guess-click nahi, spoken
   "nahi mila" + agla option (user bataye ya manual).

## Phases + gates (har phase: implement → build verify → audit → agla)
- v1.0 (is batch): A1 + A2 foundation + A3 open + A4 Chat commands.
- v1.1: A3 full form-fill + A6 routines + live conversation mode.
- v1.2: FCM wake (Firebase milne par) + wake-word model (phrase +
  dataset decision ke baad).
- v1.3: Admin panel + payment webhook (UTR auto-verify) + final
  backup restore test + phone E2E + release gates 4–7 close.
Koi phase "done" nahi jab tak build PASS + phone test + audit PASS na ho.
