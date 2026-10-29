# Licence-Verify v2 — App Client (Phase 8 coordination)

Server function `licence-verify` LFMS server builder ke paas hai
(app is function ko **nahi** chhoota — boundary ka paalan).
Ye doc sirf app-side handling batata hai.

## Response shape (v2 contract)
`{valid, status, plan, features[], expires_at, bound_device_short, reason?}`
- `status`: `active` | `revoked` | `expired` | `superseded` |
  `bound_mismatch` (purani shape me sirf `plan` + `status` tha).
- `valid`: server ka faisla (true = licence maani jayegi).

## App-side semantics (`LicenceClient.verify()`)
- **Effective plan:** Pro sirf tab jab `valid == true` AUR
  `status == "active"` AUR response `plan == "pro"`.
  Revoked/expired/superseded/bound_mismatch par plan **free**
  kar diya jata hai — warna revoke ke baad bhi Pro features
  chalte rahenge (ye bug nahi hone diya gaya).
- **Backward compatible:** purani shape (koi `valid` field nahi)
  par `valid = (status == "active")` mana jata hai.
- `lastPlan` (usage-count ka pro check) effective plan se
  set hota hai — revoked user free cap me gina jayega.
- `features[]`, `expires_at`, `bound_device_short`, `reason`
  parse karke `LicenceStatus` me rakhe jate hain (khali ho
  sakte hain — UI khali par kuch extra nahi dikhata).

## Device binding rule (server ka faisla, app sirf maanta hai)
- Ek licence ek time me **ek device** par.
- **First claim bind karta hai** — pehle jis device se verify
  hua, wahi bind rahega.
- **Auto-rebind kabhi nahi** — app khud rebind ki koshish
  kabhi nahi karti; sirf `device_id` bhejti hai.
- **Rebind sirf admin panel se** hota hai.
- Purana "15 din ke baad transfer" wala text LicenceScreen se
  hata diya gaya hai (v2 rule se takrata tha).

## Imaandaar messages (generic error kabhi nahi)
`LicenceStatus.honestMessage()` — har status ka apna message:
| Status | Message |
|---|---|
| `revoked` | "Licence revoke ho gayi hai — Pro features band hain. Admin se sampark karo." |
| `bound_mismatch` | "Ye licence kisi aur device par lagi hai (device: \<short\>) — is phone par Pro nahi chalega. Rebind sirf admin panel se hoga, apne aap nahi." |
| `expired` | "Licence ki expiry ho gayi hai — Pro band hai, renew karo." |
| `superseded` | "Ye licence nayi licence se badal di gayi hai — nayi licence check karo." |
| `active` / `local_free` / `error` | koi message nahi (offline behavior bilkul wahi) |

## Kahan dikhta hai (teen surfaces)
1. **App-start:** licence verify ke baad kharab status ho to
   chat me message (sirf chat — voice greeting se takrao na
   ho isliye awaaz nahi).
2. **Header:** har screen ke upar wali licence line ke neeche.
3. **LicenceScreen:** status line ke neeche + expiry (ho to) +
   refresh button ka note (kharab status par wahi message).

## Honest limits
- Server function deploy hone tak app purani shape par chalega
  (backward compatible parsing) — v2 messages deploy ke baad
  hi dikhenge.
- `bound_device_short` server jo bhejega wahi dikhega — app
  usse koi device pehchanne ka daava nahi karti.
- Rebind/admin approve app se kabhi nahi hota (money + admin
  rule) — panel ka kaam hai.
