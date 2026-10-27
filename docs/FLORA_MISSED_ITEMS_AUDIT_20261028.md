# Missed-items audit — plan + app (2026-10-28, user ke sawaal par)

User ne sahi pakda: video/research me "Maya dekhti thi" wala hissa
Flora me miss tha. Poora dobara audit kiya gaya — jo plan me nahi
tha ya app me nahi bana, sab yahan hai. Koi item chhupaya nahi gaya.

## 1. VISION — miss tha, is batch me fix ho raha hai
- Video #13 Scan: "kisi cheez/screen/copy ko dikhake us par sawaal
  poochna, padhke batana". Video #112: back camera se copy ke notes
  padhna. Video #105: model image dekh sakta hai ya nahi (toggle).
- Flora v1.0 tak: Scan sirf preview + photo save tha — padhna nahi tha.
- Fix (v1.1, local me ban gaya, build verify ke baad push):
  Scan screen par "Is photo me kya likha hai, padh kar batao" —
  ML Kit Text Recognition ON-DEVICE (Latin + Devanagari), photo
  kabhi server par nahi jaati, padha text screen par + bol kar.
- Agla step (opt-in, default OFF): photo par sawaal-jawaab ke liye
  user ki Gemini key se image samajhna — sirf tab jab user khud
  photo bhej kar poochhe. Screen content ka server rule isse nahi
  tootega: screen kabhi server par nahi jayegi, sirf user ki chuni
  hui photo, uski marzi se.

## 2. Baaki miss — plan me add, app me baaki (priority order me)
1. Live camera Q&A (video samajhna, bolte-bolte) — vision ke baad.
2. Incoming call announcement (call aate hi bol kar batana) — abhi
   sirf outgoing call/SMS hai.
3. Personas — Flora ke apne original characters (creator ke
   Maya/Friday/Venom/Jarvis kabhi copy nahi honge).
4. Triggers — waqt/jagah par routine apne aap chalna (ek
   confirmation ke saath, silent nahi).
5. Driving mode — gaadi me bada UI + sirf awaaz se kaam.
6. Maya Home app-side UI — pairing/control screen (hardware +
   mains kaam user/electrician ka hi rahega, safety rules ke saath).
7. Study tools + Website/Coding agent + Market/Marketing tools —
   Docs ke aage ke tools; ek-ek karke.
8. Themes/Orb/3D + live wallpaper — look-feel, functionality ke baad.
9. Gallery face scan / morning welcome — identity face match ke
   saath hi aayega (embedding model + consent enrolment pending).
10. Voice Guardian voice-match — abhi sirf PIN gate hai; ≤10
    consent samples + model step pending.
11. Wake word + FCM wake + admin panel + payment webhook —
    pehle se blocked list me hain (Firebase + user decisions).

## 3. Jo jaan-boojh kar NAHI banega (miss nahi, decision hai)
- Creator ka character, UI text, assets, trademark — kabhi nahi.
- Silent screen dekhna/record karna, silent posting, silent call
  recording, hard God-Mode lock, 18+/romantic persona — excluded.
- Payment auto-confirm — hamesha alag confirmation.
- Screen content server par bhejna — kabhi nahi (vision bhi
  on-device default hai).
