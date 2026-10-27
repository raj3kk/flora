# Spec — Termux + Wallpaper (+ Markets/Marketing/Documents/Website tools)

Source: behaviour only — V5 video agentic demo (Termux HTML + Python
server 127.0.0.1:880), manifest Termux components, website suites.
Folders: termux/, wallpaper/ (DONO ABHI KHAALI HAIN).

## Files (honi chahiye)
- termux/TermuxPermissionActivity.kt — Termux command allow karna,
  user ki saaf permission ke baad hi.
- termux/TermuxResultReceiver.kt — Termux command ka result wapas lena.
- termux/TermuxRunner.kt — HTML file + Python server chalana, Chrome
  me 127.0.0.1:880 dikhana (video demo ka behaviour, code dobara likhna).
- wallpaper/MayaWallpaperService.kt — live wallpaper, preview dekhke
  set karna; 3D/orb look naya design hoga, creator ka character asset
  copy nahi.
- Tools (ToolRouter se judenge, alag files banengi jab module shuru ho):
  Markets (stock analysis, watchlist — price accuracy unverified, source
  + time ke saath batana, "not financial advice" note), Marketing
  (profile/content/calendar/lead/follow-up — sirf user-approved),
  Documents (Word/Excel/PowerPoint/PDF banana, voice se), Website/Coding
  agent (plan → code → fix jab tak chale → Chrome me kholna → deploy
  ke liye user ka GitHub access + alag approval), Study/Whiteboard
  (board par draw karke samjhana), Maya Home (alag hardware track,
  safety gates: electrician, MCB OFF, 5A/channel, <10A total).

## Acceptance (Verify → Test → Audit)
1. Termux demo: HTML + server + Chrome view, phone test me dikhe.
2. Wallpaper preview + set, crash nahi.
3. Har tool ka source + time batana (knowledge me guess nahi); medical
   = general info only, diagnosis nahi.
4. Koi purani API key code me nahi hai.
5. Test log docs me darj hai.
