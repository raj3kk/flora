# Usage-count caller wiring — prep (LOCAL, push nahi)

Push tabhi hoga jab v2.2.1 audit PASS + v2.3 ka order aaye
(per-item gate: ek item ke baad doosra). Ye file sirf taiyaari
hai — app code me koi badlav nahi hua hai.

## Aaj ki haalat (grep se verified, 2026-10-28)
- `LicenceClient.countUsage(userId, seconds)` maujood hai
  (data/LicenceClient.kt:66), Edge Function path
  `/functions/v1/usage-count` FloraConfig me hai.
- Caller: KOI NAHI. `countUsage` ko app code me kahin se
  call nahi kiya jata.
- Local counting KAAM karti hai, alag raaste se:
  MainActivity.kt me `voice.onSpoken = { seconds -> ... }`
  callback `prefs.addTalkSeconds(today, seconds)` karta hai.
  FloraApp.kt ke 3 jagah (line ~257 routine, ~558 attachment,
  ~633 chat) sirf `talkSecondsUsed(today)` padh kar 600 sec
  limit check karte hain.
- Iska matlab: local limit tabhi trigger hota hai jab TTS
  bola ho (spoken seconds). Server ko iska kuch pata nahi
  chalta — app data clear hote hi local counter reset ho
  jata hai. Yehi audit gap hai.

## Wiring ka ek hi sahi jagah
- MainActivity.kt ke `voice.onSpoken` callback me,
  `addTalkSeconds` ke SAATH HI `licenceClient.countUsage`
  call karna hai. Teenon FloraApp sites me alag-alag call
  lagana galat hoga — double/triple counting ho jayegi,
  kyunki ek hi jawab teen jagah se nahi, ek TTS playback se
  count hota hai.
- Call ki shartein:
  - Sirf jab licence plan "pro" NAHI hai (pro unlimited hai;
    server par pro ki counting zaroori nahi).
  - `countUsage` khud no-anon-key par `counted=false` deta
    hai, isliye key na ho to bhi crash/fail nahi hoga —
    local counting tab bhi chalegi (fallback barkarar).
  - Server fail ho to local counting par koi asar nahi
    (fire-and-forget, UI block nahi).
- `userId` ke liye `prefs.userId()` use hoga — wahi function
  jo FloraApp me licence verify ke liye use hota hai
  (line ~161, ~1068). Naya ID format nahi banana.

## Wiring se PEHLE test (order mat ulta karna)
1. usage-count Edge Function ko seedha test karo: test
   user_id + seconds post karke `counted=true` aana chahiye
   aur usage table me row badhni chahiye. Ye function abhi
   tak kisi live row ke against test nahi hua hai.
2. Uske baad hi app wiring push karo, phir phone par ek
   chhota talk karke local counter + server row dono check
   karo (test entry me ye step likhna hai).
3. Server row aur local counter me farq aaye to server ko
   authoritative maanna hai — par UI me farq chhupana nahi,
   audit me likhna hai.

## Kya NAHI karna
- FloraApp ke 3 limit-check sites me countUsage mat lagana.
- Pro user ki har baat server par count karke "usage" mat
  badhana (unlimited plan ka matlab badal jayega).
- Count fail hone par user ko error dikha kar talk rokna
  nahi — local fallback hi kaafi hai.
