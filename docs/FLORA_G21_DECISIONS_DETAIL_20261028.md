# G21 — Tumhare faisle, detail me + kya sahi hoga (2026-10-28)

Har item par: kya ruka hai, options kya hain, meri sifarish kya
hai aur kyun, aur faisla na hone par kya ruka rahega. Koi item
tumhari haan ke bina aage nahi badhega.

## 1. Recordings — "Hey Flora" wakeword + voice enrolment samples

- **Kya ruka hai:** G2 trained wakeword + G4 guardian. Bina
  tumhari awaaz ke koi trained model nahi ban sakta — kisi aur
  ki awaaz se train karna galat bhi hai, kaam bhi nahi karega.
- **Chahiye:** "Hey Flora" — alag-alag kamron me, halki awaaz /
  normal awaaz / thodi door se, 20–30 chhote clips; guardian ke
  liye 3 × 3 second ke samples (enrol screen par hi).
- **Sahi kya hoga:** tum jab waqt mile tab clips de do — koi
  jaldi nahi, par yehi ek item hai jiska koi alternative nahi
  hai. Tab tak MFCC prototype + threshold se kaam chalega,
  par use "ship-grade" nahi kahunga.
- **Faisla na ho to:** wakeword prototype par hi rahega.

## 2. Keys — Deepgram key + proxy keys + S3 deploy

- **Kya ruka hai:** G1 voice quality (Deepgram TTS) + S3
  (brain-proxy step 2 + tts-proxy deploy) + app wiring.
- **Chahiye:** (a) Deepgram API key (provider pehle se CLOSED
  hai — Deepgram hi rahega); (b) Supabase dashboard secrets
  me GEMINI_API_KEY, GROQ_API_KEY, DEEPGRAM_API_KEY; (c) S3
  deploy (dashboard code editor se, browser task se nahi —
  kyunki browser granted files nahi padh sakta, main text
  paste kar dunga).
- **Sahi kya hoga:** Deepgram key banana free-tier se shuru
  karo (free credits se test, phir usage dekh kar decide).
  Proxy keys dashboard me daalne ke baad main app wiring
  kar dunga — tab Flora ki awaaz Deepgram wali ho jayegi,
  aur tumhare chune hue romantic sample ke style ki voice
  dhoondhi jayegi. Keys kabhi chat me mat bhejna — Settings
  / dashboard me seedha daalo.
- **Faisla na ho to:** Flora system TTS par hi rahegi.

## 3. Roles — default-assistant + default-dialer pick

- **Kya ruka hai:** G3 (system-wide Flora) + call-talk.
- **Chahiye:** Settings me "Default assistant" dialog me Flora
  chunna + "Default dialer" me Flora chunna — dono tumhare
  apne tap hain, koi app ye khud nahi kar sakti.
- **Sahi kya hoga:** pehle default-assistant chuno (is se
  Flora har jagah se bulayi ja sakegi), dialer tab chuno jab
  call UI test karna ho. Dono ek saath karne ki zaroorat
  nahi; assistant pehle, dialer baad me.
- **Faisla na ho to:** Flora sirf apne app ke andar rahegi.

## 4. Phone setup + app link — sabse pehla blocker

- **Kya ruka hai:** admin flag (users row tabhi banegi jab
  app link hoga) + background kaam (battery/auto-start) +
  accessibility (screenshot/gestures) + keys/VPA.
- **Chahiye:** (a) app Settings → "Website account link",
  wahi email+password; (b) battery-unrestricted + Vivo
  auto-start ON; (c) Accessibility ON (+ Android 13 par
  sideloaded app ke liye "Allow restricted settings");
  (d) Gemini key + Groq key app Settings me; (e) apna UPI VPA.
- **Sahi kya hoga:** isi order me karo — pehle app link
  (is se admin flag turant khul jayega), phir battery/
  auto-start, phir accessibility, phir keys. App link ke
  bina admin ka kaam aage nahi badh sakta, isliye ye
  number 1 hai.
