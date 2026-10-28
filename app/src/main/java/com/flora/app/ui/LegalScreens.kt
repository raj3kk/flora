package com.flora.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Privacy + Terms screens (v0.9): original Flora text, written for
 * what this app actually does — no copied policy from any other app.
 * If a feature changes, this text must be updated with it; a stale
 * policy is worse than a short honest one.
 */
@Composable
fun PrivacyScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Flora — Privacy Policy")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "1. Jo data sirf aapke phone par rehta hai: Yaadein, Documents, " +
                    "SOS contacts, settings aur API keys (Gemini/Groq/Supabase anon key) " +
                    "sirf is phone par DataStore me save hote hain. Ye kisi server par " +
                    "nahi bheje jaate, kisi backup me nahi jaate."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "2. PIN: sirf AndroidKeyStore me encrypted (AES/GCM) rehta hai. PIN " +
                    "kabhi file, message, backup ya server par nahi likha jaata, aur " +
                    "kabhi log nahi hota."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "3. Licence check: jab aap Licence check dabate hain, tabhi aapka " +
                    "user ID aur device ID Flora ke Supabase Edge Function ko jaata hai, " +
                    "sirf plan/status jaanne ke liye. Iske alawa koi chat, yaad ya " +
                    "document server par nahi bheja jaata."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "4. Camera/mic: camera sirf Scan/Face screen par, aapke permission " +
                    "ke baad khulta hai. Scan ki photo sirf aapke phone ki gallery " +
                    "(Pictures/Flora) me save hoti hai. Face screen koi photo save, " +
                    "send ya match nahi karti. Mic sirf tab jab aap Mic dabate hain."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "5. Calls/SMS: Auto Call/SMS sirf aapke ek confirmation ke baad " +
                    "hote hain, aur sirf us number par jo aapne likha hai. Flora " +
                    "kabhi chupke se call, SMS ya post nahi karti."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "6. Retention: Journal entries 30 din baad app-start " +
                    "par auto-delete hoti hain. Memories me date wali " +
                    "entries 30 din baad auto-delete hoti hain; bina " +
                    "date wali purani entries manually delete karni " +
                    "hongi (unko auto-delete kabhi nahi karega). " +
                    "Licence aur payment ke record (UTR, amount) alag se rakhe jaate hain."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "7. Bachchon ki suraksha: Flora me koi 18+ ya romantic persona nahi hai. " +
                    "Flora caring aur professional tareeke se baat karti hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("8. Support: flipify.com@gmail.com par sampark kijiye.")
        }
    }
}

@Composable
fun TermsScreen() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Flora — Terms of Use")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "1. Free plan: roz 10 minute baat, lagbhag 15 tools. Pro plan: " +
                    "₹199/month — unlimited baat, saare tools, FCM push " +
                    "(server live hai; push ke liye admin + phone ka token " +
                    "sync zaroori hai). Live wallpaper aur conversation mode " +
                    "abhi available nahi hain — ye baad me aayenge, isliye " +
                    "inhne Pro ka hissa samajhkar mat khareedna."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "2. Payment: Pro ke liye UPI se payment kijiye. Payment ke baad " +
                    "UTR admin ko verify karwana hoga — tabhi licence active hogi. " +
                    "App khud kabhi payment approve nahi karti."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "3. Aapki zimmedari: jo number ya message aap Flora se bhejwate " +
                    "hain, uske liye aap zimmedar hain. Galat number par call/SMS " +
                    "jaane par Flora zimmedar nahi hai — confirm dialog me number " +
                    "dhyaan se check kijiye."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "4. Galat istemal: Flora ka istemal kisi ko pareshan karne, dhokha " +
                    "dene, ya kanoon todne ke liye mat kijiye. Aisa karne par licence " +
                    "band ki ja sakti hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "5. Availability: Flora ke jawab AI (Gemini/Groq) aur bina-key " +
                    "tools par depend karte hain. Network ya provider ki dikkat par " +
                    "jawab na mile to Flora imaandaari se bata degi, jawab gadhegi nahi."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "6. Badlaav: ye Terms/Privacy badal sakte hain. Bada badlaav hone " +
                    "par app me bataya jayega."
            )
        }
    }
}
