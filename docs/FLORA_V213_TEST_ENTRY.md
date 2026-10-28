# Flora v2.13 phone test entry — setup wizard, contacts, location

Version: 2.13.0-rebuild (versionCode 35). CI PASS ke baad phone par ye steps chalao.

## 1. Setup wizard (fresh install par)
1. Purana app uninstall karke naya APK install karo.
2. Step 1 / 3 Welcome dikhna chahiye — "Shuru karo" dabao.
3. Step 2 / 3 Permissions dikhna chahiye — 8 rows: Microphone,
   Notifications, Camera, Phone calls, Contacts, SMS, Phone state,
   Location. Har row par "Baaki hai" dikhna chahiye.
4. Kisi ek row par tap karo — sirf usi permission ka system dialog
   aana chahiye; Allow karo to row par "Mil gaya ✓" ho jaye.
5. "Sab allow karo" dabao — Android ke dialogs ek-ek karke aane
   chahiye (ek ke baad doosra), sab Allow karo.
6. Mic Allow kiye bina "Aage badho" dabna nahi chahiye (disabled
   dikhe) aur note dikhe ki mic allow hote hi khulega. Mic Allow
   karte hi "Aage badho" khul jaye — dabao to Step 3 khule.
7. Step 3 / 3 Background dikhna chahiye — Battery row par tap karo
   to system dialog aaye; Auto-start row par tap karo to Vivo
   auto-start ya app-info khule; Accessibility row par tap karo to
   Accessibility settings khule. "Status refresh karo" se status
   update ho. "Ho gaya" dabao to Chat khul jaye.
8. App band karke dobara kholo — wizard dobara NAHI aana chahiye.
   (Settings > "Permissions dobara dekho" se wizard phir khul jaye.)

## 2. Contacts se call/SMS
1. Wizard me Contacts Allow karo (ya Settings se).
2. Chat me likho: "call <kisi contact ka naam>" — Flora ko number
   dhoondh kar confirmation maangni chahiye (number dikhna chahiye).
   Confirm karo to call lage.
3. Chat me likho: "sms <naam> hello test" — confirmation me naam +
   number + message dikhna chahiye; confirm par SMS jaye.
4. Kisi aise naam se try karo jo contacts me nahi hai — saaf message
   aana chahiye ki contacts me nahi mila, number likhiye.
5. Contacts permission Deny ho to bhi saaf message aana chahiye
   (app nahi atakni chahiye).

## 3. Yahan ka mausam
1. Wizard me Location Allow karo, phone ka Location ON karo.
2. Chat me likho: "yahan ka mausam" — bina brain key ke jawab aana
   chahiye (temperature, humidity, hawa).
3. Location Deny ho to saaf message aana chahiye ki Location
   permission chahiye (koi galat mausam nahi batana chahiye).

## 4. Full-screen pages (arrangement)
1. Menu ▾ kholo — har item dabane par poora page full screen
   dikhna chahiye, koi bikhra button row nahi hona chahiye.
2. Title har screen par badalna chahiye ("Flora — Yaadein" jaisa).

## Fail report me ye bhejo
Kaunsa step, kya dikha, kya hona chahiye tha — screenshot ke saath.
