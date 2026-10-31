# CEO D-19 Fold Record — Item 28 (ZP-A3 batch) + Notch P2-1…P2-6

**Fold, chupchaap drop nahi.** Item 28 = zero-plan G11 (Item 28 M1:
B6 P2-1/2/3) + remaining-list G17 (Item 17 N1: Notch P2-1…P2-6).
Source docs: `docs/FLORA_A3_PREAUDIT.md` §5 (N1) + §6 (M1 addendum,
coordinator decision: FOLD, alag batch nahi) aur
`docs/FLORA_ZERO_REMAINING_PLAN.md:351,357`. A3 batch LANDED hai;
is record me har point ki LIVE TIP par per-point haalat darj hai
(local copy tip-identical verify ki gayi hai — isliye neeche lines
tip ki hain). Koi point chhupaya nahi gaya hai; jo abhi bhi khula
hai wo "CARRIED" ke roop me saaf likha hai.

## N1 — Notch P2-1…P2-6 (A3 preaudit §5)

- **N1-P2-1 Dual-recognizer race — FIXED (folded).**
  `MainActivity.kt:160` (aur `:57` onSpeechDone) me ab
  `if (continuousWanted && !NotchState.interrupted) startContinuous()`
  hai — preaudit ka fix direction (interrupted par SKIP) code me
  maujood hai. Phone par duplicate-turn check abhi bhi phone-gated
  hai (is record se phone daava nahi hai).
- **N1-P2-2 Guidance-vs-interrupt ordering — FIXED, candidate (a)
  (folded).** `ui/FloraApp.kt:1385-1395` me interrupt pending ho
  to guidance ABHI nahi boli jati (comment me candidate (a) darj
  hai); pending agle bina-interrupt speech-done par bolega;
  normal path waisa hi hai.
- **N1-P2-3 fixedTts handle edges — FIXED (folded).**
  `assistant/NeuralSpeaker.kt:148,154` (onDone par
  `if (fixedTts === tts) fixedTts = null`) aur `:168-169`
  (speakFixed par purana handle clear) — onDone-clear + overwrite
  guard dono maujood hain.
- **N1-P2-4 Island "logging" — FOLDED AS ACCEPTED NIT (koi code
  change nahi, decision se).** `voice/NotchState.kt:134` `setState`
  me koi Logcat line nahi hai (Android-import-free by design);
  transitions UI-observable hain (setState → listener →
  postInvalidate chain). Preaudit ke option (a) (nit accept) ke
  mutabiq ye point code change ke bina fold hua hai — drop nahi.
- **N1-P2-5 Audit-log imprecisions — DOCS-DONE (folded).**
  `docs/FLORA_PER_ITEM_AUDIT_LOG.md:1841` par append-only sudhaar
  note maujood hai (chaaron sub-points: import-check note,
  declaration-order 193→194, FINAL-only commit wording,
  Color() phrase unverified) — tip blob se verify kiya gaya hai.
  Purani entries rewrite nahi ki gayi hain.
- **N1-P2-6 ChatMemoryStore race — INFO-ONLY, FIX NAHI (folded).**
  `data/ChatMemoryStore.kt` ka `addTurn` abhi bhi
  loadAll → mutate → save hai; `voice/BackgroundMicService.kt:576-577`
  me do addTurn calls waisi hi hain. Preaudit ke mutabiq is par
  koi code change nahi aaya hai — ye jaanboojh kar info-only hai,
  drop nahi.

## M1 — B6 P2-1/2/3 (A3 preaudit §6, Item 28 M1)

- **M1-1 Post-validation write failure partial state — FOLDED
  (failure-stop variant).** `backup/FloraBackupManager.kt` ka
  `writeAll` projectMemory/projects fail par `BackupFailure`
  throw karta hai ("restore adhoora raha, dobara koshish karo") —
  aage ke writes rukte hain aur success ka jhootha daava nahi
  hota. Saaf seema: fail se PEHLE replace ho chuke sections
  (chats/memories/journal/docs) ka partial state abhi bhi
  possible hai; preaudit ke mutabiq recovery = intact blob se
  dobara restore. Atomic temp-staging variant implement NAHI hua
  hai — ye CARRIED nuance hai, drop nahi.
- **M1-2 Audit-log entry patli — DOCS-DONE (folded).**
  `docs/FLORA_PER_ITEM_AUDIT_LOG.md:1844` par full pre-audit
  checklist shape me expansion maujood hai (R-B6-1…R-B6-11
  enumeration, do pre-push import fixes, declaration-order lines,
  timing-honesty note) — tip blob se verify kiya gaya hai.
- **M1-3 ensureFolder stale cached ID — FIXED (folded).**
  `backup/DriveClient.kt` ka `ensureFolder` ab cached ID par
  `files.get` existence + trashed re-check karta hai (comment me
  "A3 M1-3" darj hai); stale/missing par find-or-create + cache
  update; "Drive hatao" manual reset barkaraar hai.

## Fold summary

9/9 points explicitly recorded: fixed 5 (P2-1, P2-2, P2-3, M1-1
failure-stop, M1-3), docs-done 2 (P2-5, M1-2), accepted-nit 1
(P2-4), info-only 1 (P2-6). Koi point chupchaap drop nahi hua.
CARRIED (agle cycle/coordinator ke liye): M1-1 atomic staging
variant; phone-level checks (P2-1 duplicate-turn, P2-2 FINAL
sequence) phone-gated hain.
