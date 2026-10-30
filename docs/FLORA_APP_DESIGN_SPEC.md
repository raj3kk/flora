# Flora app design spec — full-app UI/UX pass (user order 2026-10-30 ~13:21 IST)

> User order (shabdon me): "Pura app ko acche se design karo sahi se. Sara
> chij ka size, header, jo bhi hai option sahi se rahe. Drop down mein sara
> option show karen, acche se samajhne layak aur sab section divide karen, jo
> andar option kuchh khula option na ho."

Is spec ka matlab: **sirf layout / visibility / consistency** badlegi.
Behavior, functionality, gating, defaults — kuch nahi badlega. Koi option
remove nahi hoga; rename sirf samajhne layak banane ke liye, mapping §4 me.

## §0. Pre-audit (implement se PEHLE) — PASS

- **P1 appearance se koi conflict nahi:** `FloraApp.kt` me `orbDepthOn =
  false`, `edgeGlowOn = false` defaults hain; charm (OverlayMicService
  mirror) P1 record ke mutabiq default OFF hai. Ye design pass in teeno
  defaults ko **chhedega nahi** — na ON karega, na OFF ka logic badlega.
- **Title rename safe hai:** `screenTitle()` sirf TopAppBar title me istemal
  hota hai (`FloraApp.kt` ~1509) + ek comment (`SelfMap.kt`); koi test
  in strings par dependent nahi hai.
- **Menu count fixed hai:** main menu me 46 items + 5 section headers
  (`FloraApp.kt` ~1508–1734). Koi item hataya nahi jayega.
- **Scroll audit:** neeche §11 ki table me har screen ka verdict hai.

## §1. Size scale (kuch bahut chhota/bada ya dabaa hua nahi)

- **Text size:** app me kahin explicit `fontSize` nahi hai — Material3
  default typography hi scale hai (body ~14sp, bodyLarge ~16sp, title
  ~22sp). Ise chhedna nahi hai; consistency isi se aati hai.
- **Base screen padding: 16dp** — main Scaffold content Column
  (`FloraApp.kt` ~1738–1740) ye deta hai. Isliye main Column ke andar
  render hone wali koi screen apna **extra outer padding nahi jodegi**
  (warna 16+16=32dp, 16+12=28dp jaisa double padding hota hai).
  Exception: SetupWizard Scaffold se PEHLE standalone khulta hai, isliye
  uska apna 16dp **rahega**.
- **Section spacing:** section header ke pehle 12dp, items ke beech 8dp,
  chhote notes ke beech 4dp (maujooda pattern, standardize yahi hai).
- **Buttons:** Material3 Button standard (min height 40dp) — custom chhota
  button kabhi nahi.

## §2. Header rule (koi section bina header ke nahi)

- Har screen ka TopAppBar title `screenTitle()` se aata hai — har Screen
  ka title hona hi chahiye.
