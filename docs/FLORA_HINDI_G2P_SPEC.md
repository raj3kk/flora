# FLORA Hindi G2P Spec — Clean-Room (v0.2, 2026-10-30)

**Status:** IMPLEMENTED — Lane C implementation landed hai (KokoroOrtEngine +
NeuralVoiceAttach + HindiG2P, is spec ke mutabiq). Ye spec ab implementation
ka record + audit reference hai; rule change sirf §4.3 process se hoga.

**Route:** CLEAN-ROOM (user decision 2026-10-30 ~00:35 IST). GPL-data route CLOSED hai —
espeak-ng ka koi data/code/file is spec me nahi hai aur APK me kabhi ship nahi hoga
(declaration §8 me hai).

**Target:** Devanagari text → Kokoro-82M phoneme tokens → onnxruntime-android (opset 17),
voice hf_alpha, 24 kHz, on-device. Text → phonemes → model → audio; koi server call nahi.

**Base facts (pehle se verified, is spec ke assumptions):**
- Kokoro vocab = hexgrad Kokoro-TTS `app.py` `get_vocab()` (commit eff6dd2d):
  `"$"` (index 0) + 16 punctuation + 52 letters + 109 IPA = **178 entries**.
- Tokenize: unknown chars silently drop hote hain (`tokenize` me `VOCAB.get` + None filter).
- Wrap: `[[0, *tokens, 0]]`; style row = **unwrapped** token list ki length
  (`ref_s = VOICES[voice][len(tokens)]` wrap se PEHLE compute hota hai).
- Style table shape: (510, 1, 256) — row index 0–509. Isliye ASSERT: `len(tokens) ≤ 509`.
- Voice file NPZ/ZIP hai; `hf_alpha.npy` naam-lookup se, 128-byte header skip karke,
  float32 LE payload, style row = token count se. (Is spec ka kaam nahi — sirf yaad dila raha hai
  ki token count har fallback choice se badalta hai, isliye style row hamesha FINAL token list se lo.)

---

## §1. Devanagari → IPA BASE TABLE

### §1.1 Parse order (implementation ke liye binding)
1. Input ko NFC normalize karo. Nukta hamesha combining mark U+093C ke roop me aata hai
   (koi precomposed क़ nahi hota) — base consonant + nukta ko ek unit samjho.
2. Text ko akshara me todo: `consonant (+ halant + consonant)* + (matra | inherent schwa |
   anusvāra/chandrabindu/visarga)`.
3. Har unit ko neeche ki tables se map karo → §2 schwa deletion → §3 h-rules → §5 cluster check.
4. Output string ke har character ka vocab me hona §6 ke assert se check hoga.

### §1.2 Swar (independent vowels + matras)
Matra wahi IPA deti hai jo corresponding independent vowel deta hai.

| Devanagari | Matra | IPA | Vocab indices |
|---|---|---|---|
| अ | (inherent) | ə | 83 |
| आ | ा | aː | 43 + 158 |
| इ | ि | ɪ | 102 |
| ई | ी | iː | 51 + 158 |
| उ | ु | ʊ | 135 |
| ऊ | ू | uː | 63 + 158 |
| ऋ | ृ | rɪ | 60 + 102 |
| ए | े | eː | 47 + 158 |
| ऐ | ै | ɛː | 86 + 158 |
| ओ | ो | oː | 57 + 158 |
| औ | ौ | ɔː | 76 + 158 |
| ऑ | ॉ | ɔ | 76 |
| ऍ | ॅ | ɛ | 86 |

Note: ए/ऐ/ओ/औ Hindi me lambe swar hain (koi chhota counterpart nahi). ऑ/ऍ sirf loanwords me
aate hain aur chhote hain. ऋ ko do token (r + ɪ) me likha jata hai — vocab me koi single
syllabic-r token nahi hai.

### §1.3 Vyanjan (consonants)
Aspirated = base + ʰ (162) agar voiceless hai, base + ʱ (163) agar voiced hai.
Dental nishan ̪ (U+032A) vocab me NAHI hai — dental stops plain letters se likhe jayenge
(fallback F-DENT, §1.6).

| Devanagari | IPA | Indices | Devanagari | IPA | Indices |
|---|---|---|---|---|---|
| क | k | 53 | ख | kʰ | 53+162 |
| ग | ɡ | 92 | घ | ɡʱ | 92+163 |
| ङ | ŋ | 112 | च | tʃ | 62+131 |
| छ | tʃʰ | 62+131+162 | ज | dʒ | 46+147 |
| झ | dʒʱ | 46+147+163 | ञ | ɲ | 114 |
| ट | ʈ | 132 | ठ | ʈʰ | 132+162 |
| ड | ɖ | 80 | ढ | ɖʱ | 80+163 |
| ण | ɳ | 113 | त | t | 62 |
| थ | tʰ | 62+162 | द | d | 46 |
| ध | dʱ | 46+163 | न | n | 56 |
| प | p | 58 | फ | pʰ | 58+162 |
| ब | b | 44 | भ | bʱ | 44+163 |
| म | m | 55 | य | j | 52 |
| र | ɾ | 125 | ल | l | 54 |
| व | ʋ | 136 | श | ʃ | 131 |
| ष | ʂ | 130 | स | s | 61 |
| ह | ɦ | 96 | | | |

