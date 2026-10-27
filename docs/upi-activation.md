# Flora UPI activation (deep link first, full integration later)

Flow (server-authoritative, app ke word par bharosa nahi):
1. User app me Pro (₹199/month) chunta hai.
2. App server se payment link maangta hai — server admin panel me set
   UPI VPA + amount 199 + note me user/tenant ID ke saath
   `upi://pay?pa=<ADMIN_VPA>&am=199&tn=Flora-Pro-<user_id>` banata hai.
   ADMIN_VPA kabhi repo/code me hardcode NAHI hoga — admin panel setting se aayega.
3. User apne UPI app me pay karta hai, phir app me UTR deta hai.
4. Admin UTR + amount + note match karke approve karta hai →
   server licences table me pro licence active karta hai (device bind).
5. App har protected action se pehle licence-verify Edge Function se
   plan check karta hai.

Full payment integration (provider baad me chuna jayega, alag approval
ke saath) aane par webhook verification + receipt is flow me judenge.
