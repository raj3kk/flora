# Journal (v1.6) — phone test entry (per-item audit gate, rule 2026-10-28)

Journal audit ke teeno hisse me se (c) yahan taiyaar hai; (a) code
audit done hai, (b) CI result aate hi poora hoga. Ye entry test plan
me merge hogi jab journal audit PASS ho jayega.

## Phone par check karne wale steps
1. Home se "Journal" button dabao — Journal screen khulni chahiye.
2. Mood chuno (5 me se koi) + ek line likho + "Journal save karo"
   dabao — entry list me sabse upar dikhni chahiye, mood + date/time
   ke saath.
3. App band karke dobara kholo — entry ab bhi honi chahiye (DataStore
   persist).
4. Kisi entry ka "Delete" dabao — wo entry list se hatni chahiye.
5. Khali text ke saath save dabao — kuch save nahi hona chahiye, koi
   crash nahi.
6. Journal ka koi data server par nahi jaata — iski pushti code se hai
   (sirf on-device DataStore); phone par iska koi visible test nahi.
