# Flora v2.3 face-match test entry (photo enrolment)

Phone par check karne ke steps (v2.3 APK par):

1. Face screen kholo — upar enrolment buttons dikhne
   chahiye (camera preview ke upar, chhipe hue nahi).
2. "Enrol photo chuno" → saaf front-face wali photo chuno
   → "Enrol ho gaya" aana chahiye.
3. "Match test photo chuno" → wahi vyakti ki doosri photo
   chuno → "Match hua" + score aana chahiye.
4. Kisi doosre vyakti ki photo se match test karo →
   "Match nahi hua" aana chahiye (score kam dikhega).
5. Bina enrol kiye match test karo (pehle templates
   delete karke) → "Pehle enrol karo" aana chahiye.
6. "Templates delete karo" → phir match test fail hona
   chahiye (koi purana template nahi bachna chahiye).
7. PIN unlock abhi bhi sirf PIN se hona chahiye — face
   match se chat unlock NAHI hona chahiye (ye rule kabhi
   nahi tootega).

Fail ho to: kaunsa step, kya message aaya, kaunsi photo
(front/side/blur) — ye bhej do. Embedding value kabhi
screen par nahi dikhegi, sirf score dikhega.
