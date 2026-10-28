# Flora v2.8 app-link — phone test entry (CI PASS ke baad)

## Pehle se chahiye
- Website par account bana hua ho (signup.html se) aur us
  account se Pro licence approved ho (admin-approve se) — warna
  link ke baad bhi app me Free hi dikhega, ye expected hai.

## Steps
1. App kholo → Settings → "Website account link" section dikhe.
2. Website wala email + password daal kar "Website account
   link karo" dabao → note me "link ho gaya" aaye; email
   screen par "Linked: <email>" dikhe. Password field khaali
   ho jaye (dobara kabhi na dikhe).
3. App band karke dobara kholo → Settings me linked email
   abhi bhi dikhe (link toota nahi).
4. Chat screen par licence refresh karo → website wala Pro
   status dikhe (agar admin ne approve kiya hai).
5. "Unlink karo" dabao → "random-UUID mode" note aaye aur
   linked email gayab ho.
6. Galat password daal kar link try karo → "Link fail" note
   aaye, koi link save na ho, koi password kahin na dikhe.

## Honest limits
- Link ke baad purani random-UUID licence (agar thi) orphan
  ho jayegi — ye UI me link se pehle likha hai.
- Password change par link tootega — dobara login karna hoga.
- Refresh token sirf is phone par hai; phone kho jaye to
  website se password change karke link toda ja sakta hai.