- **Faisla na ho to:** admin flag + background kaam ruke
  rahenge.

## 5. Gemini/Groq keys + UPI VPA (app Settings)

- **Kya ruka hai:** brain/vision (Gemini), brain fallback
  (Groq), Pro payment (UPI VPA tumhara apna).
- **Sahi kya hoga:** jo key pehle verify ho chuki hai (35
  models wali), wahi app Settings me daalo — dobara verify
  karne ki zaroorat nahi. Groq key fallback ke liye daalo
  (free tier se shuru). UPI VPA wahi jo tum payment lene
  ke liye istemal karna chahte ho; app kabhi payment approve
  nahi karegi, sirf admin UTR-match se hoga.

## 6. Grievance officer — naam/email (IT Rules)

- **Kya ruka hai:** Flora ki legal pages poori nahi hain.
- **Chahiye:** ek naam + ek email jo Flora ke legal pages
  par grievance officer ke roop me dikhe.
- **Sahi kya hoga:** apna support email
  (flipify.com@gmail.com) hi grievance email rakho, aur naam
  wahi jo tum official kaam me istemal karte ho. Koi nakli
  naam kabhi nahi likhunga — ye tumhara faisla hai, isliye
  tum batao.
- **Faisla na ho to:** legal pages par ye field khaali rahega.

## 7. Website-only approve fix — (i) ya (ii)

- **Kya ruka hai:** website se aaya payer approve nahi ho
  pata (users.device_id NOT NULL ki wajah se row nahi banti).
- **Option (i):** signup par users row banana (trigger) —
  device_id ke liye ek value chahiye hogi (device nahi hai
  to kya likha jaye, ye sawaal hai; nakli device_id nahi
  likhunga).
- **Option (ii):** approve path me website rasta — payer
  ki row na bhi ho to approve ho jaye, licence uske
  website account par lage.
- **Sahi kya hoga:** (ii) — kyunki (i) me device_id ka
  nakli hal nikalna padega, jo galat hai. (ii) me website
  account hi pehchaan hai, device baad me app link se
  jud jayega. Tum (ii) bolo to main code kar dunga.
- **Faisla na ho to:** website-only payer approve nahi hoga.

## 8. Teen naye sawaal — hosting + background auto-execute + overlay

- **(a) Public-link hosting:** website banane par public
  link chahiye ya local preview + share hi kaafi hai?
  Sifarish: Flora ka apna Supabase public bucket (content
  rules ke saath) — sabse saaf, aur Flora ke server par hi
  rahega. Local preview tab bhi rahega.
- **(b) Background auto-execute:** aaj ka niyam — background
  execution ke liye confirmation chahiye, Auto Mode ON
  exception hai. Badalna hai ya rakhna hai? Sifarish:
  RAKHNA — bina confirmation ke background execution
  galat action ka risk hai; Auto Mode hi wo switch hai jo
  tum kabhi bhi band kar sakte ho.
- **(c) Overlay mic button:** kisi bhi app ke upar tairta
  mic button (SYSTEM_ALERT_WINDOW, ek baar Settings toggle).
  Sifarish: HAAN — yehi wo cheez hai jo Flora ko "kahin
  se bhi" banati hai (tum doosri app me ho, mic dabao,
  Flora sun legi). Toggle tumhare haath me rahega.

## 9. Optional — sirf agar chaho

- **Lock screens (G8):** PIN-escapable hi banenge; hard lock
  kabhi nahi. Chaho to batao.
- **Termux bridge (G11):** per-command approval + security
  review ke saath hi; Shizuku wala research isi review me
  aayega. Chaho to batao.
- **PC companion (G13):** kaunsa PC, kis kaam ke liye —
  batao to design karun.
- **OAuth providers (G14):** Google pehle? Aur kaunse?
  Batao to sirf wahi banenge.
- **Auto-reply apps (G6):** read-only hi recommended hai;
  auto-reply sirf un apps me jinke naam tum doge, per-app
  opt-in + visible log ke saath.
