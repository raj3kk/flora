# Flora full production audit — 2026-10-28 ~01:30 IST (evidence-based)

## Sabse badi khabar (verified)
- Fix build **PASS**: run 40572830526, commit a93fe59, artifact
  `flora-debug-apk` **54,275,866 bytes**, expired: false.
  Iska matlab v1.1 + v1.2 + event triggers ka poora code compile
  aur dex-merge ho gaya — heap fix + order fix dono kaam kar gaye.
- App version (local tree): versionCode 12, 1.2.0-rebuild.
- Screens (nav se verified, 14): Chat, Settings, Yaadein, Scan,
  Tools, SOS, Face, Docs, Connect, Privacy, Terms, Auto, Routines,
  Live Screen.

## Production-ready? ABHI NAHI — poora, lekin gates ke saath
| Gate | Status | Saboot |
| 1. Build PASS + APK verified | PASS (build) / PENDING (APK content) | Artifact size upar; APK andar se verify (dex classes) + phone install baaki |
| 2. Edge Functions live | PASS | licence-verify + usage-count, UUID test 200 free/free |
| 3. Vercel site public | PASS | flora-*.vercel.app 200, ₹199 dikhta hai |
| 4. Modules + phone test | PARTIAL | Modules code me hain; phone par ek bhi test nahi hua |
| 5. Privacy/Terms + FCM | PARTIAL | Screens ban gayi; phone verify + FCM baaki |
| 6. Backup restore test | FAIL (pending) | Kabhi restore karke dikhaya nahi |
| 7. Final backup + SHA | PARTIAL | Backup 22:43 IST 27 Oct ka hai; naya final backup baaki |

Production-ready ka daava tabhi jab gate 1 (APK verify) + 4 (phone
test) + 6 (restore test) PASS hon. Usse pehle "production-ready"
bolna jhooth hoga.

## Plan thik hai? HAAN — detailed hai, aur aaj 2 naye gaps mile
Plan (master plan v2 + full missed matrix + missed audit) video ke
124 + screenshots + shorts sab cover karta hai. Aaj ke audit me 2
naye gaps mile jo plan me nahi the — dono v1.3 me add kar diye:
1. **Free talk limit enforce nahi hai**: TALK_SECONDS track karne
   ka code hai, par usko kabhi call hi nahi kiya jaata — na record,
   na 600 sec/day cap check. Matlab Free = 10 min/day abhi kagaz
   par hai, code me nahi.
2. **Data export / delete-all nahi hai**: Memories me ek-ek
   edit/delete hai, par poora data export ya sab delete ka button
   nahi — jabki Privacy me 30-day retention ka vaada hai.
3. **Licence gating partial hai**: Settings se server check hota
   hai, par Pro features par har action se pehle server check ka
   enforcement code me poora nahi dikha.

## Features — kya implement ho gaya, kya baaki hai
HO GAYA: Chat + Brain (Gemini→Groq) + system TTS/mic, Memories
(add/edit/delete), Scan photo save + on-device text-read, SOS
(3 contacts + auto SOS), Docs, currency/dictionary/weather/search
tools, Connect call/SMS (ek confirmation), Auto (koi bhi app,
Accessibility click/scroll/type/swipe), Routines auto-run, Live
Screen vision (frame + Gemini vision), Call announce, Event
announce (charger/battery), Gaana command, Privacy/Terms screens,
UPI deep link + UTR note, Supabase licence check, Vercel site.
BAAKI (v1.3+): default-dialer call answer+talk, continuous live
screen, driving mode, Flora personas, themes/orb/3D/live wallpaper,
Market/Marketing/Website-Coding/Study tools, journal/mood, chat
attachments, optional key fields (Maps/Tavily/Brave/SerpAPI),
multi-key rotation, voice-match, face-match + gallery face scan +
watchman + touch guard, WhatsApp auto-reply/alert (risk ki wajah
se sirf intents tak), email read/reply, admin panel + payment
webhook (server UTR verify), licence change-limit rule (15 din),
talk-limit enforcement, export/delete-all, FCM, wake word.

## HOLD par kya hai (baad me karenge) — wajah + kya chahiye
- FCM wake: user ka Firebase project (google-services.json +
  server key) chahiye.
- Wake word: phrase decision + consent recordings/dataset chahiye.
- Dialer call-talk: user ko Flora ko default dialer banana hoga
  (role decision) + phone par test.
- Voice-match: user ke ≤10 consent samples + model step.
- Face-match: embedding model + consent enrolment.
- Payment webhook: admin UTR server verify (admin flow decision).
- Phone test + APK delivery + backup restore: user ka phone.
- Play Store: Flora sideload hi rahegi (user ka pehle ka
  sideload decision); Play Store upload ka koi plan nahi.
- EXCLUDED (hold nahi, kabhi nahi): creator copy, silent
  dekhna/record/post, hard God-Mode, 18+/romantic, payment
  auto-confirm, screen server par bhejna.

## Aage kya hoga (kram)
v1.3 (upar ke 3 naye gaps + dialer/continuous/driving) → build
PASS → phone par install + har screen ka test + DB check → backup
restore test → final backup + SHA → tabhi "production-ready"
ghoshna. Usse pehle har daava "code me ban gaya, phone test
baaki" ke saath aayega.