**Trap warnings (galti aam hai):**
- ग = IPA ɡ (92) hai, letter `g` (49) NAHI. ह = ɦ (96), letter `h` (50) NAHI.
  र = ɾ (125), letter `r` (60) NAHI. व = ʋ (136), letter `v` (64) NAHI.
- च/ज do-token hain (t+ʃ, d+ʒ) — single-codepoint ʧ (133) / ʤ (82) vocab me hain par
  is spec me emit NAHI hote (fallback F-AFFRICATE, §1.6).

**Notation convention (v0.2 se binding):** is spec ki example/output strings me र से
aane wala segment hamesha ɾ (125), व se aane wala hamesha ʋ (136), ग se aane wala
hamesha ɡ (92), aur §3.2 ɛ-list wala swar hamesha ɛ (86) likha jayega. Plain letters
r/v/g/e kabhi output string me nahi aayenge. (Ye convention §2.4/§3.2/§5.2 ki example
strings par lagu hai; §1 ki tables wahi sahi hain aur unhe chheda nahi gaya hai.)

### §1.4 Nukta consonants
| Devanagari | IPA | Indices | | Devanagari | IPA | Indices |
|---|---|---|---|---|---|---|
| क़ | q | 59 | | ख़ | x | 66 |
| ग़ | ɣ | 139 | | ज़ | z | 68 |
| फ़ | f | 48 | | ड़ | ɽ | 129 |
| ढ़ | ɽʱ | 129+163 | | | | |

### §1.5 Conjunct specials (halant rule se hi bante hain — test clarity ke liye list)
- क्ष → kʂ (53+130). त्र → tɾ (62+125). ज्ञ → ɡj (92+52). श्र → ʃɾ (131+125).
- General halant rule: halant wala consonant apna inherent schwa khota hai; agla consonant
  judta hai. Do identical consonants halant se juden to geminate rule lagta hai (F-GEMINATE).

### §1.6 Signs + fallback rules
- **Halant (्):** inherent schwa mitao; koi token nahi.
- **Anusvāra (ं) / Chandrabindu (ँ) — NASAL RULE (fallback F-NAS):** nasalization nishan
  ̃ (U+0303) vocab me NAHI hai, isliye:
  - (a) Stop/affricate se pehle → **homorganic nasal consonant**: velar se pehle ŋ (112),
    palatal se pehle ɲ (114), retroflex se pehle ɳ (113), dental se pehle n (56),
    labial se pehle m (55).
  - (b) Fricative/sonorant se pehle → **drop** (nasalization kho jati hai, koi spurious
    segment nahi jodte).
  - (c) Vowel se pehle ya word-finally → **ŋ** (112) jodo. Wajah: हैं/है, मैं/मे jaise
    minimal pairs me contrast bachana zaroori hai; drop karne se farq mit jayega.
  - Ye rule validation-open hai (§7 listening test se confirm hoga).
- **Visarga (ः):** → ɦ (96).
- **F-DENT:** dental diacritic ̪ kabhi emit mat karo (vocab me nahi). त/थ/द/ध/न plain
  letters se likhe jayenge.
- **F-AFFRICATE:** च/ज hamesha do-token (t+ʃ, d+ʒ). Single ʧ/ʤ emit mat karo.
  Wajah: test-set convention (रचना = ɾətʃnaː) aur token-count/style-row consistency.
  Listening test (§7 Path B) is choice ko confirm karega.
- **F-GEMINATE:** halant se jude do identical consonants → consonant + ː (158).
  Jaise बच्चा → bə + tʃ + ː + aː = bətʃːaː. Listening test se confirm hoga.
- **F-STRESS:** koi stress mark (ˈ 156 / ˌ 157) emit mat karo. Hindi stress predictable hai;
  prosody model par chhod do. Validation me agar stress-related mismatch mile to ye rule
  wapas khulega (open item O9).
- **F-UNKNOWN:** jo Devanagari char is spec ki kisi table me nahi hai, use drop karo
  (Kokoro tokenize ka default behaviour bhi yahi hai) — PAR §6 ka assert test set/corpus me
  kisi bhi drop par FAIL hoga. Drop sirf test coverage ke bahar tolerate hai, aur log me
  darj hona chahiye.

### §1.7 Vocab assert list (source-verbatim, implementation MUST copy from source)
Source: hexgrad Kokoro-TTS `app.py`, `get_vocab()`, commit eff6dd2d (Apache-2.0 project —
ye model ki interface table hai, koi GPL content nahi).

```
_pad = "$"
_punctuation = ';:,.!?¡¿—…"«»“” '   (16 entries, aakhri entry space hai)
_letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'   (52 entries)
_letters_ipa = "ɑɐɒæɓʙβɔɕçɗɖðʤəɘɚɛɜɝɞɟʄɡɠɢʛɦɧħɥʜɨɪʝɭɬɫɮʟɱɯɰŋɳɲɴøɵɸθœɶʘɹɺɾɻʀʁɽʂʃʈʧʉʊʋⱱʌɣɤʍχʎʏʑʐʒʔʡʕʢǀǁǂǃˈˌːˑʼʴʰʱʲʷˠˤ˞↓↑→↗↘'̩'ᵻ"
symbols = [_pad] + list(_punctuation) + list(_letters) + list(_letters_ipa)
```

- Index formula: `$` = 0; punctuation = 1–16; letters: A = 17 … Z = 42, a = 43 … z = 68;
  IPA: ɑ = 69, uske baad string order me +1 har code point par.
