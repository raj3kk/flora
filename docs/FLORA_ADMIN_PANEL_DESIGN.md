# Flora Admin Panel — design (user order 2026-10-28)

User ka order: admin panel se saara kuch operate ho — website
par user account banaye, licence le, buy kare; APK download
karke chalane wale user ke paas koi secret na ho. Audit har
cheez ka ho.

## Seedha sach: secret ka kya hoga, kya nahi
- APK me kabhi nahi jayega: service_role key, Firebase admin
  SDK JSON, Gemini/Groq/Tavily jaise provider keys, koi bhi
  private key. Ye sirf server par rahenge (Supabase Edge
  Function secrets / dashboard).
- APK me jo hoga, wo secret NAHI hai: Supabase publishable
  (anon) key aur Firebase google-services.json — ye dono
  client config hain, Supabase/Firebase ka design hi ye hai
  ki ye app me hote hain; data ki suraksha RLS rules se hoti
  hai, in keys ko chhupane se nahi. Inko "secret" samajh kar
  chhupana galat design hoga.
- Admin panel se secret VALUES kabhi dikhengi nahi, kabhi
  edit nahi hongi. Panel sirf ye batayega: kaunsa server
  secret present hai / missing hai. Secret set/rotate karna
  sirf Supabase dashboard se hoga (owner-only) — ye platform
  ki limit hai, isko admin UI se bypass karna suraksha ka
  risk hoga, isliye nahi kiya jayega.

## Admin kaun hai (auth)
- Website par Supabase Auth se signup/login hoga.
- Pehla admin owner khud banayega: website par account
  banao, phir Supabase dashboard/SQL se us user ko admin
  flag do (ek baar ka owner kaam). Koi bhi user khud ko
  admin nahi bana sakta — self-promotion ka raasta nahi
  hoga, RLS se roka jayega.
- Admin panel ke har action par audit row banegi: kaun
  admin, kya kiya, kab, pehle kya tha / baad me kya hua.
  Audit table append-only hogi (admin bhi purani row edit/
  delete nahi kar sakta).

## Admin panel ke sections (Phase A — pehle ye banega)
1. Users & Licences: user list/search, plan (free/pro),
   status, device, licence activate/revoke.
2. Payments / UTR approvals: pending UTR submissions ki
   list, amount + note + UTR match karke approve/reject.
   Approve hote hi licence activate hogi. Reject par reason
   likhna hoga. Yehi money gate hai — koi auto-charge nahi,
   koi payment gateway nahi (gateway ke liye alag approval
   chahiye, abhi order nahi hai).
3. Usage: har user ka daily talk usage, free 600 sec/day
   limit ke against.
4. Plans: free/pro details dekhna; price badalna owner-gated
   hoga (admin propose karega, owner confirm karega — price
   change bina confirmation ke nahi).
5. Secrets status: sirf present/missing list, koi value nahi.
6. Audit log: saare admin actions ki list.

## Website user flow (Phase A ke saath)
- /signup → /login → /pricing (₹199/month Pro) →
  /pay: UPI deep link + UTR form (amount, UTR, note) →
  admin approval → /account par licence status.
- App me user apna user_id se licence check karega
  (licence-verify pehle se live hai).

## Phase B — brain proxy (keys ko server par le jana)
- Aaj app Gemini/Groq key user ke Settings se leta hai.
  Admin-operated keys ke liye ek server proxy Edge Function
  banegi: app prompt proxy ko bhejega, proxy admin ki keys
  (Edge secrets) se Gemini/Groq ko call karega, jawab wapas
  dega. Tab jaakar provider keys poori tarah APK se bahar
  hongi.
- Phase B Phase A ke baad hai, kyunki usme naya server
  endpoint + app ke BrainClient ka raasta badalna hai —
  ek saath sab kuch karne se audit kharab hoga.

## Tables (naye, Phase A me)
- admin_audit (admin_user_id, action, target_user_id,
  before_json, after_json, created_at) — append-only, RLS:
  sirf admin padh sakta hai.
- payment_submissions (user_id, amount_paisa, utr, note,
  status: pending/approved/rejected, reviewed_by,
  reviewed_at, reject_reason) — user sirf apni row dekh
  sakta hai, admin sab dekh sakta hai.
- users table me admin flag: is_admin boolean, default
  false, sirf service_role/dashboard se set hoga.

## Abhi kya chal raha hai (2026-10-28)
- Browser task: Supabase dashboard se publishable key lena,
  push_tokens table banana, FIREBASE_SERVICE_ACCOUNT_JSON
  secret set karna — result aane par anon key ko app ka
  default banaya jayega (Settings me user ki key ho to wahi
  pehle chalegi).
- Admin panel ka code abhi likha nahi gaya hai — ye design
  uska base hai. Code Phase A ke order me likha jayega.