- Har screen ka pehla element uska title header Text hai.
- Jo screen 2+ groups rakhti hai, har group ka apna section header Text
  hoga (jaise Settings me "Keys — …", "Wake Word — …", "Background aur
  notifications — …"; Sd8 me "8.3 Languages"; Permissions me "App
  permissions", "Background"; Routines me "Meri routines", "Mere
  macros"; FavSongs me "Favourites (n):").

## §3. Dropdown rule (saare options dikhen, koi missing/cut nahi)

- App me **ek hi dropdown** hai: main Menu (TopAppBar actions,
  `FloraApp.kt` ~1508). Usme 46 items + 5 section headers hain.
- Material3 DropdownMenu lambi list ko **andar hi scroll** karta hai
  (framework ka internal scroll; koi height cap nahi lagaya gaya hai).
- **End-marker:** menu ke aakhir me ek disabled item
  "— Sab options khatm —" hoga, taaki user ko pata chale ki usne
  saare options dekh liye hain (koi option neeche chhupa hua nahi hai).
- Koi item remove / reorder nahi hoga; section headers wahi rahenge.

## §4. Label rule (samajhne layak, chhota, Hinglish-first)

TopAppBar titles jo angrezi jargon the, wo chhote Hinglish titles honge.
Menu item texts wahi rahenge, sirf jahan menu item screen title se alag
ho wahan bhi yahi mapping lagegi (consistency ke liye):

| Pehle (screenTitle) | Naya |
|---|---|
| Permissions | Anumati |
| Licence | Plan |
| SD-1 Connectors | Apps jodo |
| Task list | Kaam |
| SD-8 Settings | Extra Settings |
| Teach once | Ek baar sikhao |
| PC Link | PC jodo |

Baaki titles (Chat, Settings, Yaadein, Scan, Camera dekho, Tools, SOS,
Face, Docs, Connect, Favourite gaane, Gaadi, Awaaz sikhao, PIN gate,
Builder, Planner, Journal, Models, Notifications, Whiteboard, Home,
Avatar, Browser, Auto, Privacy, Terms, Auto Log, Live Screen) pehle se
samajhne layak hain — wahi rahenge.

## §5. Section-division rule (andar ka koi option khula/hidden na rahe)

- Har screen ke groups header + spacing se divided honge (§2).
- Koi group bina header ke nahi; koi option kisi section ke bahar
  "latka hua" nahi.
- Jo screen Settings ke andar inline khulti hai (BackupScreen,
  Settings LazyColumn ka pehla item), uska apna header ("Backup /
  Restore") wahi rahega.

## §6. Scroll rule (lambi screen ka neeche wala option pahunche)

- Har content screen scroll karegi: `LazyColumn` ya
  `verticalScroll(rememberScrollState())`.
- **Exception (scroll nahi chahiye):** camera preview / canvas screens —
  Scan (preview `weight(1f)`), CameraLook, Face, Whiteboard — aur chhota
  PIN gate (LockScreen). Inme content screen par fit hota hai.
- BackupScreen Settings LazyColumn ke item ke andar hai — outer scroll
  hi kaafi hai, isko apna scroll nahi chahiye.

## §7. Safe-zone rule (koi option overlay ke peeche nahi)

- Flora screens Scaffold padding ka samman karengi (content TopAppBar
  ke neeche nahi jayega — `padding` lambda maujooda hai).
- Charm / overlay pill (OverlayMicService: top-center pill 72dp,
  y=24dp; charm TOP|END x=16, y=300) **doosre apps par** overlay hai,
  Flora screens par nahi — Flora screens inke liye jagah reserve nahi
  karengi.
- Edge glow sirf chat area ke around, non-clickable border hai —
  scroll/input par koi asar nahi (P1 record ke mutabiq).

## §8. Text rule (text kabhi cut nahi hoga)

- Labels/options par `maxLines = 1` + ellipsis **kabhi nahi** (app me
  abhi bhi aisa kuch nahi hai — wrap default hai, wahi rahega).
- TopAppBar titles chhote rakhe gaye hain (§4) taaki chhoti screen par
  bhi title cut na ho.

## §9. Touch-target rule

- Material3 Button (40dp) / IconButton (48dp) standard — chhota custom
  target nahi.
- SetupWizard ke permission rows: full-width clickable + vertical
  6dp padding + 2 lines text ≈ 56dp height — 48dp minimum se upar hai,
  theek hai.

## §10. Keyboard rule (chat input keyboard ke peeche nahi)

- MainActivity par `android:windowSoftInputMode="adjustResize"` hoga,
  taaki keyboard khulne par chat area sikude aur input field hamesha
  keyboard ke upar dikhe. (Manifest me pehle koi mode set nahi tha.)

## §11. Per-screen audit table (code se, implement se pehle)

| Screen | Verdict | Fix |
|---|---|---|
| Chat / Baat view (`FloraApp.kt`) | OK — LazyColumn messages, voice↔chat toggle button ("💬"/"🎤 Baat") maujood, Scaffold padding lagu | Menu end-marker (§3); title renames (§4); manifest adjustResize (§10) |
| Main Menu dropdown | OK — 46 items, 5 sections, M3 internal scroll; gap sirf end ka pata nahi chalta | End-marker item |
| Settings + sub-screens | OK — LazyColumn + section headers maujood | Koi code change nahi |
| Appearance | OK — verticalScroll + header; P1 toggles default OFF barkaraar | Sirf outer padding hatana (§1) |
| SD-8 Settings | OK — verticalScroll + headers ("8.3 Languages"…) | Sirf outer padding hatana (§1) |
| FavSongs | OK — verticalScroll + header + "Favourites (n):" | Sirf outer padding hatana (§1) |
| Routines | OK — scroll + headers ("Meri routines", "Mere macros") | Koi change nahi |
| Permissions | OK — scroll + headers ("App permissions", "Background") | Koi change nahi |
| Builder | OK — verticalScroll + header; gated message ("Builder abhi is plan me nahi hai") behavior hai, design nahi — chhedna nahi | Sirf outer padding hatana (§1); entry ki jaanch planner coordinator ke paas hai |
| Backup (Settings inline) | OK — outer Settings LazyColumn scroll karta hai | Sirf outer padding hatana (§1) |
| Home | GAP — plain Column, lambi content chhoti screen par cut ho sakti hai | verticalScroll jodo |
| Avatar | GAP (halka) — plain Column, 5 styles; badi font par overflow risk | verticalScroll jodo |
| PC Link | GAP — plain Column + apna 16dp (double padding 32dp) | verticalScroll jodo + apna padding hatao |
| Scan / CameraLook / Face / Whiteboard | OK — preview/canvas fill, scroll nahi chahiye | Koi change nahi |
| Lock (PIN gate) | OK — chhota gate, fit hota hai | Koi change nahi |
| IncomingCallActivity | OK — centered Column, 24dp padding, message + 2 buttons | Koi change nahi |
| SetupWizard | OK — LazyColumn + "Step X / 9" header; standalone hai isliye apna 16dp rahega | Koi change nahi |
| Notifications | OK — header + buttons | Koi change nahi |

## §12. Phone visual-test checklist (Phase 10, har screen ka screenshot-check)

Har screen par: (1) title header dikhe, (2) saare sections header ke
saath divided dikhen, (3) neeche tak scroll karke aakhri option dikhe,
(4) koi text kata hua nahi, (5) koi button overlay ke peeche nahi.
Menu par: poora scroll karke aakhri item "— Sab options khatm —" dikhe.
Keyboard test: chat me keyboard kholo — input field keyboard ke upar
dikhe. Charm ON karke: Flora screens par charm kisi option ko nahi
dhakta (charm doosre apps par hai, Flora screen par nahi).
