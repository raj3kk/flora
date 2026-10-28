# Flora FCM test entry (v2.2) — phone par check karne ke steps

Scope: FCM client wiring only (Firebase project flora-f22a7, package
com.flora.app). Server se push bhejna is item me NAHI hai — server
function + push_tokens table alag step hai. Isliye test sirf token
save + Firebase console se test push tak hai.

1. App kholo → Settings kholo → upar "Push: chalu (token sirf is
   phone par save hai)" dikhna chahiye. (Token value kabhi screen
   par nahi dikhegi — ye jaanboojh kar hai.)
2. App band karo (poori tarah, recent se hata kar) → Firebase
   console > Cloud Messaging se is device par test push bhejo →
   notification aani chahiye, tap karne par Flora khulni chahiye.
3. App khuli ho tab bhi test push bhejo → notification aani chahiye.
4. Internet OFF karke app kholo → app rukni nahi chahiye, koi crash
   nahi; internet ON karne par token dobara save hona chahiye.
5. Settings me "Push: token abhi nahi mila" dikhe to: internet
   check karo, app dobara kholo; phir bhi na mile to fail report me
   likho — token kabhi chat/message me mat bhejo.
6. Koi bhi push aane par koi silent action nahi hona chahiye — sirf
   notification dikhe, Flora khud kuch na kare jab tak user tap na kare.

PASS tabhi jab 1–3 pass hon; 4 me crash = FAIL (root fix + re-audit).
