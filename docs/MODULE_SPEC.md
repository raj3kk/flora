# Rebuild Module Spec — Maya v5.2.44 (videos + manifest se, jadx verification pending)

Har module ke liye: kaam kya hai, kahan se pata chala, acceptance test kya hoga.
Jadx ke baad har module ke saamne "verified from decompiled code" ya "behaviour only"
likhna hai — abhi sab "behaviour only" hain.

## App shell
- MainActivity: home screen, tabs Home / Scan / Mic / Memories / Chat (video se).
  Test: fresh install par wizard → permissions → home khulta hai, crash nahi.
- MayaApplication: foreground service + WorkManager init, Room/DataStore init.
  Test: boot ke baad service dobara start hoti hai (BootReceiver).

## Assistant
- MayaAssistActivity: default assistant (power-button long-press) ka UI.
- MayaVoiceInteractionService / SessionService / RecognitionService: "Hey Maya"
  sunna, wake word TFLite se detect karna, phir STT → LLM → TTS.
  Test: shant kamre me 10 baar wake word bolo, kam se kam 9 baar jage;
  TV shor me bhi test karo, false wake note karo.
- MayaForegroundService: mic + camera + data-sync foreground types ke saath chalna,
  notification ke saath, battery optimisation off maangna.

## Automation
- MayaAccessibilityService: screen padhna + tap karna, SIRF user-approved kaam.
  Test: har action se pehle preview dikhe, "ruko" dabane par turant ruke.
- MayaNotificationListener: naye message ki alert padhke sunana (on/off toggle).

## Security
- FaceLockActivity: face se lock (settings/chat/memories/app), har baar / 1 min / 5 min.
- WatchmanActivity: camera on rakhke kaun aaya-gaya, baad me list.
- UnlockActivity: voice se pattern/PIN unlock, swipe length + dot position tuning.
- Voice/face guard: soft lock hi hoga — fail hone par warning + dobara poochna,
  phone ko hard lock karke band nahi karna (God Mode hard lock NAHI banega).

## Communication / schedules
- ReminderReceiver, SocialAlarmReceiver, MarketingAlarmReceiver: time par
  reminder / daily story / marketing post ka kaam (auto-post sirf user-approved
  aur official API se hi, Accessibility se auto-post NAHI).
- PhoneStateReceiver, DrivingOffReceiver: call announce / driving mode me
  preset message (WhatsApp + SMS) — sirf user ke chune hue contacts par.
- MicCaptureReceiver: mic capture ka trigger (wake word ke saath).

## Termux / browser / wallpaper
- TermuxPermissionActivity + TermuxResultReceiver: Termux command allow karna,
  HTML file + Python server chalana, Chrome me 127.0.0.1:880 dikhana (video demo).
- BrowserActivity: in-app browser (scan / web search ke liye).
- MayaWallpaperService: live wallpaper, preview dekhke set karna.

## Data (jadx se confirm karna hai)
- Room entities (anuman, final nahi): memories, chat, settings, licence,
  voice profiles, face profiles, reminders, macros, social schedules.
- DataStore keys (anuman): wake word on/off, theme, orb size, persona, STT/TTS
  choice, battery saver, echo guard, media mode, typing mode.
- Backend (domains pakke, paths jadx se): api.getmaya.cloud, getmaya.online,
  sync.hunterislive.online — endpoint paths abhi unknown hain, guess mat karo.

## Acceptance (har module ke liye)
1. Module akela chalke test pass kare (unit + phone test).
2. Koi crash / hidden exception nahi.
3. Mic off / incognito / delete kaam kare.
4. Koi purani API key code me nahi hai.
5. Module ka test log docs me darj hai.
