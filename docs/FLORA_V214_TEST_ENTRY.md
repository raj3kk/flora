# Flora v2.14 phone test entry — continuous mic, turn-taking, background wake

Version: 2.14.0-rebuild (versionCode 36). v2.13 test entry ke saath ye bhi chalao.

## 1. App khulte hi mic ON (default = baat)
1. Wizard poora karke (mic Allow ke saath) Chat kholo.
2. Chat me "🎤 Sun raha hun…" dikhna chahiye — bina kuch dabaye.
3. Bolo: "yahan ka mausam" — bina type kiye, bina Bhejo dabaye jawab aana chahiye.
4. Bolo: "call <kisi contact ka naam>" — confirmation aani chahiye, phir confirm par call lage.
5. App band karke dobara kholo — phir se apne aap "Sun raha hun…" aana chahiye (mute na kiya ho to).

## 2. Mute + manual typing bhi rahega
1. "🔇 Mute" dabao — sunna band ho jaye, indicator gayab ho jaye.
2. Mute ke baad type karke Bhejo dabao — manual kaam karna chahiye (ye raasta kabhi band nahi hota).
3. "🎤 Mic ON karo" dabao — sunna phir shuru ho jaye.

## 3. Turn-taking + interrupt (smart baat)
1. Lamba jawab bolne do — jawab ke dauraan "🔊 Bol rahi hun…" dikhna chahiye.
2. Jawab ke beech me "Stop" dabao — Flora ko rukna chahiye aur casually poochhna chahiye: "Ruk, kuch bol rahe the?" — phir sunna shuru ho jaye.
3. Jawab khatam hote hi apne aap "Sun raha hun…" wapas aana chahiye — Flora ko apni awaaz khud nahi sunni chahiye (koi self-reply nahi aana chahiye).
4. Chup raho (kuch na bolo) — kuch der baad bhi sunna chalu rehna chahiye, app atakni nahi chahiye.

## 4. Background wake se baat
1. Settings me Wake Word ON karo, 3 samples se enrol karo (agar pehle se nahi hai).
2. App ko background me bhejo (home dabao, doosra app kholo).
3. "Hey Flora" bolo — app aage aani chahiye aur "Haan, boliye" ke baad sunna shuru hona chahiye.
4. Phir bolo: "yahan ka mausam" — jawab aana chahiye.

## Imaandaari se baaki (is push me nahi hai)
- Phone poori tarah pocket me, app band, bina wake word ke lambi background conversation — iske liye poora pipeline service me le jana padega, wo alag bada kaam hai. v2.14 ka background raasta wake-launch hai.
- Beech me bolna (barge-in, Flora bol rahi ho tabhi user bole to Flora turant ruk jaye) — system recognizer TTS ke saath ek saath nahi sun sakta, isliye Stop button hi interrupt ka raasta hai.

## Fail report me ye bhejo
Kaunsa step, kya dikha, kya hona chahiye tha — screenshot ke saath.
