# Flora phone test plan — v1.4 / v1.4.1 ke baad (user ke phone par)

Ye checklist tab chalegi jab latest PASS APK milega. Har step ka
result PASS/FAIL likhna hai — andaze se "ho gaya" nahi bolna.

## 0. Install se pehle
- Purana Flora (agar ho) uninstall karo, phir naya APK install karo.
- Version check: Settings > Apps > Flora me versionCode 15 /
  1.4.1-rebuild dikhna chahiye (jo APK mile uske hisaab se).

## 1. Permissions (user khud dega, ek-ek karke)
- Mic (mandatory), Notifications, Camera, Contacts, Calls, SMS,
  Location, Gallery, Bluetooth, Accessibility (FloraAutomationService),
  Notification access, Battery optimisation OFF, Default assistant
  (jo chaho). Har permission ke baad app dobara kholo — crash nahi
  hona chahiye.

## 2. Keys (user apni keys daalega, Settings me)
- Apni free Gemini key, Supabase anon key, (chaaho to Groq key).
- Keys kabhi chat me mat bhejo, sirf app ke Settings field me.

## 3. Har screen ka test
- Chat: text sawaal + Mic sawaal + brain jawab + TTS sunai de.
- Yaadein: add/edit/delete + Export (share) + Sab delete (confirm dialog).
- Scan: photo lo, save ho, text-read bol kar bataye.
- Tools: search, mausam, currency, dictionary — ek-ek karke.
- SOS: 3 contacts save, SOS confirm dialog, dialer/SMS draft khule.
- Face: presence check (photo save/send nahi hona chahiye).
- Docs: add/delete, sirf phone par.
- Connect: Auto Call + Auto SMS (ek confirmation ke baad hi).
- Privacy/Terms: dono screens khulen, text padha ja sake.
- Auto: Accessibility ON karke click-by-text / type / scroll / back.
- Routines: ek chhoti routine banao, confirmation ke baad chale;
  call/SMS step skip ho + spoken note aaye.
- Live Screen: permission dialog -> "Is screen par kya hai?" jawab.
- Continuous: Chalu -> 10 sec interval par summary bole, 10 frame
  par khud band; Roko dabate hi band + projection indicator hat jaye.
- Gaadi: Gaadi mode ON -> call-announce ON; Mic button; OFF par
  call-announce user-setting waisa hi rahe.
- Talk limit: Free plan par 600 sec ke baad brain jawab roke aur
  Pro ka message bole (ye test lamba hai — bol kar batana).

## 4. Licence
- Settings > Licence check karo: anon key set ho to plan dikhe
  (free/pro). Key na ho to free default, app chale.

## 5. Dialer call-talk (ALAG decision — pehle user bataye)
- Flora ko default dialer banana hoga (Settings > Apps > Default
  apps > Phone app > Flora). Tabhi call utha kar baat ka test hoga.
- Bina is role ke call-talk ka daava nahi karna.

## 6. Backup restore test
- APK + docs ka backup lekar dobara restore karke dikhana hai ki
  backup se kaam chalta hai (SHA verify ke saath).

## 7. Fail mile to
- Kaun si screen, kya dabane par, kya error/message aaya — teeno
  likho. Screenshot ho to bhejo. Fix ke baad wahi step dobara test
  hoga, poora plan dobara nahi.