- **Duplicate note (176 vs 178 ka suljhaav):** list me 178 entries hain, par apostrophe `'`
  do baar aata hai (IPA tail me: …↗ 172, ↘ 173, `'` 174, U+0329 175, `'` 176, ᵻ 177).
  `get_vocab()` dict banata hai, isliye dict me 177 unique keys hain aur `'` ka lookup
  **176** deta hai (aakhri occurrence jeet-ta hai). Implementation assert:
  `len(list) == 178` AND `len(dict) == 177` AND `dict["'"] == 176` AND `symbols[0] == "$"`.
  U+0329 (combining vertical line below, index 175) apna alag token hai — use kisi
  consonant ke saath jodkar ek grapheme mat samjho; tokenizer code point se chalta hai.
- Is spec ki tables me use hone wala har IPA symbol upar ki list me hai — §6 ka assert
  isi ko machine-check karta hai.

---
## §2. SCHWA-DELETION RULES (Ohala 1983 + conditions)

### §2.1 Underlying form
Har consonant jo halant ya matra se bandha nahi hai, uske baad inherent schwa ə (83) lagao.
Matra inherent schwa ko REPLACE karti hai; halant use MITATA hai. Uske baad §2.2–§2.3 lagao.

### §2.2 Core rule
**ə → ∅ / VC __ CV**
Matlab: schwa tabhi delete hota hai jab uske PEHLE ek vowel + consonant ho (VC) aur uske
BAAD ek consonant + vowel ho (CV). Dono taraf ki shartein poori honi chahiye.

### §2.3 Conditions (sabhi binding hain)
- **C1 — First syllable kabhi nahi:** shabd ke pehle akshara ka schwa kabhi delete nahi hota.
- **C1-exception — Single-vowel word-final schwa (a426359a ki recording, rule change nahi):**
  C1 ki protection sirf MEDIAL schwa par lagu hoti hai. Agar shabd ka ekmatra vowel hi
  word-final schwa ho (single-akshara input), to wo schwa C1 se safe NAHI hai — us par
  §5.1 default lagu hota hai (delete, jab tak §5.3 block na lage). Code
  (`HindiG2P.kt`, `applySchwaDeletion`) first-vowel schwa ko tabhi protect karta hai jab
  wo aakhri segment NA ho; aakhri segment hone par §5.1/§5.3 wali final branch chalti hai.
  Examples (§1.4 nukta rows / §1.5 conjunct rows ke mutabiq): क़→q, फ़→f, क्ष→kʂ.
- **C2 — Vowel se pehle kabhi nahi:** agar schwa ke baad vowel hai (CV nahi bana), to delete nahi.
- **C3 — Word-final tabhi jab cluster admissible ho:** aakhri schwa tabhi delete hota hai jab
  uske hatne se bana aakhri cluster §5 ke mutabiq admissible ho; nahi to schwa rehta hai.
- **C4 — Right-to-left application:** rule ko shabd me DAAYEN se BAAYEN lagao (aakhri schwa
  pehle, phir usse pehle wala, …). Left-to-right lagane se galat output aata hai
  (example: कमल, §2.4 me dekho). Har deletion ke baad context dobara check karo.
- **C5 — Morpheme boundary paar nahi:** schwa deletion morpheme boundary ke paar nahi hota.
  Plain text me boundary marks nahi hote, isliye implementation me C5 ko **exception list
  (§4)** se enforce kiya jayega: jo shabd boundary ki wajah se rule todta hai, wo list me
  jayega. Parser se boundary guess mat karo.

### §2.4 Worked examples (step-by-step; har step me rule + condition ka naam)

**1. राम → ɾaːm**
- Underlying: ɾ aː m ə (रा = ɾ+aː, म = m+ə).
- RTL pass: aakhri schwa (m ke baad) — word-final deletion, C3 check: hatne se aakhri
  cluster sirf `m` rehta hai (koi cluster hi nahi banta) → admissible → DELETE.
- Surface: **ɾaːm** ✓

**2. रचना → ɾətʃnaː**
- Underlying: ɾ ə tʃ ə n aː (र = ɾ+ə, च = tʃ+ə, ना = n+aː).
- RTL pass: pehle schwa (ɾ ke baad) C1 se safe hai (first syllable) → rehta hai.
  Doosra schwa (tʃ ke baad): pehle VC hai (ə + tʃ), baad me CV hai (n + aː) → DELETE.
- Surface: **ɾətʃnaː** ✓

**3. देवनागरी → deːʋnaːɡɾiː**
- Underlying: d eː ʋ ə n aː ɡ ə ɾ iː (दे = d+eː, व = ʋ+ə, ना = n+aː, ग = ɡ+ə, री = ɾ+iː).
- RTL pass: ɡ ke baad wala schwa — pehle VC (aː + ɡ), baad me CV (ɾ + iː) → DELETE.
  ʋ ke baad wala schwa — pehle VC (eː + ʋ), baad me CV (n + aː) → DELETE.
- Surface: **deːʋnaːɡɾiː** ✓

**4. कहना → kɛɦnaː** (poora mechanism §3 me hai; yahan sirf deletion hissa)
- Underlying: k ə ɦ ə n aː (क = k+ə, ह = ɦ+ə, ना = n+aː).
- RTL pass: ɦ ke baad wala schwa — pehle VC (ə + ɦ), baad me CV (n + aː) → DELETE
  (ɦ ke aas-paas deletion normal rule se chalta hai — §3.1).
