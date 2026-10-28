# Flora v2.12 phone test entry — permission gate, keys-check, menu, background

Version: 2.12.0-rebuild (versionCode 34). CI PASS ke baad phone par ye steps chalao.

## 1. Permission gate (pehli baar)
1. App ko uninstall karke naya APK install karo (purana data saaf ho).
2. App khulte hi "Pehle permissions — phir Flora" screen aani chahiye,
   6 permissions kaaran ke saath: Mic, Camera, Call, SMS, Notifications,
   Phone state.
3. "Sab permission maango" dabao, sab Allow karo — har row par
   "Mila hua ✓" dikhna chahiye.
4. "Aage badho" dabao — Chat khulna chahiye. App band karke dobara
   kholo — gate dobara NAHI aana chahiye.
5. Ek permission Deny karke bhi check karo: app nahi atakni chahiye,
   sirf uska feature band hona chahiye.

## 2. Keys-check (Settings)
1. Menu > Settings kholo.
2. Gemini key daalo, "Keys save karo" dabao — "save ho gayi" note aaye.
3. "Keys check karo" dabao — sahi key par "Gemini 1: sahi hai, kaam
   karegi" aana chahiye; galat key par "galat ya expired hai" aana
   chahiye (pehle wala generic "key daaliye" message ab sirf tab
   aayega jab koi key saved hi na ho).
4. Chat me sawaal poochho — key sahi ho to jawab aaye; key galat ho
   to message saaf bataye ki key galat hai, na ki "key daaliye".

## 3. Dropdown menu (arrangement)
1. Upar "Flora — Chat" title ke saath "Menu ▾" button hona chahiye;
   16 bikhre buttons nahi hone chahiye.
2. Menu kholo — 5 groups dikhne chahiye: Baat-cheet, Tools, Meri
   cheezein, Setup, Kanooni. Har item dabane par wahi screen khule
   aur title badle (jaise "Flora — Yaadein").

## 4. Background + notifications
1. Settings me "Background aur notifications" section me notification
   aur battery status live dikhna chahiye.
2. "Notification permission do" se permission milne ke baad status
   "mila hua ✓" ho jaye.
3. "Battery optimization band karo" se system dialog aaye; Allow ke
   baad "Status refresh karo" se status "band hai ✓" ho jaye.
4. Vivo/iQOO par "Auto-start settings kholo" se auto-start screen
   khule (ya app-info khule).
5. Wake Word ON karo, phone restart karo — reboot ke baad Wake Word
   ki notification wapas aani chahiye (bina app khole).
6. Push: admin ke send-push ke baad notification aani chahiye aur
   Wake Word ki ongoing notification se takrana nahi chahiye
   (pehle dono ka id 1001 ek tha — ab push ka id 2001 hai).

## Fail report me ye bhejo
Kaunsa step, kya dikha, kya hona chahiye tha — screenshot ke saath.
