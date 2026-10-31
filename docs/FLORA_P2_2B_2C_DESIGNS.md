# Flora Part 2 — 2B/2C Designs, Procedures, Code-State (implementer, 2026-10-31)

> Status: DESIGNS + PROCEDURES + CODE-STATE ONLY. Koi live step yahan se
> nahi chala hai, koi deploy nahi hua hai, koi DB change nahi hua hai.
> Jo live/phone/user par gated hai, wo parent-delegation me darj hai.

## §1. P2-04 — Command path (design draft, implementation NAHI)

- Maujooda design doc: `docs/FLORA_G10_COMMAND_DESIGN_OPTIONS.md` (tip par)
  — research + options only, koi approval nahi, koi code nahi badalta.
  Is round me us doc ko EDIT nahi kiya gaya hai; ye section sirf window
  note + status jodta hai.
- Code state (tip se): command-dispatch endpoint maujood NAHI hai;
  home-sync registration/sync-only hai; HomeSyncClient ko control ke liye
  reuse karna FORBIDDEN hai (wo registration endpoint hai).
- Prior-owner hole: plan ke mutabiq 403 guard se code-closed hai; command
  path phir bhi absent hai — guard ko command-path ka faisla mat mano.
- SD-P5 window note: command path ka implementation SIRF SD-P5 window me
  hoga — alag post-session deploy, last-hop clear + nayi clearance +
  Pehle-Jaanch ke baad. Isi window me Home server half (P1-25) ka deploy
  slot ALAG track par ride karega (G10 command path se alag track, wahi
  window) — dono ko ek item mat mano.
- Decision needed: D-07 (command-path numbers/last-hop + SD-P5 clearance +
  command path decision — defer, not drop).

## §2. P2-12 — S2-9 partial index (HYPOTHESIS, KABHI RUN NAHI)

- Targeted statement (HYPOTHESIS ke taur par, `core_tables.sql` lines
  199–210 se liya gaya shape — is doc me sirf likha gaya hai, chalaya
  NAHI gaya hai):

```sql
CREATE UNIQUE INDEX IF NOT EXISTS payment_submissions_utr_approved_uidx
  ON public.payment_submissions (utr)
  WHERE status = 'approved';
```

- Pehle zaroori: LIVE me index hai ya nahi, ye parent-delegation check
  (dashboard/SQL) se confirm ho — bina iske kuch mat chalao.