- Pehla schwa C1 se rehta hai, par §3.2 ke lexical ɛ-list ki wajah se ɛ banta hai.
- Surface: **kɛɦnaː** ✓

**5. सुबह → sʊbəɦ**
- Underlying: s ʊ b ə ɦ ə (सु = s+ʊ, ब = b+ə, ह = ɦ+ə — §2.1 ke mutabiq har
  halant/matra-rahit consonant ko inherent schwa milta hai, aakhri ह ko bhi).
- RTL pass (daayen se): PEHLE aakhri schwa (ɦ ke baad) — word-final, C3 check:
  hatne se aakhri segment sirf ɦ rehta hai (koi cluster nahi banta) → admissible
  → DELETE → s ʊ b ə ɦ.
- AB b ke baad wala schwa — baad me CV NAHI hai (ɦ ke baad koi vowel nahi raha,
  shabd khatam) → C2/VC__CV fail → NO DELETE.
- Surface: **sʊbəɦ** ✓ (ye example dikhata hai ki CV shart kitni zaroori hai —
  aur ki RTL order me final-ə ka step pehle aata hai, tabhi b wala schwa bachta hai)

**6. कमल → kəməl (RTL order ka saboot)**
- Underlying: k ə m ə l ə (क = k+ə, म = m+ə, ल = l+ə).
- RTL pass (daayen se): aakhri schwa (l ke baad) — word-final, C3: hatne se `l` akela
  rehta hai → DELETE → kəməl.
  Ab m ke baad wala schwa — baad me CV NAHI hai (l ke baad koi vowel nahi raha) → NO DELETE.
- Surface: **kəməl** ✓
- (Galat tareeka: left-to-right lagate to m wala schwa pehle delete hota → *kəmlə →
  *kəml — galat. Isliye C4 binding hai.)

**7. लड़का → ləɽkaː**
- Underlying: l ə ɽ ə k aː. RTL: ɽ ke baad wala schwa — VC (ə+ɽ) __ CV (k+aː) → DELETE.
- Surface: **ləɽkaː** ✓

**8. नमक → nəmək**
- Underlying: n ə m ə k ə. RTL: aakhri schwa → DELETE → nəmək.
  m wala schwa — baad me CV nahi (k ke baad vowel nahi) → rehta hai.
- Surface: **nəmək** ✓

**9. किताब → kɪtaːb**
- Underlying: k ɪ t aː b ə. RTL: aakhri schwa → DELETE → kɪtaːb.
- Surface: **kɪtaːb** ✓

**10. मकान → məkaːn**
- Underlying: m ə k aː n ə. RTL: aakhri schwa → DELETE → məkaːn. Pehla schwa C1 se safe.
- Surface: **məkaːn** ✓

**11. मतलब → mətləb**
- Underlying: m ə t ə l ə b ə. RTL: aakhri schwa → DELETE → mətləb.
  l wala schwa — baad me CV nahi (b ke baad vowel nahi) → rehta hai.
  t wala schwa — VC (ə+t) __ CV (l+ə) → DELETE.
- Surface: **mətləb** ✓

---

## §3. h-EXCEPTION

### §3.1 Structural rule (general, har shabd par lagu)
- **ɦ kabhi delete nahi hota.** Schwa deletion ke dauraan ɦ khud hamesha bachta hai —
  koi output aisa nahi hona chahiye jisme underlying ɦ gayab ho (jaise कहना se *kənaː).
- **ɦ ke aas-paas schwa deletion NORMAL rule se chalta hai:** ɦ ke baad wala schwa
  VC__CV poora kare to delete hota hai (कहना: kɛɦnaː me post-ɦ schwa deleted hai);
  ɦ se pehle wala schwa first syllable ho to C1 se bachta hai; ɦ word-final ho to
  usse pehle wala schwa CV-shart fail hone se bachta hai (सुबह: sʊbəɦ).
- Is structural rule ka koi exception nahi hai — jo dikhe wo §3.2 ka lexical case hoga.

### §3.2 Lexical ɛ-list (general rule NAHI — sirf listed morphemes par lagu)
Kuch morphemes me orthographic ə, ɦ se pehle **ɛ (86)** ban jata hai. Ye GENERAL rule
nahi hai — ye sabit hai kyunki पहला isko todta hai (neeche dekho). Isliye ye list se
chalta hai, rule se nahi:

| Morpheme | Example | Output | Note |
|---|---|---|---|
| कह | कहना | kɛɦnaː | post-ɦ schwa normal delete + ɛ |
| रह | रहना | ɾɛɦnaː | wahi pattern |
| बह | बहना | bɛɦnaː | wahi pattern |
| पहन | पहनना | pɛɦənnaː | pɛɦən + naː (nn vs nː — F-GEMINATE consistency OPEN hai, O8 dekho; tab tak table output hi T2 expected hai) |
| शहर | शहर | ʃɛɦəɾ (OPEN) | ɛ pakka, par aakhri schwa ka rehana/khatam hona validation-open hai (§4 E2) |

**पहनना ki step-by-step derivation (v0.2, poori):**
- Underlying: p ə ɦ ə n n aː (प = p+ə, ह = ɦ+ə, न् = n [halant, koi schwa nahi],
  ना = n+aː).
