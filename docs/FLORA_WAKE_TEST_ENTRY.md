# Flora v2.4 — Wake Word v0 phone test entry

Item: Wake Word "Hey Flora" v0 (MFCC mean template, foreground
service, default OFF). Phone test ke bina ye item PASS nahi hai.

## Steps (phone par)
1. Settings kholo — "Wake Word — Hey Flora" section dikhna
   chahiye, status "Wake Word: band hai".
2. Bina enrol kiye Wake Word switch ON karo — "Pehle enrol
   karo" message aana chahiye, service start NAHI honi chahiye.
3. "Enrol: 3 baar 'Hey Flora' bolo" dabao — 3 samples bolo,
   har sample ke baad status badalna chahiye, ant me "Enrol ho
   gaya" aana chahiye. (Template sirf is phone par encrypted
   save hota hai; koi audio sample save nahi hota.)
4. Mic permission allow karke Wake Word ON karo — notification
   tray me "Flora sun raha hai" notification hamesha dikhni
   chahiye. Ye notification hi proof hai ki sunna chhupke nahi
   ho raha.
5. Zor se "Hey Flora" bolo — 10 second ke andar "Haan, boliye"
   sunai dena chahiye. Dobara turant bolne par 10 second
   cooldown me dobara jawab NAHI aana chahiye.
6. Normal baat-cheet / TV ke saamne 2 minute rakho — galat
   trigger (false positive) ho to uska exact shabd + waqt
   report karo. v0 me false trigger ho sakta hai (threshold
   0.60 sirf shuruaati guess hai) — ye expected risk hai, bug
   nahi chhupaya jayega.
7. Switch OFF karo — notification gayab honi chahiye, uske
   baad "Hey Flora" bolne par koi jawab NAHI aana chahiye.
8. Face screen se delete-all karo — uske baad Wake Word ON
   karne par phir se "Pehle enrol karo" aana chahiye (wake
   template bhi delete hota hai).

## Kabhi nahi (is test me bhi)
- Wake Word se PIN unlock, koi action, koi posting kabhi nahi
  hota — sirf TTS jawab. Agar kuch aur ho to turant report karo.
- Enrol ke dauraan koi audio file phone par save nahi honi
  chahiye (sirf encrypted template file wake_template.enc).
