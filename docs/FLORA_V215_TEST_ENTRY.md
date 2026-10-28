# Flora v2.15 test entry (phone par check karne ke liye)

Version: 2.15.0-rebuild, versionCode 37. CI PASS ke baad hi "done" mana jayega.

## 1. Brain / server connection (root-cause fix)
- Settings me Gemini key daalo, "Keys check karo" dabao — Gemini lane sahi dikhni chahiye.
- Baat karo — pehle wali "network/server se baat nahi ho payi" galti nahi aani chahiye.
  (Wajah: purane model names band ho chuke the — Gemini 1.5-flash retired,
  Groq llama-3.1-8b-instant 16 Aug 2026 ko free/dev se hata. Ab Gemini 2.5-flash /
  2.5-flash-lite aur Groq openai/gpt-oss-20b / 120b chalte hain.)
- Galat key daal kar dekho to message me lane + asli code dikhna chahiye,
  generic "network" message nahi.

## 2. Background mic
- Mic ON karo, phir app ko back karo (home button) — Flora background me
  sunti rehni chahiye aur sawaal ka jawab bol kar deni chahiye.
- Notification dikhni chahiye: "Flora background me sun raha hai".
- Background me "call 9876543210" bolo — app aage aani chahiye confirmation
  ke liye (background se call execute nahi hona chahiye).
- Mute dabao — background sunna + notification dono band hone chahiye.

## 3. Permissions screen (Menu > Permissions)
- Battery unrestricted ka live status + button dikhna chahiye.
- Accessibility ka live status + settings button dikhna chahiye.
- Dialer role ka status + button dikhna chahiye.
- Har runtime permission row par status + "Do" button hona chahiye.

## 4. Licence screen (Menu > Licence / Pro)
- Live status (plan/status/server-verified) dikhna chahiye.
- Steps samajh aane chahiye: website signup > UPI pay > UTR save > admin approve.
- ₹199 UPI button + UTR save kaam karna chahiye.

## 5. Baat view (default)
- App khulte hi sirf orb + mic/stop/chat emoji buttons dikhne chahiye —
  likha hua transcript, typing box, status lines nahi dikhne chahiye.
- 💬 dabao to Chat view khule (transcript + typing wapas).
- 🎤 Baat dabao to wapas Baat view.

## 6. Mic beep
- Mic ON/OFF hote waqt "tu-ta" beep nahi aani chahiye (kuch phones ki
  apni beep isse bhi nahi rukti — wo device limit hai, batana).

## 7. Image banao
- "image banao ek billi" bolo — image ban kar Gallery > Flora me save
  honi chahiye, aur Flora bol kar bataye.

## Phone par FAIL ho to
- Kaunsa step, kya message aaya, aur phone ka model — ye teeno batao,
  tabhi root fix hoga. "Kaam nahi kar raha" akela kaafi nahi.