- Step 1 — ɛ-list (§3.2, पहन morpheme): word-start par पहला schwa ə → ɛ
  → p ɛ ɦ ə n n aː.
- Step 2 — ɦ ke baad wala schwa: pehle VC hai (ɛ + ɦ), par BAAD me CV NAHI hai
  (ɦ ke baad n aata hai, aur us n ke baad n hai — koi vowel nahi) → C2 fail
  → NO DELETE → ə rehta hai.
- Surface (table output): **pɛɦənnaː** ✓
- **OPEN (rule change nahi, O8 se juda):** is derivation me न्न par F-GEMINATE
  lagane se nː banta hai (pɛɦənːaː), jabki table nn likhta hai. Dono me se kaun
  sahi hai, ye Path B listening test (O8, geminate form) faisla karega — tab tak
  T2 expected = table output (pɛɦənnaː) hi rahega, aur F-GEMINATE ka न्न par
  lagu hona OPEN hai.

### §3.3 Non-cases (jo exception me NAHI aate — galat generalisation se bachne ke liye)
- **पहला → pəɦlaː** (NOT *pɛɦlaː): ɦ ke baad wala schwa normal delete hota hai, par
  pehla schwa ə hi rehta hai. Ye example sabit karta hai ki ə→ɛ/__ɦ koi general rule
  nahi hai — isliye §3.2 list-based hai.
  - **UNVERIFIED (Path A confirmation ke liye flag, v0.2):** "pehla" [pɛɦlaː] aam
    pronunciation hai — agar reference pɛɦlaː de to पहला ɛ-list me jud jayega,
    mechanism bachega, sirf example badlega. Tab tak T2 expected = pəɦlaː hi rahega.
- **सुबह → sʊbəɦ** (NOT *sʊbɛɦ): ɦ word-final hai, usse pehle wala schwa ə rehta hai.
- **महल → məɦəl** (NOT *mɛɦəl): ɦ ke baad wala schwa C3/CV-shart se bachta hai,
  pehla schwa ə rehta hai — ye morpheme ɛ-list me nahi hai.

---

## §4. EXCEPTION LIST

### §4.1 Seed entries (is version me sirf ye hain — koi invented word nahi)
- **E1 — §3.2 ɛ-list** (कह/रह/बह/पहन/शहर): ye overrides hain, rule-failure nahi.
  Implementation me in morphemes ko word-start par match karke ɛ lagao, baaki pipeline
  normal chalegi.
- **E2 — शहर final-ə (OPEN):** ʃɛɦəɾ vs ʃɛɦɾ — reference validation (§7 run 1) faisla
  karega; tab tak spec me dono forms note hain, koi ek claim nahi.
- **E3 — (KHAA LI — validation run 1 se bharega):** literature accuracy ~89% ka matlab
  hai ki 17-word test set me ~2 shabd rule se galat nikalenge. Wo shabd abhi NAHI pata
  (baaki 6 shabd open item O1 hain) — isliye E3 ko guess se bharna MANA hai.
  Run 1 ka mismatch hi E3 ka seed banega.

### §4.2 Watch list (validation run 1 me pehle check honge, exception tabhi banenge jab reference galat saabit kare)
- नमस्ते: nəməsteː (careful) vs nəmsteː (fast) — dono forms attested hain; reference
  ek chune, doosra note me rahe.
- शहर: upar E2.
- Koi bhi test-set shabd jo run 1 me mismatch de.

### §4.3 List badhne ka process (binding)
1. Validation run (§7) me mismatch milta hai.
2. Mismatch ko taxonomy me dalo: SCHWA / ASPIRATION / NASAL / VOWEL-LENGTH / GEMINATE / LEXICAL.
3. Reference IPA + literature + (zaroorat par) listening test — teeno me se kam se kam
   DO se confirm karo ki rule galat hai aur reference sahi hai.
4. Tabhi entry jodo: shabd + surface form + wajah + date + kis run ne pakda.
5. Test set dobara chalao — test set ke saare shabd pass hone chahiye (§7 pass bar).
6. Bina evidence ke entry jodna MANA hai. List ka size badhna koi sharm ki baat nahi —
   ~89% base accuracy ka matlab hi ye hai ki list banegi.

---

## §5. CLUSTER TABLE (word-final schwa deletion ke liye)

### §5.1 Default rule
**Default = word-final schwa DELETE hota hai.** Rok sirf tab lagti hai jab hatne se bana
aakhri cluster §5.3 ki inadmissible list me ho. Burden of proof blocking par hai —
jo cluster kisi list me nahi hai, wo delete hoga, aur agar reference use rokta hai to
wo cluster inadmissible list me jodega (§4.3 process se).

### §5.2 Admissible final clusters (attested examples ke saath)
| Pattern | Example | Output |
|---|---|---|
| nasal + stop | अंत | ənt |
| fricative + stop | दोस्त | doːst |
| fricative + stop | सख्त | səxt |
| sonorant + stop | दर्द | dəɾd |
| sonorant + stop | गर्म | ɡəɾm |
| sonorant + fricative | बर्फ | bəɾf |
| sonorant + stop | अर्थ | əɾtʰ |
| stop + stop | वक़्त | ʋəqt |
| stop + sonorant | मित्र | mɪtɾ |

