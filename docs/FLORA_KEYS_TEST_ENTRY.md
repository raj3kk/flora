# Optional keys + multi-key rotation (v2.1) — phone test entry (per-item audit gate)

Keys audit ke teeno hisse me se (c) yahan taiyaar hai; (a) code
audit done hai, (b) CI result push ke baad aayega. Ye entry test plan
me merge hogi jab keys audit PASS ho jayega.

## Phone par check karne wale steps
1. Settings kholo — key fields me Gemini key 2, Groq key 2, Tavily,
   Brave, SerpAPI, Maps ke naye optional fields dikhne chahiye.
2. Gemini key 2 daal kar save karo, app band karke kholo — key
   field me value yaad rehni chahiye (password style me chhupi hui).
3. Sirf ek key se chat chalao — pehle wali key se jawab aana chahiye,
   rotation se koi farq nahi padna chahiye.
4. Tavily key daal kar Tools me search karo — result ke neeche
   "Source: Tavily" dikhna chahiye.
5. Tavily key hata kar search karo — wapas DuckDuckGo result aana
   chahiye, crash nahi.
6. Brave/SerpAPI/Maps key daal kar save karo — save honi chahiye,
   aur UI me saaf likha hona chahiye ki inka lane abhi nahi bana hai.
