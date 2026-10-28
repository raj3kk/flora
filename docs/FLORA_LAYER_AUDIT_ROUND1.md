# Flora full-layer audit — ROUND 1 (2026-10-28, user order: cycle tab tak jab tak 0 na ho)

## Layer map (evidence ke saath)
- APP: 16 screens (CHAT SETTINGS MEMORIES SCAN TOOLS SOS FACE DOCS
  CONNECT PRIVACY TERMS AUTO ROUTINES LIVE DRIVING JOURNAL). Latest
  PASS v2.6 (59,405,561 bytes). TODO/FIXME/coming-soon in app/ = 0.
- BACKEND (Edge Functions, sab live-verified 2026-10-28):
  licence-verify 200, usage-count fixed+verified, send-push auth
  gate verified, admin-approve auth gate verified, push-token-sync
  ok:true verified. Auth endpoint live (bad-creds 400
  invalid_credentials — bina user banaye prove hua).
- DATABASE: tables ka SQL verify browser task se chal raha hai
  (Round 1 report me uska result jodega). Pehle se verified:
  push_tokens, payment_submissions, admin_audit, users.is_admin.
- WEBSITE: 6 pages (index/admin/signup/login/pricing/pay/account
  — account mila ke 7) latest deploy par 200 the; Round 1 fix ke
  baad naya deploy phir 200-check hoga.

## ROUND 1 me mile mismatches (frontend/backend/DB)
1. [FIXED] Terms + website Pro text me live wallpaper /
   conversation mode Pro feature bataye gaye the — ye bane hi
   nahi hain. Fix: sab jagah imaandari se "abhi available nahi,
   baad me aayenge" likha gaya (LegalScreens, index, pricing,
   admin plans section).
2. [FIXED] Admin page ke Users/Usage/Secrets/Audit sections sirf
   heading the. Fix: Audit list wired (admin_audit par admin
   select policy maujood hai); Users/Usage list ke liye server
   function baaki hai (Round 2 item); Secrets by-design sirf
   note hai (values kabhi page par nahi aayengi).
3. [OPEN] settings table: app me ZERO reference — DB table bina
   frontend/backend use ke (dormant). Fix: ya to use karo ya
   schema doc me dormant likho (Round 2).
4. [OPEN] plans table: koi code runtime par isko read nahi karta
   (licence-verify licences.plan_id deta hai; admin-approve
   plan_id='pro' hardcode karta hai). Price badalne par kuch
   nahi badlega — plans section ka text seedha seeds se match
   karna padta hai. Fix: plans ko source-of-truth banao ya
   dormant-config likho (Round 2).
5. [OPEN] Retention claim vs code: Privacy me "30 din" likha hai,
   lekin koi auto-delete enforcement nahi hai (sirf constant +
   text). Fix: Memories/Journal pruning implement karo (Round 2 —
   data delete hai, isliye alag item me careful audit ke saath).
6. [OPEN] Free "15 tools" limit: constant hai, ToolsScreen par
   koi gating nahi (free user ko sab milta hai — zyada milna,
   kam ka jhootha vaada nahi). Fix: gating ya text (Round 2).
7. [OPEN] APP-LINK (sabse bada): website account (Auth uid) vs
   app random UUID alag hain — website Pro app me nahi dikhega.
   Design taiyaar (FLORA_APP_LINK_DESIGN.md). Fix: v2.7+ me
   implement (Round 2, sabse pehle).
8. [OPEN] Brain Phase B: app me Gemini/Groq keys user ko khud
   daalni hain; server-side brain proxy nahi hai (admin keys
   server par nahi). Fix: Phase B proxy (Round 3).
9. [OPEN, phone par hi prove hoga] wake/face/FCM-sync/usage
   runtime, asli push, asli approve — bina phone test + admin
   user ke ye kabhi "verified" nahi kahlaenge.
10. [OPEN, by-design nahi bug] Turso sync_queue: Turso token
    watcher-level par invalid hai; Supabase authoritative hai,
    isliye block nahi hai — bas dormant note.

## Round 1 fix status
- Terms + website strings: FIXED (is round me push hoga).
- Admin Audit list: WIRED (is round me push hoga).
- Baaki 3–8: Round 2/3 items, cycle rukega nahi.
- Issue count abhi 0 NAHI hai — cycle jaari rahega.

## Round 1 verification (2026-10-28 ~10:50 IST)
- Commit f205cf3f pushed; Vercel deploy
  dpl_CuaXuNBDT4856L5pEs1fkNrqssXb READY:
  flora-3ez9xuwhn-webbuilder1.vercel.app par 7/7 pages 200
  (/, admin, pricing, pay, signup, login, account) aur pricing
  par imaandari wala text live confirm hua.
- CI run 40636820057 (v2.7) completed success, artifact
  flora-debug-apk 59,408,672 bytes, expired false. v2.7 = latest
  PASS (v2.8 CI chalne tak).
- DB SQL check (browser task) ka result abhi pending hai.

## DB SQL verification — RESULT AA GAYA (2026-10-28 ~10:44 IST run, read-only)
- 8/8 tables maujood hain: admin_audit, licences,
  payment_submissions, plans, push_tokens, settings, usage,
  users. RLS sab 8 par ON hai.
- Row counts (sirf count, koi personal data nahi padha):
  users 1, plans 2 (seeds), settings 0, usage 0, licences 0,
  push_tokens 1, payment_submissions 0, admin_audit 0.
- Policies sirf 7 hain (admin_audit 2, payment_submissions 4,
  push_tokens 1). licences, plans, settings, usage, users par
  RLS ON hai par KOI policy nahi → anon key se in tables ka
  direct read hamesha khaali/denied aayega. Isliye admin ke
  Users/Usage sections client-side kabhi nahi chalenge — unke
  liye service-role server function hi raasta hai (Round 2
  item confirm ho gaya). App/functions isse affect nahi hote
  (functions service key se chalte hain, live-verified).
- users=1 aur push_tokens=1 wali rows meri test-UUID ki hain
  (push-token-sync live test) — asli user abhi koi nahi hai.
  Test rows ki cleanup Round 2 me (dashboard se delete).

## Round 2 prep (local only, push v2.8 CI PASS ke baad)
- settings/plans dormant resolution (taiyaar): settings table
  ka app me koi use nahi hai aur plans table ko koi code read
  nahi karta — inko schema doc me "dormant config, runtime
  source-of-truth nahi" likha jayega; licence-verify aur
  admin-approve hi source-of-truth rahenge (plan_id + 30 din).
  Koi table delete nahi hogi (destructive nahi).
- Retention pruning design (taiyaar, implement baad me):
  Journal entries me ts hai → 30-din pruning seedha ban sakta
  hai. Memories sirf plain strings hain, unme ts NAHI hai →
  purani memories ki age jaani hi nahi ja sakti; nayi entries
  ke liye ts jodna + sirf timestamp wali prune karna hi
  imaandari wala raasta hai. Privacy text ko isi ke hisaab se
  theek karna hoga (journal 30-din auto, memories manual
  delete + nayi entries par 30-din) — jhootha "sab 30 din"
  vaada nahi rahega.