### §5.3 Inadmissible final clusters (seed — validation se confirm hoga)
| Pattern | Wajah |
|---|---|
| C2 = ɦ | Surface ke aakhri do vyanjan C1C2 me C2 = ɦ ho to word-final schwa deletion BLOCK hai — ɦ coda-cluster ka doosra member nahi banta. Check deletion karne se PEHLE hota hai: hatne ke baad aakhri do vyanjan kya banenge, ye dekho; agar doosra ɦ ho to deletion mat karo. |
| C2 = voiced aspirate (ɡʱ, dʒʱ, ɖʱ, dʱ, bʱ) | conservative seed — reference se confirm hona baaki hai |
| Koi bhi cluster jo reference me final-ə rokta dikhe | §4.3 se add hoga |

**Worked example — C2 = ɦ check (schematic, koi naya shabd claim nahi):**
Maan lo kisi derivation me halant wala roop …C ् ɦ ə ho (C par halant, phir ɦ+ə).
Final-ə deletion karne par surface …Cɦ banega — aakhri do vyanjan C1C2 = Cɦ,
C2 = ɦ → is row se deletion BLOCK hai → final ə RETAIN rahega, surface …Cɦə.
Implementation check: final-ə deletion karne se pehle hatne ke baad wale aakhri do
vyanjan dekho; agar doosra ɦ ho to deletion skip karo. Note: aisa koi attested
test-set shabd is spec me nahi hai — ye row conservative seed hai, O5 reference
confirmation me confirm hogi.

**OPEN (v0.2 review note, rule change NAHI):** C2=ɦ row aur pre-ɦ medial schwa ke
beech tension hai — वजह-type roop (…V C ə ɦ ə#) me current rules (RTL order +
C2=ɦ block) ʋədʒɦə dete hain, jabki agar reference ʋədʒəɦ deta hai to ya to
C2=ɦ row galat hai ya pre-ɦ schwa retention ka alag rule chahiye. Iska faisla
validation run 1 (Path A) me hoga — tab tak C2=ɦ row wahi rahegi, koi rule nahi
badla gaya hai, aur वजह T2 me nahi hai.

Note: सुबह-type cases is table se nahi, §2 ke CV-condition se sulajhte hain (ɦ ke baad
koi vowel hi nahi hai) — dono mechanism ko confuse mat karo.

---

## §6. VOCAB-COVERAGE ASSERT (machine-checkable hissa)

Implementation ke test me ye asserts MUST-pass hain — koi bhi fail = G2P gate FAIL.

- **ASSERT-G1 (vocab membership):** G2P output string ka HAR character §1.7 ki 178-entry
  list me hona chahiye. Koi bhi character bahar ho to test FAIL (implementation use
  silently drop karke aage nahi badh sakta — drop sirf §1.6 F-UNKNOWN ke tehat hota hai
  aur test me drop = FAIL).
- **ASSERT-G2 (length):** `len(tokens) ≤ 509` (510-row style table, indices 0–509).
  Zyada ho to input ko sentence boundary par chunks me todo; har chunk alag synthesize
  hoga. Chunking rule: sentence end (।, ., !, ?) par todo; ek sentence bhi 509 se zyada
  ho to word boundary par todo.
- **ASSERT-G3 (wrap + style row):** final token list = `[[0, *tokens, 0]]`; style row
  index = **unwrapped** `len(tokens)` (wrap se pehle wali length). Style row hamesha
  FINAL token list se lo — koi fallback (drop/insert) uske baad nahi aana chahiye.
- **ASSERT-G4 (table self-test):** vocab list ki length == 178; `symbols[0] == "$"`;
  unique keys == 177; duplicate sirf apostrophe hai; `dict["'"] == 176`.
- **ASSERT-G5 (no-drop audit):** test set aur corpus par koi F-UNKNOWN drop nahi hona
  chahiye. Drop dikha to ya to table me kami hai (spec update) ya input coverage se
  bahar hai (corpus se bahar rakho, par log karo).

### Input edge rules (v0.2)
- **INPUT PUNCTUATION:** input me punctuation characters (।, ., ,, !, ? aadi — jo
  §1.7 ki 16-entry punctuation list me hain) **token banakar RAKHE jayenge, strip
  nahi kiye jayenge** — kyunki ASSERT-G2 ka chunking rule sentence boundary par
  toda hai aur Kokoro tokenize vocab-wali punctuation ko tokens me rakhta hai. Jo
  punctuation §1.7 ki list me nahi hai, wo F-UNKNOWN ke tehat drop hoga (test me
  drop = FAIL, ASSERT-G5 ke mutabiq).
- **EMPTY INPUT:** khaali input (ya sirf drop hone wale characters wala input) →
  tokens = [] → wrap `[[0, 0]]` → style row index 0 (wrap se pehle wali length 0).
  G2P is par FAIL nahi hoga; caller khaali input par speak call skip karega ya
  silence return karega (caller-side handling, G2P rule nahi).

### Coverage test plan
- **T1 — Table self-test:** ASSERT-G4 (upar).
- **T2 — Test set (17 words):** har shabd ka G2P output, expected IPA se word-exact
  match hona chahiye. **11 shabd is spec me worked examples ke saath defined hain
  (§2.4 examples 1–11: राम, रचना, देवनागरी, कहना, सुबह, कमल, लड़का, नमक, किताब,
  मकान, मतलब) — examples 6–11 T2 me ginte hain, ye v0.2 me saaf declare kiya gaya
  hai.** Baaki **6 shabd OPEN ITEM O1 hain — FRESH DEFINE karne hain** (accessible
  records me nahi mile, grep se confirm; 'recover' ka daava nahi); jab tak 17 poore
  nahi hote, T2 "pass" claim nahi hoga (partial 11/11 likha jayega, 17/17 nahi).
