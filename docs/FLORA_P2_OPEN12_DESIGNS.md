# Flora Part 2 — OPEN-12 Design Options (P2-02, D-05 ke liye)

> Status: DESIGN OPTIONS ONLY. Koi item yahan se close nahi hota, koi
> implementation decision yahan nahi liya gaya hai. Har item ka faisla
> D-05 batch me aayega; tab tak sab HELD hain. Koi force-close nahi.
> Note: STT counting commit S1-6 ko close nahi karta (alag path hai);
> app-link JWT fix S2-3/S3-3 ko close nahi karta (unlinked path abhi bhi hai);
> ZP-A3/S1-26 closed hai, renewal branch D-22 me hai — yahan dobara nahi kholega.

## S1-5 — 15-day rule

- Sawaal: kisi time-bound rule (jaise link/approval ki 15-din ki khidki)
  khatam hone par kya ho — apne aap aage badhe ya ruk kar poochhe?
- Vikalp (a) Auto-transfer: expiry par state apne aap agle step par jaye.
  Fayda: user ko dobara action nahi; nuksaan: galat state par auto-move ka
  risk, audit me "kisne badla" ka sawaal.
- Vikalp (b) Confirmation-gated: expiry par state HOLD ho, admin/user
  confirmation ke baad hi aage badhe. Fayda: suraksha + audit saaf;
  nuksaan: pending queue badh sakti hai.
- Alternate: expiry se pehle reminder + expiry par hold (b ka halka roop).
- Decision needed: D-05 (S1-5).

## S1-6 — Gate increment reporting reliability

- Sawaal: licence/usage gate ke increment ki report kitni bharosemand ho —
  client report par bharosa ya server-side ginati?
- Tip state: stt-proxy par server-side counting code hai, par wo SIRF proxy
  path ki hai (successful transcription, sirf Free, fail-open). Ye S1-6 ko
  close nahi karta — gate increment ka doosra path (app/main-flow) alag hai.
- Vikalp (a) Server-side source-of-truth: har increment server par RPC se,
  client sirf display kare. Fayda: chhedchhad-proof; nuksaan: har call par
  server dependency.
- Vikalp (b) Client-report + server-reconcile: client report kare, server
  samay-samay par milaan kare. Fayda: offline/weak-network me kaam chale;
  nuksaan: reconcile logic + mismatch handling chahiye.
- Decision needed: D-05 (S1-6). D-24 (main-flow counting route) aur D-30
  (STT seconds Free counter me shamil ya nahi) se juda hai — teeno alag
  faisle hain, ek ko doosre ka faisla mat mano.
- Decisions-trace note (fixer, 2026-10-31): D-24 isi S1-6 link se trace
  hota hai — main-flow counting route ka faisla; D-05 (S1-6 design) aur
  D-30 (STT seconds) se alag faisla hai, koi decision yahan invent nahi
  hua hai.

## S1-17 — TTS model

- Sawaal: TTS ke liye kaunsa model/voice profile default ho?
- Tip state: tts-proxy v3 me Deepgram speak model `aura-asteria-en` hai.
- Vikalp (a) Maujooda model hi rakho (aura-asteria-en) jab tak phone par
  voice-quality test na ho. Fayda: koi naya risk nahi; nuksaan: behtar
  Hindi/English voice ka option khula rehta hai.
- Vikalp (b) Model ko provider decision (D-21) ke saath badlo — pehle
  phone test, phir model lock. Fayda: evidence-based; nuksaan: ek aur gate.
- Decision needed: D-05 (S1-17) + D-21 (provider side).

## S1-27 — device_id ALTER

- Sawaal: licences/device binding me `device_id` column ki nullability /
  constraint kaise handle ho (live TEXT NOT NULL se takraav ka dar hai)?
- Vikalp (a) ALTER session: dashboard se ek baar `ALTER … DROP NOT NULL`
  (ya jo live schema kahe), phir code uske mutabiq. Fayda: saaf schema;
  nuksaan: live ALTER ek irreversible-ish step hai — sirf parent session me,
  pehle live schema confirm karke.
- Vikalp (b) Nullable handling bina ALTER ke: code NULL device_id ko
  website-path (unlinked) case ki tarah treat kare, schema na chhede.
  Fayda: live schema ko haath nahi; nuksaan: do raaste code me rehte hain.
- Alternate: pehle live schema read-only check (parent-delegation), phir
  (a) ya (b) me se ek chuno — guess par ALTER kabhi nahi.
- Decision needed: D-05 (S1-27).

## S1-30 — Rate-limit DB store

- Sawaal: rate limit counters kahan rahen — memory me ya database me?
- Vikalp (a) DB store: har function instance ek hi counter dekhe; multi-
  instance deploy par bhi limit sahi lage. Fayda: sahi; nuksaan: har call
  par DB write ka kharch.
- Vikalp (b) In-memory + loose limit: halka, par instance restart par
  counter reset + instance-wise alag limit. Fayda: sasta; nuksaan: limit
  ko dodge karna aasan.
