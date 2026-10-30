# FLORA MARKETING 8.9 — SPEC (parity batch 2026-10-31, M6)

## Records se kya mila (verified, isse zyada daava nahi)

- Maya records me "Marketing" sirf do jagah naam ke roop me mila hai:
  side-menu modules list ("Tools: Maya Home, Market, Marketing") aur
  `MarketingAlarmReceiver` (exact-alarm receiver ka naam).
- Marketing tools ka content vyavhaar (kya karte the, kis ko, kis
  format me, kis channel par) records me **UNVERIFIED** hai.
- SD-8 plan 8.9 bhi isi ko SPEC-FIRST kehta hai: spec freeze
  pehle, tabhi build + gate.

## Is cycle ka faisla (spec freeze, build NAHI)

- Is spec me marketing ka koi vyavhaar invent nahi kiya gaya hai.
  Isliye M6 ka **koi code build nahi hua** — guess par marketing
  tool banana galat hota (kya bhejega, kisko bhejega, kis
  consent se — ye sab bina definition ke silent-action risk hai).
- M6 tabhi build hoga jab (a) records se verified vyavhaar mile,
  ya (b) user khud definition de: marketing ka matlab kya hai
  (copy draft? post schedule? campaign list?), kis channel par,
  kis consent ke saath.
- Jo Flora me pehle se hai aur marketing se takraata nahi: WhatsApp
  alert composer + group report (CommConsentScreen — user Send
  dabata hai), post history (PostHistoryStore). Ye M6 ka
  replacement nahi hain — sirf ye bataya ja raha hai ki Flora
  me consent-gated communication pehle se maujood hai.