- Scan-first rule (core_tables.sql deploy-check #11 se): index chalane se
  PEHLE live `payment_submissions` me duplicate approved UTRs ka SCAN
  zaroori hai. Duplicate mile to index LOUD FAIL hogi — unhe DELETE karke
  nahi suljhana; kaunsi row asli approved hai, ye ADMIN faisla hai.
- App-level guard pehle se hai: admin-approve approve path par duplicate
  approved UTR check + 400 + audit karta hai; index sirf raced approval
  (23505) ko DB level par rokne ka hardening hai.
- POORA `core_tables.sql` KABHI NAHI chalana: live TEXT NOT NULL se
  takrayega, aur file ke apne header me 11 deploy-checks likhe hain jo
  bina verify kiye poore nahi hote. Sirf targeted statement hi chalega,
  aur wo bhi scan + clearance ke baad.
- ZP-K7 clarification (D-15): agle ASLI server push ke saath ride karega
  (solo push nahi). Is round me koi solo server push nahi hua hai.
- Decision needed: D-15.

## §3. P2-03 — Licence/billing/admin completion (code-side state)

- Tip par maujood functions: licence-verify, usage-count, push-token-sync,
  admin set (admin-approve, admin-list, admin-licences, admin-keys,
  admin-payments, admin-settings, admin-overview, admin-plan-features,
  admin-instructions, admin-flags).
- REG-B2 (licence per-reply round-trip tradeoff): tip doc
  `docs/FLORA_B2_TRADEOFF.md` me darj hai — har gated reply se pehle
  fresh verify; worst-case stall unlinked = 1 post tak, linked + expired
  token = 2 post tak; cache wapas lagana REJECTED (013 ka faisla).
- REG-B5 (cap message/usage display): tip par B5 docs maujood hain
  (`FLORA_B5_CI_PENDING.md`, `FLORA_B5_MIGRATION_AUDIT.md`).
- ZP-G14 grep-confirm — VERIFY-ONLY, is round me verify hua:
  `LicenceClient.kt` (tip) me "Transfer" ke 0 hits; local file tip se match.
  App files ko is round me EDIT nahi kiya gaya hai.
- Live steps (parent-delegation, koi bhi yahan se nahi chala):
  DB UPDATE + redeploys; ZP-F7 phone re-link (admin row sirf re-link se
  banegi, sirf wahi row is_admin); ZP-F8/F9 UPI set; ZP-F10 2× ₹199 SIRF
  real UTR evidence se; RD-C-12 provider_keys storage decision; D-10 cap
  number (S1-20); ZP-F11 N4 key-path live test; ZP-F12 N5 D8 gate live test
  (Free <600s → 429); ZP-F17 Phase 7 E2E licence flow.
- Decision needed: D-10, RD-C-12 (D-21 ke saath), D-30 (STT seconds).
- P2-05 status note (fixer, 2026-10-31): website pages HAVE (text) /
  visual + UPI + wording PENDING — plan ke mutabiq; iske liye koi nayi
  file nahi banayi gayi hai, note isi 2C doc me darj hai.

## §4. P2-11 — Gmail lane (spec skeleton draft, deploy NAHI)

- Tip state verify hua: `supabase/functions/gmail-proxy/index.ts` 501
  draft state me hai ("gmail oauth not configured" — koi Google call nahi,
  koi table nahi); tip doc `docs/FLORA_SERVER_GMAIL_DESIGN.md` design +
  draft only kehta hai, deploy nahi hua hai.
- Spec skeleton — poora spec banne ke liye ye sections chahiye (is doc me
  sirf skeleton hai, implementation nahi):
  1. Cloud project + consent screen (kaunsa project, kaunsi screen).
  2. Scope: `gmail.readonly` hi (likhna/bhejna scope me nahi).
  3. Redirect URI (website/app ke mutabiq, guess nahi).
  4. `gmail_tokens` table (columns, RLS, rotation — token storage rule).
  5. Proxy endpoints (inbox read-only, snippet vs full body, rate limit).
  6. App-side option removal gate: sirf tab jab server replacement
     LIVE-verify ho jaye — usse pehle app-side option (email-app snippet
     flow + app-kholna mapping) chhua nahi jayega.
- Decision needed: D-12 + ZP-H17 (full spec). Deploy iske baad hi.

## §5. P2-10 — Live probe procedures (taiyaar, chalaye NAHI)

- Gate: user ka Deepgram security code (ZP-G2) + user JWT — dono ke bina
  probe ka daava nahi.
- ZP-K8 probe (STT counting): user JWT se stt-proxy par successful
  transcription karo → response me `counted_seconds` dekho → usage table
  me us din ka increment dekho (Free user par; Pro par skip by design).
  Pass = counted_seconds > 0 (120s cap ke andar) + usage increment mila.
- ZP-K9 probe (TTS success): user JWT se tts-proxy par TTS call karo →
  audio success dekho. Note: key ke bina 503 → system fallback BY DESIGN
  hai — usko fail mat mano, par success ka daava bhi mat karo.
- Dono probes user code + JWT par gated hain — parent-delegation me.

## §6. P2-13 — Fresh-JWT test procedure (taiyaar, chalaya NAHI)

1. User phone par fresh login karo (naya JWT lo — purana/expired token
   se test mat karo).
2. Us JWT se licence-verify POST karo, body me wahi user_id jo JWT me hai.
3. Expected: 200 + licence fields. 401 aaye to uska matlab token hi valid
   nahi tha (galat project/anon-key/expired) — code-path ka dosh nahi.
4. Key-path confirm (parent session): JWT gate sab paths se pehle hai —
   unlinked key users key path use nahi kar sakte jaisa hai; iska confirm
   bhi parent session se hoga.
- Live test parent-delegation me hai.

## §7. Decisions-trace note (fixer, 2026-10-31) — D-09

- D-09 (WhatsApp Business Cloud API CLOSED): Part 2 me iski relevance
  sirf itni hai ki koi bhi live-send scope re-confirmation isi closure
  ke against hogi; spend approval alag se chahiye. Isse koi Part 2
  live-send step khulta nahi — isliye Part 2 ke liye D-09 note-only hai.
  Koi decision yahan invent nahi hua hai.