- **T3 — Chhota Hindi corpus (≥100 utterances):** selection criteria (content yahan
  invent nahi kiya ja raha — implementation waqt banega):
  - Flora app ke apne Hindi UI strings (jo user roz sunega) — pehli priority.
  - High-frequency Hindi shabd/sentences (rozmarra baat-cheet) — doosri priority.
  - Har category se kuch: schwa wale, h wale, nasal wale, conjunct wale, loanwords.
  - Corpus file test ke saath rakhi jayegi; har entry ka expected IPA reference se aayega (§7).
- **T4 — Regression:** har spec update ke baad T1+T2+T3 dobara chalenge; purana pass
  tootna = gate FAIL.

---

## §7. REFERENCE-VALIDATION PLAN

### Haalat (verified)
- **espeak-ng is VM me NAHI hai** (round-2 me verify hua). Isliye Path A ko doosri
  machine chahiye; Path B ko phone + debug APK chahiye. Dono abhi OPEN hain (O7).

### Path A — espeak-machine reference comparison (IPA-to-IPA, data transfer ZERO)
1. Ek aisi machine dhoondho jahan espeak-ng installed ho (ya install ho sake).
   Is VM par install karne ki koshish dobara karne se pehle check karo ki wo allowed hai
   ya nahi — zabardasti nahi.
2. Us machine par espeak-ng Hindi voice se T2 (17 words) + T3 (corpus) ka reference IPA
   nikaalo. **espeak sirf reference TOOL hai** — uska output sirf compare karne ke
   liye hai; uska koi rule, list ya data spec ya app me nahi jayega.
3. Dono taraf ke IPA ko §1 ke symbol conventions par normalize karo (jaise espeak ka
   output agar alag symbols use kare to mapping table banao — mapping table comparison
   ke liye hai, ship nahi hogi).
4. Har mismatch ko §4.3 ki taxonomy me dalo (SCHWA / ASPIRATION / NASAL /
   VOWEL-LENGTH / GEMINATE / LEXICAL).
5. Har mismatch ka faisla: (a) spec rule galat hai → spec update + exception entry;
   (b) reference galat/attested-variant hai → note me darj, spec unchanged;
   (c) faisla nahi ho raha → Path B listening test par bhejo.
6. **Pass bar:** T2 = 17/17 (exception list ke saath); T3 ≥ 95% word-exact.
   T2 ka koi bhi shabd exception list ke baad bhi fail ho to **spec wapas revision me
   jayegi** — "close enough" bolkar gate pass karna MANA hai.

### Path B — Phone-side listening validation (aakhri gate)
1. Debug APK me G2P ko test hook se jodo (user-facing feature nahi — sirf test).
2. T2 + T3 ke samples ko neural voice (hf_alpha) se bajao — 4 voice paths par check karo
   (main chat, call announcement, event announcement, wake ack) + in-call fallback path
   (FloraInCallService wahi engine padhta hai).
3. Sunne wala (user + coordinator) har sample ko pass/fail dega: samajh aaya? schwa sahi?
   nasal sahi? aspiration sahi?
4. **Pass bar:** user sample approval + Phase 10 phone test pass — yehi neural voice ka
   standing gate hai (system TTS Lily tabhi hatega). Bina iske "neural ready" claim nahi.

### Adjudication rule
Reference aur literature me takrao ho to faisla listening test (Path B) karega —
kabhi bhi reference ka data uthakar spec/app me copy karke faisla nahi hoga.

---

## §8. CLEAN-ROOM DECLARATION

Ye spec neeche likhi cheezon se bani hai, aur sirf inse:

**Use kiya gaya (sources):**
- Ohala (1983) — Hindi schwa deletion rule ə → ∅ / VC__CV aur uski conditions,
  jaise round-2 research me summarize hua tha (first syllable, pre-vocalic, word-final
  cluster, right-to-left, morpheme boundary).
- Narasimhan et al. — schwa-deletion constraints (upar wali conditions ka literature base).
- Standard Hindi phonology — swar/vyanjan inventory, aspiration (ʰ/ʱ), nasal
  assimilation, ɦ/ʋ/ɾ jaise sounds ki pehchan. Ye linguistic facts hain, kisi ek
  source ka proprietary data nahi.
- hexgrad Kokoro-TTS `app.py` `get_vocab()` (commit eff6dd2d, Apache-2.0) — sirf
  model ki 178-entry interface table (§1.7), jo model ko address karne ke liye zaroori hai.
- Is spec ki tables, rule ordering, fallback rules, cluster table aur exception-list
  process — **is spec ki apni original formulation** hai.

**Use NAHI kiya gaya (explicit):**
- espeak-ng ke `hi_rules`, `hi_list`, `ph_hindi` — in files ko khola nahi gaya, inka
  koi rule, list ya data is spec me copy nahi hua hai.
- Koi GPL-licensed code ya data — is spec me nahi hai, aur APK ke G2P path me kabhi
  ship nahi hoga.
