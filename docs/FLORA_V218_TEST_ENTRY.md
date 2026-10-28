# Flora v2.18 test entry (phone par check karne ke liye)

Version: 2.18.0-rebuild, versionCode 40. CI PASS ke baad hi "done" mana jayega.

## 1. Auto Mode default ON (user order)
- App kholo — Settings me Auto mode switch ON dikhna chahiye
  (pehle se OFF tha, ab default ON hai).
- Notification me "Flora Auto mode ON" dikhna chahiye.
- "call 9876543210" bolo — bina confirm dialog ke seedha call
  lagni chahiye + Auto Log me entry honi chahiye.
- "auto mode band" bolo — switch OFF + notification hatni chahiye,
  aur agla call phir confirm dialog ke saath aana chahiye.

## 2. Bol kar narration (har kaam)
- Har voice command par Flora ko bol kar batana chahiye kya kar
  rahi hai ("khol raha hun", "bhej diya", "poori ho gayi") —
  chupchaap koi kaam nahi hona chahiye.
- Routine chalao — har step bol kar ("Step: …" + result) aur
  routine ke call/SMS steps bhi Auto Mode me seedha chalne chahiye
  (pehle skip hote the).

## 3. Ek confirmation abhi bhi lagega (ye sach hona chahiye)
- Payment (Pro khareedna) — hamesha UTR/admin wala raasta.
- Delete-all (Yaadein/Journal saaf karna) — confirm ke baad hi.
- Ek naam ki ek se zyada app mile to Flora ko poochhna chahiye.
- Pehli baar permission (system dialog) — wo system ka hai.
- SOS emergency — confirmation ke baad hi call/SMS.

## Phone par FAIL ho to
- Kaunsa step ruka, Flora ne kya bola, Auto Log me kya likha —
  ye teeno batao.
