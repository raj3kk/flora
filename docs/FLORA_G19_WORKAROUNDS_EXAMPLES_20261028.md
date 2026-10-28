# G19 workarounds — detail me, example ke saath (2026-10-28)

CEO plan ke G19 section ka detail. Har item par: kya nahi ho
sakta, uske sabse kareeb legitimate raasta, aur ek concrete
example — taaki user dekh kar faisla kar sake.

## 1. Khud se permission grant — example: Shizuku

**Example (kaise chalega):**
1. User Play Store se Shizuku app install karta hai (free,
   open-source).
2. Developer options me "Wireless debugging" ON karta hai
   (Android 11+ par bina PC ke hota hai).
3. Shizuku khol kar "Start via Wireless Debugging" dabata hai,
   pairing code daalta hai — Shizuku chalu ho jata hai.
4. Ab Flora (Shizuku API se, user ki permission se) ADB-level
   command chala sakti hai, jaise:
   `pm grant com.flora.app android.permission.POST_NOTIFICATIONS`
   — matlab wo permission jo user ne dialog me nahi di, wo
   Shizuku ke zariye mil jati hai.
5. Reboot par Shizuku dobara chalana padta hai; user jab chahe
   Shizuku band kar sakta hai — tab Flora ke extra rights bhi
   band.

**Limits:** Shizuku root NAHI deta, sirf ADB-level rights deta
hai. Kuch permissions (jaise accessibility toggle, default
roles) isse bhi nahi milti — wo hamesha user ke tap se hi
milengi. Isliye Shizuku "sab kuch automatic" nahi hai, par
"bahut kuch automatic" hai.

**Doosra example — Device owner:** phone factory reset karo,
setup ke waqt QR scan se Flora ko device owner banao — phir
silent grant + policies possible hain. Keemat: poora phone
reset. Isliye ye sirf tab jab user khud chahe.

**Aaj ka default (bina Shizuku):** Flora sahi Settings screen
khol deti hai, user ek tap karta hai. Sabse surakshit, sabse
seedha.

## 2. MediaProjection grant — example

**Example:** tum "screen share chalu karo" bolte ho, Flora
MediaProjection dialog kholti hai, tum Allow dabate ho —
session tab tak zinda hai jab tak Flora ka process zinda hai.
Agar phone Flora ko maar de (battery saver), to agle process
me Allow dobara chahiye hoga. **Iska hal jo ban gaya hai:**
accessibility screenshot — service ON ho to "screen dekho"
bolte hi bina kisi dialog ke screenshot mil jata hai. Matlab
MediaProjection ki zaroorat hi khatam ho jati hai roz ke kaam
ke liye.

## 3. Call me TTS — example

**Example (speaker loop):** SIM call par Flora speaker par
bolti hai, phone ka mic us awaaz ko uthata hai, saamne wala
sunta hai. Par gunj (echo) ho sakti hai, aur awaaz halki bhi
ho sakti hai — isliye guarantee kabhi nahi di ja sakti.
**Example (VoIP — alag product):** agar call Flora ki apni
internet-call ho (Flora khud call ka ek taraf ho), to TTS
seedha audio stream me ja sakta hai — kyunki wahan koi SIM
audio path nahi hai. Ye SIM call ka fix nahi hai, naya
hissa hai.

## 4. WhatsApp call — example

**Example:** WhatsApp par incoming call aaye to Flora
accessibility se swipe karke call utha sakti hai (at most),
phir speaker loop se bol sakti hai. Par WhatsApp ki call
audio me TTS inject karne ka koi official raasta nahi hai.
**WhatsApp Business API ka example:** wo sirf business
messaging ke liye hai (template message, customer support) —
us se personal WhatsApp call nahi hoti. Isliye is item par
koi "jugaad" nahi hai, sirf swipe-answer + speaker loop.

## 5. Bina hosting ke public link — example

**Example (Flora bucket):** tum "website banao" bolte ho,
Flora HTML banati hai, phir Flora ke apne Supabase bucket
me upload karti hai — public URL ban jata hai, jaise
`.../flora-sites/mere-site/index.html`. Content rules
lagengi (galat content public nahi hoga).
**Example (user ka account):** GitHub Pages — tum ek baar
GitHub me login karte ho, Flora repo banati hai, Pages ON
karti hai — URL ban jata hai. Dono me server chahiye hota
hai; phone par HTML se sirf local preview + share milta hai.

## 6. Play-Store-safe automation — example

**Example:** Flora website se APK download karke install hoti
hai (sideload) — isliye accessibility automation chal sakta
hai. Agar Flora ko Play Store par daalna ho, to Google ki
policy ke hisaab se cross-app automation ka scope kaatna
padega (sirf apne app ke andar ka automation). Isliye Flora
private sideload hi rahegi — ye scope ka faisla hai, koi
technical kami nahi.
