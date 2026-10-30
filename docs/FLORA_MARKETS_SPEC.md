# FLORA MARKETS — SPEC (parity batch 2026-10-31, M7)

## Records se kya mila (verified, isse zyada daava nahi)

- Maya records me "Market" sirf side-menu modules list me naam ke
  roop me mila hai. Koi markets-tool count, koi tool list, koi
  trading vyavhaar recover nahi hua.
- "~32 tools" wala daava **kabhi fact nahi** — is spec me bhi
  nahi, screen par bhi nahi.

## Flora-new v1 design (Maya copy nahi — Flora ka apna design)

- **Read-only watchlist.** User symbol jodta hai (jaise
  RELIANCE.NS, AAPL), screen Yahoo Finance lane se price/change
  dikhata hai (Flora ka maujooda `SearchTools.stockPrice` — bina
  key, bina kharcha).
- **Koi trading nahi:** buy/sell/order/advice is screen me kabhi
  nahi hoga. Price indicative hai — screen par saaf likha hai ki
  live price ki pushti exchange/app se karo.
- Symbols sirf is phone par save hote hain (WatchlistStore,
  DataStore JSON) — server par kabhi nahi.
- Network fail ho to imaandaar error line dikhti hai, koi
  invented price kabhi nahi.
