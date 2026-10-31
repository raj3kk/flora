# CEO D-20 Freeze — Specs Bundle (Part 1, 2026-10-31)

Is doc se neeche ke saare points FREEZE hain (spec/definition
level). **Koi point is freeze se build nahi hota** — jahan build
mana hai wahan saaf likha hai. Verify base: live tip; code anchors
neeche darj hain.

## (a) GAP-M6 Marketing 8.9 — SPEC ONLY freeze (build NAHI)

- Frozen spec: `docs/FLORA_MARKETING_8_9_SPEC.md` (tip par maujood)
  — Maya records me Marketing sirf naam ke roop me mila hai
  (side-menu list + `MarketingAlarmReceiver` naam); vyavhaar
  UNVERIFIED hai, isliye **koi marketing code build nahi hua** hai.
- Freeze shart: M6 tabhi build hoga jab (i) records se verified
  vyavhaar mile, ya (ii) user khud definition de (kya, kis channel
  par, kis consent se). Guess par marketing tool banana abhi bhi
  mana hai. `ui/Sd8SettingsScreen.kt:72` par bhi 8.9 SPEC-FIRST
  darj hai.

## (b) SD-8 specs C1–C4 freeze (zero-plan §3C)

- **C1 = 8.9 Marketing:** upar (a) wali spec hi frozen spec hai.
- **C2 = 8.13 Touch Guard:** SPEC-FIRST, detail records me
  UNVERIFIED hai (`docs/FLORA_SD_IMPLEMENTATION_PLAN.md` §5 table:
  8.13 gate = "Spec freeze pehle … tabhi build + gate").
  **Koi Touch Guard code is freeze se nahi banta** — vyavhaar
  freeze hone tak build mana hai. (Is cycle me Touch Guard ka
  koi code anchor nahi hai — yehi freeze ka matlab hai.)
- **C3 = 8.10 Whiteboard-full:** SPEC-FIRST. Maujooda
  `ui/WhiteboardScreen.kt` (canvas + voice) "full" ka poora
  vyavhaar nahi hai; poora vyavhaar freeze hona baaki hai
  (`Sd8SettingsScreen.kt:75-77` par bhi SPEC-FIRST darj hai).
  **Is freeze se whiteboard-full build nahi hota.**
- **C4 = 8.11 Drawer/widgets:** App UI shell DONE hai, par drawer
  navigation test + home widget test (Glance/AppWidget pattern)
  spec me freeze hona baaki hai (SD plan §5 table 8.11).
  **Is freeze se drawer refactor / AppWidget receiver build nahi
  hota** — dedicated batch ka spec + gates iske baad aayenge.

## (c) H15 market definition — educational watchlist + Seekho

- **Definition (FROZEN):** Market = read-only educational
  watchlist + Seekho (basic jaankari, static text). **Trading
  execution / advice kabhi nahi.**
- Anchors: `docs/FLORA_MARKETS_SPEC.md` (tip par maujood —
  read-only watchlist, koi trading nahi, price indicative);
  `ui/MarketsScreen.kt:45` (Seekho — static text) aur `:226`
  ("Seekho — Share Market ki basic jaankari (sirf education)");
  `tools/SearchTools.kt:432` `stockPrice` (Yahoo lane, bina key);
  `data/WatchlistStore.kt` (symbols sirf is phone par, server par
  kabhi nahi). "~32 tools" wala daava kabhi fact nahi tha — is
  freeze me bhi nahi hai.

## (d) H16 pairing definition — app-link / pairing-code

- **Definition (FROZEN):** Pairing = maujooda do mechanisms —
  (1) app-link (website account link: `data/FloraPreferences.kt:337`
  `linkedUserId()` + device-only refresh token; access token
  kabhi save nahi hota), aur (2) pairing code
  (`data/FloraPreferences.kt:444` PC pairing code, sirf is phone
  par; `ui/PcLinkScreen.kt` par display).
- **Saaf seema:** Music-app pairing (C7) is definition ka hissa
  NAHI hai — `ui/FavSongsScreen.kt:44-46,106` par pairing HELD /
  undefined darj hai, isliye favourites abhi bhi YouTube search
  se bajte hain; koi pairing invent nahi ki gayi hai. Ye mapping
  nuance coordinator ke liye flag hai (CEO text "app-link /
  pairing-code" maujooda mechanisms se match karta hai; C7 alag
  HELD rehta hai).

## (e) H18 trained-model note — learned patterns + skill engine

- **Definition (FROZEN):** "Trained" ka matlab = on-device learned
  patterns + skill engine. **LLM retraining ka daava kabhi nahi.**
- Anchors: `planner/LearnedEngine.kt` (learned patterns, bina AI
  call ke repeat kaam); `ui/Sd8SettingsScreen.kt:277`
  ("Skill engine = on-device learned patterns. LLM retraining ka
  daava kabhi nahi."); SD plan §5 table 8.14 bhi yahi kehta hai.

## (f) H19 wake sub-TODO split

Wake kaam (8.16/8.17 bundle) neeche ke sub-TODOs me split hai —
koi bhi sub-TODO is freeze se build nahi hota, sirf darj hai:

1. **W-1 MFCC v0 metric:** threshold (0.60 default) par
   false-accept / false-reject phone test (SD plan §5 table 8.16
   gate) — phone-gated.
2. **W-2 Dataset collection design (8.17):** consent-gated
   collection, on-device encrypted storage, user-delete — spec
   level; collection ka maqsad sirf wake/voice-match hai.
3. **W-3 24×7 mic disclosure UI verification:** disclosure TODO
   (P1-02 me darj) — Wake Word ON par mic-on disclosure dikhna
   chahiye; verification phone/user par hai.
4. **W-4 Neural wake pipeline:** dataset (W-2) ke baad hi; tab
   tak MFCC v0 hi shipped path hai (D-29 barkaraar).
5. **W-5 Battery test:** wake listener ka battery asar phone
   test se (8.16 gate ka hissa).

## (g) 3D orb — spec-done, art-pending (koi fake art nahi)

- **Spec (FROZEN, procedural-only):** 3D orb sirf code-drawn /
  procedural hoga — Compose Canvas par radial-gradient shading,
  depth layers (outer glow → mid sphere → inner highlight) aur
  state/amplitude-driven motion (pehle se maujood FloraOrb ke
  state/amplitude pattern par). **Koi external/creator asset
  kabhi nahi** — na image, na model, na copy kiya gaya design.
- **Haalat:** Maujooda `ui/FloraOrb.kt` 2D code-drawn orb hai
  (apna art — header me "koi creator asset nahi" darj hai) —
  ye 3D NAHI hai aur is freeze me use 3D daava nahi kiya ja
  raha hai. **Poora 3D art is cycle me possible nahi hua, isliye
  status = SPEC-DONE + ART-PENDING.** Koi fake 3D art banakar
  ship nahi ki gayi hai.
