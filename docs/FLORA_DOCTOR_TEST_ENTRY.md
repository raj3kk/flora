# B4-DOCTOR phone test entry (Phase 10 checklist — koi pass/fail daava nahi, jab tak phone par na chale)

## Sample project (6 categories, jaanboojh kar daale hue issues)
Builder me naya project banao, ye files likho (koi asli key kabhi nahi — synthetic shapes hi):
1. `index.html` — ek dead button (bina onclick), ek dead link (`href="#"`), ek http link, ek iframe `allow="camera"`.
2. `app.js` — ek 6+ line ka dead function (kahin call nahi), ek `apiKey = "..."` wali line (synthetic value), ek unbalanced-brace wali file alag se.
3. Compliance: corpus khaali hai, isliye screen par "corpus pending" dikhna chahiye — jhootha compliance scan nahi.

## Steps
1. Menu > Doctor kholo > project chuno — khulte hi health check scan dikhe (read-only, issues ki ginti).
2. "Scan karo" dabao — har category ka kam se kam ek issue dikhe; secret wale issue me value kabhi na dikhe (sirf file:line + pattern naam).
3. Kisi http issue par "Fix karo" dabao — https ho jaye, baaki lines na chhedein; phir Verify me rescan ginti kam ho.
4. Secret-line wale issue par "Fix karo" dabao — confirm dialog aaye; "Nahi" par kuch na badle; "Haan" par line hate + rollback versions me naya snapshot aaye.
5. Rollback: kisi file ka purana version "wapas lao" dabao — content wapas aaye, dobara scan chale.
6. Pre-deploy gate (planner se): jis deploy files me secret-shape line ho, VERCEL_DEPLOY step chalao — deploy RUKE + wajah me file:line + pattern dikhe, deploy create na ho.
7. Allowlist: planner se aise repo/project par push/deploy karo jo builder se bana hi nahi — refuse + saaf wajah aaye.
8. Planner screen se "Doctor kholo" button — Doctor route khule (sirf navigation).
9. Auto Log me `[doctor:<projectId> ...]` entries dikhein; Room doctor_events me bhi (durable proof).
10. Builder preview WebView: http page ke mixed content par block dikhe (hardened settings).

## Honest limits
- Unit tests (`DoctorScannerTest`) CI par execute nahi hote (CI sirf assembleDebug) — local `./gradlew :app:testDebugUnitTest` JDK machine par chalana baaki hai.
- Compliance scan tabhi chalega jab corpus mile; tab tak pending state hi sahi jawab hai.
- Failure-kind accessor session-scoped hai (Room step row me kind persist nahi hota) — app restart ke baad kind "session me nahi" dikhega, failed-steps list Room se phir bhi milegi.