- Alternate: DB store sirf licence-key level par (P3-03 general rate limit
  se alag item hai — usse confuse mat karo).
- Decision needed: D-05 (S1-30).

## S2-3 — Unlinked ownership proof

- Sawaal: bina link kiye hue user apni khareed/licence ka malik hone ka
  saboot kaise de (app-link JWT fix isko close nahi karta — unlinked path
  abhi bhi khula hai)?
- Vikalp (a) Proof-before-link: UTR + email + khareed ka waqt — teeno
  milne par hi link ho. Fayda: hijack mushkil; nuksaan: user ko details
  yaad rakhni hongi.
- Vikalp (b) Link-then-verify: pehle link, phir admin verify kare, tabhi
  licence active ho. Fayda: user flow aasan; nuksaan: verify hone tak
  aadha state rehta hai.
- Decision needed: D-05 (S2-3).

## S3-2 — Device binding

- Sawaal: licence ek device se bandhe ya kayi devices par chale?
- Vikalp (a) Strict binding: ek licence = ek device_id; device badle to
  transfer/revoke flow se. Fayda: sharing rok; nuksaan: user ka phone
  badalne par friction.
- Vikalp (b) Multi-device allowance: seemit devices (jaise 2) par licence
  chale, har device alag row se. Fayda: user-friendly; nuksaan: sharing
  ka darwaza thoda khulta hai.
- Decision needed: D-05 (S3-2).

## S3-3 — Usage-count JWT-mandatory

- Sawaal: usage-count function JWT ke bina chale ya JWT lazmi ho?
- Vikalp (a) JWT-mandatory: bina valid JWT ke usage-count 401 de. Fayda:
  farzi counting namumkin; nuksaan: unlinked user ka usage count nahi hoga.
- Vikalp (b) Anon-key fallback: unlinked path par anon key se seemit
  counting. Fayda: unlinked user bhi count ho; nuksaan: farzi counting ka
  risk — isliye (b) ke saath rate limit (S1-30) lazmi hai.
- Decision needed: D-05 (S3-3). App-link JWT fix isko close nahi karta.

## S3-4 — Origin allowlist

- Sawaal: functions par CORS allowlist me kaunse hosts hon?
- Tip state: brain-proxy comment me darj hai — "S3-4 allowlist HELD — host
  list parent se aane par badlega". Kai functions abhi `*` (wildcard) par hain.
- Vikalp (a) Explicit list: sirf website host + app origins; baaki block.
  Fayda: suraksha; nuksaan: host list ke bina kuch nahi badlega.
- Vikalp (b) Wildcard barkaraar + document karo ki ye jaanboojh kar hai
  (public read-only endpoints ke liye). Fayda: koi break nahi; nuksaan:
  suraksha audit me kamzor.
- Host list GUESS NAHI hogi — parent se aayegi. Tab tak HELD.
- Decision needed: D-05 (S3-4).

## S4-3 — Supersede-fail direction

- Sawaal: licence supersede (purani hata, nayi lagao) beech me fail ho to
  kaunsi direction me wapas jayein — purani wapas lao ya nayi ko poora karo?
- Vikalp (a) Auto-revert: fail par purani licence wapas active + audit.
  Fayda: user kabhi bina licence ke nahi rehta; nuksaan: revert logic +
  race handling chahiye.
- Vikalp (b) Single RPC: poora supersede ek atomic transaction me ho —
  fail ho to kuch na badle. Fayda: aadha state kabhi nahi; nuksaan: RPC
  design + migration chahiye.
- Decision needed: D-05 (S4-3).

## S6-1 — LFMS (licence feature/flag management system)

- Sawaal: licence gate par jab feature/flag state saaf na ho to response
  kya ho — sakht error ya chetawani ke saath chalne do?
- Vikalp (a) 400/404: galat/missing state par saaf error, gate band.
  Fayda: galat state chhupti nahi; nuksaan: user ko error dikhega.
- Vikalp (b) Dead-key warning: gate chalne do, warning log + admin flag
  lagao. Fayda: user kaam nahi rukta; nuksaan: galat state lambi chal
  sakti hai.
- Decision needed: D-05 (S6-1).

## S6-2 — Fail-open vs fail-closed

- Sawaal: licence/usage gate ka server/database fail ho to darwaza khula
  rahe (fail-open) ya band (fail-closed)?
- Vikalp (a) Fail-closed: gate fail = access band. Fayda: suraksha; nuksaan:
  server down hone par paying user bhi ruk jayega.
- Vikalp (b) Documented fail-open: gate fail = seemit access + saaf message
  + audit log (jaise stt-proxy counting me fail-open hai — counting kabhi
  transcript nahi rokti). Fayda: user kaam nahi rukta; nuksaan: fail ke
  dauraan limit enforce nahi hoti — isliye fail-open hamesha DOCUMENTED
  hona chahiye, chhupke se nahi.
- Decision needed: D-05 (S6-2). Har gate par alag faisla ho sakta hai —
  ek gate ka fail-open doosre gate par lagu mat mano.
