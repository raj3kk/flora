# B2 — Licence gate timeout bound: fresh-verify-vs-cache tradeoff (VM attempt, 2026-10-30)

**Samasya (tip evidence):** Har gated reply se pehle `LicenceGate.freshStatus`
`LicenceClient.verify()` chalata hai. `LicenceClient.post()` ke timeouts sab
callers ke liye ek jaise hain: connect 15_000 / read 15_000. Worst-case licence
stall per reply: unlinked user = 1 post = 30s tak; linked user + expired access
token = JWT refresh post (30s tak) + verify post (30s tak) = 60s tak, brain
shuru hone se pehle. (`SessionTokenCache` 55-minute reuse karta hai, isliye
expired-token case hi do-post wala hai.)

## Vikalp aur faisla

| Vikalp | Faisla | Wajah |
|---|---|---|
| (a) Cache wapas lagao (15-min ya session cache) | REJECTED | 013 ka faisla jaanboojh kar tha: direct lane (user ki apni keys) ka ekmatra gate app gate hai; server gate sirf proxy lane cover karta hai. Cache lagana revoke promptness ko kamzor karega. |
| (b) `withTimeout` se gate ko lapeto | REJECTED (akela kaafi nahi) | Blocking `HttpURLConnection` read coroutine cancellation ko nahi maanta — timeout ke baad bhi gate socket ke peeche atak sakta hai. Asli bound sirf socket timeout se milega. |
| (c) Gate-path ke posts par chhota socket timeout (verify/verifyWithKey) | CHUNA GAYA | Server reachable ho to fresh status waisa hi milega (013 semantics barkaraar — koi cache nahi, har gated use par fresh attempt). Sirf slow/unreachable case ka worst-case ghat-ta hai, jo pehle se maujood cached/local fallback par girta hai. |
| (d) Verify ko background prefetch se garam rakho | REJECTED | Gate ka faisla phir bhi seconds-purani status par hota, jo 013 ke letter ke khilaaf hai. |

## Numbers (spec ke chune hue, guess nahi)

- `LicenceClient.post()` ko timeout parameters milenge, defaults wahi
  15_000 / 15_000 — baaki callers ka behavior bilkul nahi badlega.
- `verify()` / `verifyWithKey()` gate timeouts pass karenge:
  connect 5_000 / read 8_000.
- Attempt 1 me refresh path ke timeouts defaults par rahenge
  (worst expired-token case 15+15+5+8 = 43s; typical case 8s tak).
  Agar re-audit me refresh dominate karta dikhe to follow-up: timeout ko
  cache ke through thread karna (alag attempt, alag gate).

## Pre-audit evidence (tip 4c17f535, guess nahi)

- `post()` callers (9 sites, koi 15s par nirbhar nahi): link (143),
  linkWithGoogle (170), refreshSession (194), refreshAccessToken (211),
  verify (269), verifyWithKey (292), countUsage (376), fetchServerUpi (391),
  syncPushToken (411). Sab aaj defaults istemal karte hain; defaults
  badle nahi, isliye inka behavior bilkul waisa hi rahega.
- `verify()` callers (sab ko 5s/8s bound milega — yahi scope hai):
  LicenceGate:135 (gate), FloraApp:859 (app-open verify),
  FloraApp:7162 (onLicence), LicenceScreen:105 (manual verify button),
  FloraBackupManager:659 (backup re-verify), AiBuildQuota:150 (builder
  quota check), BackgroundMicService:477 (cached plan verify).
- **Spec note correction:** FloraApp "Licence session renew" (FloraApp:872-891)
  `refreshSession` istemal karta hai, `verify` nahi — isliye renew path
  is change se BILKUL prabhavit nahi hoga (defaults par hi rahega).
  Renew background hai, fail par agli baar retry hota hai — waisa hi rahega.
- **Edge cold-start data:** Repo ke docs/code me Edge-function cold-start
  timing ka koi data nahi mila (sirf app cold-start ke zikr hain, jo
  alag cheez hai). Isliye 8s read spec ka chuna hua number hi rehta hai;
  agar Phase 10 me cold-start >8s dikhe to follow-up = read timeout
  badhana (alag gate, abhi guess par number badhana nahi).
- **Fallback waisa hi:** Timeout → `post()` null return karta hai (catch-all)
  → `verify()` error status → `freshStatus` null → `checkInner` last-known
  cached par girta hai (fail-open) → local cap. Chhota timeout sirf is
  fallback tak pahunchne ka waqt ghatata hai; koi crash, koi galat block
  nahi ho sakta (aaj unreachable-server par wahi fallback chalta hai).

## Scope boundaries (todna nahi)

- Koi naya cache nahi, koi plan change nahi, koi server change nahi.
- `FloraApp.kt` is change me nahi chhua jayega (sirf LicenceClient.kt).
- Brain chain ke timeouts (postJson 20s/30s, discovery GET 15s) B2 ka scope
  nahi hain.
