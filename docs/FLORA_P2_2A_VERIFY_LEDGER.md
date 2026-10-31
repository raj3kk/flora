# Flora Part 2 — 2A Verify Ledger (implementer, 2026-10-31)

> Status: CODE-SIDE VERIFY ONLY. Ye doc koi live state declare nahi karta.
> Dashboard/SQL/Vercel wale hisse generic agent se verify NAHI hue hain —
> wo parent-delegation me hain, yahan FAKE verify nahi hai.
> ZP-B1–ZP-B5 ko CLOSED mat mano jab tak ZP-B5 live gates dobara verify na hon.

## 1. Live tip (GitHub API se fresh, is round me)

- Repo: raj3kk/flora, branch main, tip `c26a9ac4`
  ("M1 browser-open wiring: ScanScreen onOpenUrl -> BrowserScreen via BrowserNav").
- Tree: 473 entries, truncated: false.
- TIP-MOVE NOTE (is round me): verification shuru hui tip `c26a9ac4` par;
  docs likhte waqt tip `c43ff68d` par move hua (Part 1 team ka push:
  REG-F9 residuals + GAP-M8 watchlist export). Push se pehle saare Part 2
  evidence files naye tip se dobara check kiye gaye — sab MATCH (koi drift
  nahi, Part 1 push ne Part 2 files ko nahi chhua). Isliye ye ledger dono
  tip par lagu hai; ledger ka commit SHA ab `c43ff68d` wali push ke baad
  wali commit hogi.
- Local snapshot se drift check (12 files, tip content vs local):
  `supabase/functions/{licence-verify, stt-proxy, tts-proxy, brain-proxy,
  gmail-proxy, send-push}/index.ts`, `app/build.gradle.kts`,
  `web/{pricing, pay, download, index}.html`, `web/updates/version.json`,
  `app/src/main/java/com/flora/app/data/LicenceClient.kt`,
  `app/src/main/java/com/flora/app/tools/WebsitePublisher.kt` —
  SAB MATCH (koi drift nahi). In files par kaam fresh content se hua hai.

## 2. Send-push tip-landed confirm (P2-01 / P2-09 code side)

- `supabase/functions/send-push/index.ts` tip par maujood hai.
- Gate: bina auth header ke 401; success shape `{ ok: true, message_id }`
  (FCM response ka `name` field se liya jata hai; device token kabhi log nahi).
- Live E2E + panel test (ZP-F13) phone/parent-delegation me hai — yahan
  sirf code-landed confirm hai, live-send ka daava nahi.

## 3. Function versions (tip se, live deploy state NAHI)

| Function | Tip state |
|---|---|
| licence-verify | JWT gate: 401 sirf 2 jagah (neeche §4) |
| stt-proxy | Counting code maujood: successful transcription par sirf Free users, `increment_usage` RPC, response me `counted_seconds`, fail-open (count fail ho to bhi transcript milta hai); sirf is proxy path ki counting hai, app ke system-recognizer path ki nahi |
| tts-proxy | v3 header + LFMS licence gate; Deepgram speak model `aura-asteria-en` |
| brain-proxy | v5 licence gate; order Gemini first, Groq fallback; **OpenRouter leg tip par NAHI hai (grep count 0)** — ye P2-14 ka kaam hai, yahan pehle se maujood hone ka daava nahi |
| gmail-proxy | 501 draft only, kabhi deploy nahi (P2-11) |
| home-sync | Repo-only; live 404 ka purana record barkaraar (deploy pending) |

## 4. P2-13 code-path re-confirm (licence-verify 401)

`supabase/functions/licence-verify/index.ts` me 401 sirf 2 jagah se aata hai:

1. `Authorization: Bearer …` header na ho → 401.
2. Anon client se `auth.getUser` fail ho (ya user na mile) → 401.

User-id mismatch 403 hai, licence state 200 + fields hai, DB error 500 hai.
Isliye "authed 401" ka matlab: probe ne valid token bheja hi nahi, ya token
expired / wrong-project / anon-key tha. Settle sirf fresh-JWT test se hoga
(procedure: `FLORA_P2_2B_2C_DESIGNS.md` §6) — live test parent-delegation me.

## 5. Deployment ledger DRAFT (code-side fields only)

- Commit SHA: `58631cd0` (is ledger wali Part 2 implementer commit khud).
  `c26a9ac4` sirf tip-at-writing tha (doc likhte waqt ka tip) — usko is
  ledger ka apna SHA mat mano; upar §1 ka TIP-MOVE NOTE bhi isi hisaab
  se padho.
- Function versions: upar §3 wali tip states (file header versions).
- CI run IDs: is round ke push ke baad wale run id yahan nahi — push ke
  baad CI verify karke coordinator ko report me diye jayenge.
- **KHAALI (parent-delegation, kabhi guess nahi):** Vercel deployment ID;
  live function list + live versions; RLS baaki 5 tables (10 verified ka
  purana record hai, 15-table exit criterion baaki hai); ZP-B5 live-gate
  results (OPTIONS 200 + ACAO, home-sync 403/200, revoke/reveal 400,
  licence-key list, brain 503/502, usage RPC increment, send-push 401 +
  panel ok/message_id, duplicate-UTR warning, signup no-checkbox block,
  admin audit bina RLS error).

## 6. Parent-delegation list (2A)

1. Supabase dashboard session: RLS baaki 5 tables + live function list/versions.
2. Vercel dashboard: deployment ID + live deploy state (deploy#2 disputed
   state ka settle isi se hoga).
3. ZP-B5 live gates dobara chalana (fresh session me).
4. ZP-K7 clarification (D-15) agle asli server push ke saath ride karega —
   solo push nahi (P2-12 doc me bhi darj hai).

## 7. Stale-note flag (edit NAHI kiya gaya — gated hai)

- `web/updates/version.json` ka `notes` text abhi bhi kehta hai GitHub
  Releases "private-repo ki wajah se no-auth channel nahi" — repo ab PUBLIC
  hai, isliye ye note stale hai. File ko is round me CHHUA NAHI gaya hai
  (apkUrl/sha256 2D-gated hain); notes update bhi version.json fill ke
  saath hi hoga, alag se nahi.