- Kisi creator ka code, text, assets ya trademark — kuch nahi liya gaya.
- espeak-ng ka bhavishya ka role sirf §7 Path A ke mutabiq black-box reference
  comparator hai — uska output compare hoga, transfer nahi hoga.

**Agar koi is spec me GPL-origin content paaye to:** use turant flag karo, wo hissa
spec se hataya jayega aur dobara clean-room tareeke se likha jayega. Clean-room ka
matlab hi ye hai ki har line ka source bataya ja sake.

---

## OPEN ITEMS (is spec ke baad bhi khule — guess par "complete" mat kaho)

- **O1 — Test set ke baaki 6 shabd:** 11 worked examples defined hain (§2.4
  examples 1–11). Baaki 6 shabd FRESH DEFINE karne hain — accessible records me
  nahi mile (grep se confirm; 'recover' ka daava nahi); tab tak T2 partial hai
  (11/11, 17/17 nahi).
- **O2 — Affricate form:** t+ʃ / d+ʒ (do-token) vs ʧ / ʤ (single-token) — spec ne
  do-token chuna hai (test-set convention), listening test (Path B) se confirm hona baaki hai.
- **O3 — Anusvāra fallback (§1.6 F-NAS):** (a)/(b)/(c) branches listening test se
  confirm hone baaki hain — khaas kar word-final ŋ insertion (हैं/है contrast).
- **O4 — शहर final-ə:** ʃɛɦəɾ vs ʃɛɦɾ — Path A run 1 faisla karega.
- **O5 — Inadmissible cluster list (§5.3):** seed list hai; reference se confirm hona baaki hai.
- **O6 — Corpus (T3):** ≥100 utterances ki file implementation waqt banegi (§6 criteria se).
- **O7 — Validation machines:** espeak wali machine (Path A) aur phone + debug APK (Path B)
  abhi arrange nahi hue hain.
- **O8 — Geminate ː:** consonant + ː form listening test se confirm hona baaki hai.
- **O9 — Stress marks:** F-STRESS (koi ˈ/ˌ nahi) — agar Path A/B me stress mismatch
  mile to ye rule wapas khulega.
- **O10 — Scope se bahar (alag spec chahiye):** Devanagari digits ka number-expansion,
  Roman-script Hindi input, aur archaic vowels (ॠ/ऌ — F-UNKNOWN se drop honge).

**Spec version history:**
- v0.1 (2026-10-30) — pehla draft, user ke clean-room decision ke baad likha gaya.
- v0.2 (2026-10-30) — CORRECTION PASS (detail verification ke verdict par; RULE CHANGE
  NAHI — sirf notation + completeness fixes, taaki T2 me galat IPA lock na ho):
  1. Notation convention binding ki gayi (§1.3 ke baad): example strings me र = ɾ,
     व = ʋ, ग = ɡ — §2.4 (राम, रचना, देवनागरी), §3.2, §5.2 aur §1.6 ke रचना example
     me plain r/v/g theek kiye gaye. §1 ki tables chhedi nahi gayi hain.
  2. h-exception outputs me e → ɛ (§3.2 table, §2.4 ex 4, §3.1, §3.3 ke starred
     wrong-forms, §4 E2, O4): kɛɦnaː, ɾɛɦnaː, bɛɦnaː, pɛɦənnaː, ʃɛɦəɾ — ɛ = 86,
     letter token 47 nahi.
  3. §5.2 corrections: दोस्त → doːst, अर्थ → əɾtʰ, दर्द/गर्म/बर्फ/मित्र me r → ɾ,
     वक़्त me v → ʋ.
  4. T2 count reconcile: §2.4 ke 11 examples T2 me ginte hain (declare kiya gaya) —
     11 defined, baaki 6 FRESH DEFINE karne hain (accessible records me nahi mile,
     grep se confirm; 'recover' ka daava nahi). O1/E3/T2 isi hisaab se update hue.
  5. सुबह ki derivation poori ki gayi (underlying final-ə step samet, RTL order me)
     + §5.3 C2=ɦ row ki rewording (check deletion se pehle hota hai) + schematic
     worked example (koi naya shabd claim nahi).
  6. पहला counterexample ko Path A confirmation ke liye UNVERIFIED flag kiya gaya
     (§3.3): agar reference pɛɦlaː de to पहला ɛ-list me jud jayega, mechanism
     bachega, example badlega.
  7. §6 me input edge rules jode gaye: vocab-wali punctuation token banakar rakhi
     jayegi (strip nahi); empty input → [[0, 0]], style row 0, caller-side skip/silence.
  8. §3 me पहनना ki poori step-by-step derivation jodi gayi — nn vs nː (F-GEMINATE)
     consistency ko O8 se juda OPEN flag kiya gaya hai (rule change nahi; tab tak
     T2 expected = table output pɛɦənnaː).
  - Review note (rule change nahi, OPEN flag): C2=ɦ row aur pre-ɦ medial schwa ke
    beech tension (§5.3 me darj, वजह-type roop) — faisla validation run 1 (Path A) me hoga.
  - Abhi bhi validation-open: O2/O3/O4/O5/O7 (run 1 ke hain, wahi rahenge); O1
    partially resolved (11 defined, 6 fresh define karne hain); O6/O8/O9/O10 wahi hain.
  - Agla version validation run 1 ke natijon se banega.
