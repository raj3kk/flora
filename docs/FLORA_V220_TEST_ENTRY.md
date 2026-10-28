# Flora v2.20 test entry (phone par check karne ke liye)

Version: 2.20.0-rebuild, versionCode 42. CI PASS ke baad hi "done" mana jayega.

## 1. Brain kabhi technical error nahi bolega (user order)
- Koi bhi sawaal poochho jab brain down ho ya key me dikkat ho:
  Flora ko kabhi "code 404", "model nahi mila", "API key gadbad"
  jaisi baat NAHI bolni chahiye. Sirf pyar se normal baat —
  jaise "abhi meri soch wali line atak gayi hai, phir se bolo na".
- Basic baat bina brain ke bhi chalni chahiye: "namaste",
  "kaise ho", "tum kaun ho", "kitne baje hain", "aaj ki date",
  "shukriya", "bye" — in par Flora ko turant pyar se jawab
  dena chahiye, bina kisi error ke.
- Asli kaaran dekhna ho to Settings > "Keys check karo" me
  dekho — wahan likha hoga ki key galat hai, quota khatam hai,
  ya is key wale project me Gemini API on nahi hai.
- Tone: jawab muskaan + pyar se, chhota, tumhare tone me.
  Awaaz thodi narm (pitch thoda upar, speed thodi dheemi).

## 2. Screen share — ek baar permission, phir har baar (user order)
- Live screen kholo → "Screen permission do" → permission do.
- "Is screen par kya hai?" dabao — frame milna chahiye.
- Dobara wahi button dabao — is baar bhi frame milna chahiye
  (pehle dobara "frame nahi mila" aata tha, ab nahi aana chahiye).
- "App kholo aur dekho": naam likho (jaise WhatsApp) → Flora
  ko app kholni chahiye, 3 sec rukna chahiye, phir us app ki
  screen dekh kar batana chahiye.
- Session mar jaye to Flora ko pyar se dobara permission
  maangni chahiye aur dialog khud kholna chahiye.

## FAIL ho to
- Kaunsa step ruka, Flora ne kya bola (exact shabd), aur
  Settings > Keys check me kya likha hai — ye teeno batao.
