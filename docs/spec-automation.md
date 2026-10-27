# Spec — Automation (Accessibility + Notification Listener)

Source: behaviour only — videos/shorts (app kholna, tap/type/swipe,
message padhna/reply), manifest services, website (150+ actions / 17
suites). Folder: automation/ (ABHI KHAALI HAI).

## Files (honi chahiye)
- automation/MayaAccessibilityService.kt — screen padhna + tap/type/swipe/
  scroll; SIRF user-approved kaam, har action se pehle preview, "ruko"
  dabane par turant rukna; visible stop control mandatory.
- automation/MayaNotificationListener.kt — naye message/call ki alert
  padhke sunana; on/off toggle; WhatsApp/Instagram message check + reply
  sirf user ke kehne par (app route, ban risk ka note saath).
- automation/MacroEngine.kt — builtin macros (5, step counts screenshot
  se confirm: 8/8/16/17/14) + user macros; step format: tool + args,
  label lookup ko coordinates par preference; kisi aur app ke andar
  automation sirf user-approved/official route par, warna redesign.
- automation/ToolRouter.kt — LLM se: samajhna → tool chunna → karna →
  report karna; har tool ka fallback (key lane → free lane → backup lane).

## Behaviour — kya karega
- Kisi bhi app ko kholna + usme kaam karna, call uthake baat karna
  (announce, voice pick/reject, speaker baat, hang up), music (YouTube /
  Spotify / Gaana app kholke bajana), phone control (torch, brightness,
  volume, hotspot, silent, lock, settings), browser me kholna + operate
  karna, code likhna + deploy (deploy ke liye user ka GitHub access +
  alag approval), website banana (plan → code → fix jab tak chale).
- Live screen dekhna + us par action (error → fix, form → approval se
  bharna, price/product → judge, game/video → commentary) — sirf jab
  user kahe, sirf us moment ke liye, on-device processing pehle.
- Live camera dekhna (object/text/board/document/scene pehchanna).

## Excluded (kabhi nahi)
- Silent auto-post (WhatsApp/Instagram), silent screen viewing, silent
  call recording, passwords store karna, phone PIN store karne ke alawa
  koi credential store karna. PIN sirf encrypted on-device, default OFF,
  user-approved (2026-10-27 decision).

## Acceptance (Verify → Test → Audit)
1. Har action ka preview + stop control phone test me dikhe.
2. Koi crash / hidden exception nahi.
3. Mic off / incognito / delete kaam kare.
4. Koi purani API key code me nahi hai.
5. Test log docs me darj hai.
