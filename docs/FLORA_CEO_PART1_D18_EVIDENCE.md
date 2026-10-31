# CEO D-18 Evidence — Item 34 O-1 / O-3 (Part 1, 2026-10-31)

**BEHAVIOR CHANGE NAHI.** Ye doc sirf landed Item 34 code se verify
ki gayi evidence hai (CEO faisla: O-1 prospective-only, O-3 prefix
unchanged). Is cycle me Item 34 code me koi badlav nahi kiya gaya
hai — isliye is doc ke liye code push ki zaroorat nahi hai; ye doc
khud hi deliverable hai. Verify base: live tip (D-24 push ke baad
ka tip; neeche lines us waqt ke local=tip code ki hain).

## O-1 — prospective-only (koi retroactive gate change nahi)

**Verdict: CODE PEHLE SE MATCH KARTA HAI.**

- Koi startup migration nahi hai: `data/FloraPreferences.kt:1000`
  `persona()` stored value ko jaisa hai waisa lautata hai (koi
  flip, koi gate-check nahi); startup load sirf load hai
  (FloraApp me `voice.persona = prefs.persona()` — set nahi).
- Naye-install-only gate-card: `data/FloraPreferences.kt:1020`
  `hasStoredPersona()` — comment ke mutabiq purane installs ko
  kabhi nag nahi, "unka stored persona hi rehta hai".
- Gate sirf aage ke requests par lagta hai:
  - Voice: `ui/FloraApp.kt:2842` — age-confirm na ho to
    `ageGatePendingVoice = true` (dialog), bina confirm ke
    romantic ON ka koi raasta nahi.
  - Greeting: `ui/FloraApp.kt:411` — romantic greeting sirf tab
    jab `persona() == "romantic" && ageConfirmed()`.
  - Age flag default false: `FloraPreferences.kt` me
    `AGE_CONFIRMED ?: false`.
- **Nuance (flag, change nahi):** Backup restore par fail-closed
  downgrade maujood hai — `backup/FloraBackupManager.kt:551`
  (romantic + !ageConfirmed → `setPersona("flora")` +
  `restoreRomanticDowngraded`). Ye startup migration NAHI hai
  (restore-time path hai, landed Item 34 ke D-I34-4 faisle ka
  hissa hai) aur is cycle me ise chheda nahi gaya hai. Agar CEO
  ka O-1 is restore path ko bhi cover karta ho to uske liye
  alag explicit faisla chahiye — implementer ne guess par ise
  nahi badla hai.

## O-3 — prefix unchanged

**Verdict: PREFIX UNCHANGED (is cycle me koi badlav nahi).**

- Persona prefix text waisa hi hai: `ui/FloraApp.kt:182-189`
  `personaPrefix()` (flora/saathi/guru/romantic strings) — is
  cycle me chhua hi nahi gaya hai.
- Background-mic path par prefix ki haalat bhi unchanged hai:
  `voice/BackgroundMicService.kt` ka `brain.ask` call sirf
  `transientNote` (battery note) bhejta hai, koi persona prefix
  nahi — ye wahi haalat hai jo register ke G10 me darj thi
  ("prefix code me nahi mila"). D-18 ka "prefix unchanged" isi
  se poora hota hai: na prefix text badla, na background-mic
  path par prefix joda/hataya gaya hai.
- **Carried nuance (flag, change nahi):** D-I34-2 me approved
  `BrainClient.ask()` ke andar persona move abhi bhi implemented
  nahi hai (`assistant/BrainClient.kt:60` ka `ask()` koi persona
  resolve nahi karta) — ye pehle se maujood gap hai, is cycle
  ka deviation nahi hai, aur D-18 ke "unchanged" order se ise
  is cycle me implement karna galat hota.
