# Flora v2.9 retention — phone test entry (CI PASS ke baad)

## Steps (jo phone par check ho sakta hai)
1. Journal me aaj ki entry daalo, ek Memory add karo.
2. App band karke dobara kholo → dono entries maujood honi
   chahiye (aaj ki entries prune nahi hoti).
3. Privacy screen kholo → retention wali line padho: Journal
   30 din baad auto-delete; Memories me date wali 30 din baad
   auto-delete, bina date wali manually.
4. Settings ka retention note bhi wahi precise wording me
   dikhna chahiye.

## Honest limit (chhupaya nahi jayega)
- 30-din ka asli prune phone par tabhi dikhega jab koi entry
  30 din purani ho — UI se date fake nahi ki ja sakti (by
  design, koi debug backdoor nahi hai). Isliye prune ka proof
  abhi sirf code audit + CI hai; 30 din baad ki entry par
  phone test se confirm hoga.
- Bina date wali purani Memories kabhi auto-delete nahi
  hongi — unko Memories screen se manually delete karna hoga.
  Ye Privacy text me saaf likha hai.
