# Flora server Gmail (server-OAuth) — design + draft (Phase 1, Item D)

Status: DESIGN + DRAFT ONLY. Function `supabase/functions/gmail-proxy/index.ts`
repo me draft hai, DEPLOY NAHI hua hai, poora implementation claim NAHI hai.

## Aaj ki haalat (pre-audit)

- Server par Gmail naam ka KOI function nahi hai.
- App me Gmail API inbox NAHI hai: CommConsentScreen sirf user ke email
  app se snippets (read-only, consent ke saath) dikhata hai aur khud
  likhta hai ki "poora Gmail API inbox is batch me NAHI hai (uske liye
  alag Gmail OAuth spec + gate chahiye hoga)". AppAutomation me sirf
  `gmail -> com.google.android.gm` package mapping hai (app kholna).
- Isliye "app-side Gmail option" = email-app snippet flow + app-kholna
  mapping — ye is round me CHHUA NAHI GAYA hai.

## GATE (pakka, todna nahi)

App-side Gmail option TABHI hatega jab server-side replacement LIVE verify
ho jaye (deploy + OAuth consent + user-JWT probe + snippet read success).
Is round me hatane ka kaam hi nahi kiya gaya — sirf design/draft +
blocker report hai.

## Design (poora hone par)

- Endpoint: POST `/functions/v1/gmail-proxy`, JWT gate + caller-match
  (body user_id JWT user se match, nahi to 403) — brain-proxy jaisa.
- Actions v1 (READ-ONLY): `list` (max 10, inbox) aur `get` (ek message).
  Response me sirf id + snippet + from/subject/date — poora body,
  attachment aur token kabhi nahi (snippet-only, aaj ke consent
  screen ke barabar).
- SEND v1 me NAHI hai — bhejna alag decision + alag gate hoga.
- Token flow: Google OAuth (authorization code + refresh token),
  refresh token server-side rakha jayega; access token short-lived,
  expiry par refresh endpoint se naya liya jayega. Token kabhi log
  ya response me nahi.
- Key/env: `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`
  (dashboard secrets, repo/app me kabhi nahi).

## Pending decisions (USER se, force nahi honge)

1. Google OAuth client: kaunsa Google Cloud project, OAuth consent
   screen (app name, scope `gmail.readonly`), redirect URI.
2. Consent UX: user consent kahan dega (website login server ya app
   browser) — website login server (flora-henna-alpha.vercel.app)
   maujooda route hai, par decision user ka hai.
3. Token storage: `gmail_tokens` table ka schema + at-rest protection
   (service-role-only read; encryption decision pending) — table abhi
   nahi bani hai, isliye draft 501 deta hai.
4. Scope approval: `gmail.readonly` se aage koi scope nahi (send nahi).

## Draft me kya hai, kya nahi

- Hai: CORS, JWT gate, caller-match, action validation, OAuth-config
  gate (env/table na ho to 501, koi Google call nahi).
- Nahi hai: token refresh ka live code, Gmail API calls, app-side
  wiring, app-side option hatana — ye sab decisions + live verify
  ke baad, alag item me honge.
