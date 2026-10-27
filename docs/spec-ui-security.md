# Spec — UI + Security (activities, Guardian, Face Lock, SOS)

Source: behaviour only — manifest activities, screenshots (Home/Chat/
Yaadein/Drawer/Setup/Accessibility), V5 video security section, shorts
(Guardian refusal action me). Folder: ui/ (ABHI KHAALI HAI).

## Files (honi chahiye)
- ui/BrowserActivity.kt — in-app browser (scan / web search); intent
  extra se URL, tabs, address bar, banner, cookies ka behaviour.
- ui/FaceLockActivity.kt — face se lock (settings/chat/memories/app),
  har baar / 1 min / 5 min ka option.
- ui/WatchmanActivity.kt — camera on rakhke kaun aaya-gaya, baad me list.
- ui/UnlockActivity.kt — voice se pattern/PIN unlock; PIN sirf encrypted
  on-device, default OFF, user-approved; DISABLE_KEYGUARD flow rebuild
  me excluded hai (evidence mila, scope se bahar rakha gaya).
- ui/ScreenCapturePermissionActivity.kt — screen capture permission UI.
- ui/GuardianSetup.kt — Voice Guardian register: owner "Boss", ≤10
  samples, consistency check, test 94–97% jaisa demo nahi — apna metric,
  suggested threshold 0.86; Owner-only + Away mode; non-owner ko refusal
  ("lock karo" jaise command par mana karna).
- ui/SosManager.kt — SOS: last 5 contacts, location WhatsApp + SMS se,
  pehle number ko 5 baar call phir agla.
- ui/TouchGuard.kt — Touch Guard (soft lock hi; fail par warning +
  dobara poochna, phone ko hard lock karke band nahi karna — hard God
  Mode kabhi nahi banega).

## Behaviour — personas
- Caring/friendly persona hi banega; GF-style ka friendly hissa theek
  hai, romantic/18+ content kabhi nahi. Teen personalities (website):
  friendly, professional, venom-style — alag voice + manner, ek sentence
  me switch; koi bhi persona 18+ line nahi bolega.

## Acceptance (Verify → Test → Audit)
1. Guardian: owner pass, non-owner refuse — dono ka phone test.
2. SOS: test call/message sirf user ke test contacts par, disclosure ke
   saath.
3. Koi crash / hidden exception nahi; hard lock ka koi code path nahi.
4. Koi purani API key code me nahi hai.
5. Test log docs me darj hai.
