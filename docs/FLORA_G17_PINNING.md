# G17 — Flora Domain Pinning (Phase 8)

## Kya hai
Certificate pinning: app Flora ke apne do domains se tabhi baat
karega jab server certificate ka public key (SPKI) pin set me
ho — taaki koi beech me (MITM) Flora ke licence/payment/data
calls ko doosre certificate se intercept na kar sake. Ye domain
kharidna/lock karna NAHI hai — sirf app ke andar ka check hai.

## Pins (LIVE certs se, 2026-10-29, openssl s_client)
| Domain | Leaf SPKI (base64 SHA-256) | Leaf expiry | Issuer |
|---|---|---|---|
| `keomyweghrmhqglaxmyv.supabase.co` | `B9zUXavotYYIs217h2OZnCqCTp2vB/2pNC1zDijcIAo=` | 22 Jan 2027 | Google WE1 |
| `flora-henna-alpha.vercel.app` | `xRUwi70J41GFIbtkDY38VXIAoWtbsGGG+LWfenCZh7k=` | 27 Nov 2026 | Google WR1 |
| Shared Google intermediate (dono chains) | `47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=` | — | — |

Check: **pehle** system ka default TrustManager chain validate
karta hai (CA trust + expiry), **phir** chain ke kisi cert ka
SPKI pin set me hona chahiye (leaf **ya** intermediate).
Hostname verification default hi chalta hai (chheda nahi gaya).
Pin fail = `CertificateException` = call fail — **kabhi silent
unpinned fallback nahi** (fallback pinning ko bekaar kar deta).

## Implementation — OkHttp CertificatePinner NAHI (deviation, wajah ke saath)
Task me OkHttp CertificatePinner likha tha, par grep se pata
chala: **OkHttp sirf `DeepgramStreamClient` me hai** (third-party
host `api.deepgram.com`) — Flora-domain traffic **HttpURLConnection**
par chalta hai. CertificatePinner un paths ko cover hi nahi karta,
isliye uska daava jhootha hota. Asli implementation:
**`core/PinnedTrust.kt`** — TLS layer par, `HttpsURLConnection`
ke `SSLSocketFactory` me custom `X509TrustManager`
(default chain validation + pin check). Naye clients (flags,
heartbeat, home-sync, Kokoro download) isi helper se chalte hain.

## Kahan laga (swap kiya gaya)
`LicenceClient.post`, `BrainClient` (proxy `getCode`/`postJson`),
`WebsitePublisher` (Supabase storage), `ModelDownloader`
(manifest + model download), `HomeSyncClient`, `FeatureFlags`,
`HeartbeatClient`, `KokoroModelManager`.
**Nahi chheda gaya (jaankari ke saath):** direct Google calls
(`BrainClient` Gemini discovery, `VisionClient` — user key se
seedha Google, Flora domain nahi), `SearchTools` (DuckDuckGo /
Open-Meteo), `ImageTools`, `DeepgramClient` (OkHttp, third-party
host) — ye Flora ke apne domains nahi hain, in par default
system trust hi chalta hai.

## Rotation plan (pin expire/rotate hone par kya hoga)
1. **Leaf rotation se app nahi tootegi** — jab tak Google ka
   intermediate wahi hai, intermediate pin check pass karega.
   (Leaf har ~90 din rotate hota hai — Let's Encrypt cycle.)
2. **Intermediate badle to app update chahiye:** har release se
   pehle live certs se pins refresh karo (`openssl s_client`
   → leaf pubkey → DER → SHA-256 → base64) aur `PinnedTrust.kt`
   me update karo. Bina refreshed pin ke release mat karo.
3. **Pin fail hone par user ko imaandaar message:** licence/
   link/sync calls fail honge (local free mode / retry message)
   — kabhi chupchaap unpinned connection nahi banega, aur
   kabhi jhootha "ho gaya" nahi dikhega.
4. **Expiry watch:** Vercel leaf **27 Nov 2026** ko expire hota
   hai, Supabase leaf **22 Jan 2027** ko. Intermediate pin ke
   chalte leaf expiry se app nahi rukegi, par phir bhi har
   release par pins re-verify karo (cert rotate hote hi).
5. **Edge variance risk:** Supabase/Vercel alag edge POP par
   alag leaf de sakte hain — intermediate pin is variance ko
   cover karta hai (same intermediate). Agar CA hi badal jaye
   (Google se koi aur CA), to pin fail hoga aur app update se
   hi theek hoga — ye fail-closed design hai, bug nahi.

## Kabhi mat karna
- Guessed pin ship karna (galat pin = app ka server band).
- Pin fail par silent fallback (MITM ka darwaza khul jayega).
- Creator/Maya ke domains ya pins copy karna (sirf Flora ke
  apne domains pin hain).
