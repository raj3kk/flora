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
                "1. Jo data sirf aapke phone par rehta hai: Yaadein, Journal, " +
                    "Documents, Routines, Jobs, Chains, SOS contacts, Notifications " +
                    "aur Auto Log encrypted store me save hote hain. Settings aur " +
                    "API keys sirf is phone par app ke private store me rehte hain, " +
                    "kisi server par save nahi hote aur kisi backup me nahi jaate. " +
                    "(Key sirf tab us provider ko pehchaan ke liye jaati hai jab " +
                    "aap usse baat karte hain — point 4 dekho.)"
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
                    "sirf plan/status jaanne ke liye."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "4. Baat karna (AI): jab aap Flora se baat karte hain, tabhi aapka " +
                    "sawaal + aakhri 20 yaadein (context ke liye) us AI provider ko " +
                    "jaate hain jiski key aapne Settings me daali hai (Gemini/Groq). " +
                    "Key na ho to Flora bina-key tools se jawab deti hai, tab kuch " +
                    "server par nahi jaata. Aapki poori yaadein kabhi upload nahi hoti."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "5. Khoj, mausam, market, photo dekhna: jab aap poochhte hain tabhi " +
                    "aapka sawaal us free service ko jaata hai (DuckDuckGo, Open-Meteo, " +
                    "Yahoo Finance, Tavily) — sirf utna hi jitna poochha gaya. Image " +
                    "banane par aapka prompt Pollinations ko jaata hai, bani hui photo " +
                    "sirf aapki gallery me save hoti hai. Camera " +
                    "se dekhi hui photo sirf tab Gemini ko jaati hai jab aap 'camera " +
                    "dekho' ya 'screen dekho' bolte hain."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "6. Awaaz sunna: Mic sirf tab jab aap Mic dabate hain. Continuous mic " +
                    "ON ho to background me sunta hai (notification hamesha dikhegi). " +
                    "Wake Word ON ho to 'Hey Flora' sunne ke liye mic chalu rehta hai " +
                    "(audio kabhi save/server par nahi jata). Deepgram key daali ho to " +
                    "aapki awaaz text banane ke liye Deepgram ko jaati hai, warna " +
                    "system wali sunne ki suvidha chalti hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "7. Browser: in-app browser me aap jo site kholte hain, wo site " +
                    "aapke phone se seedha us website ko dikhegi (jaise kisi bhi " +
                    "browser me). Browser history sirf is phone par rehti hai. " +
                    "Download aapke confirm karne par hi hota hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "8. Push: FCM push ke liye aapka device token + device ID Flora ke " +
                    "Supabase ko jaata hai (sirf tab jab aapne website account link " +
                    "kiya ho). Push ka text kabhi kisi teesre ko nahi becha jaata."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "9. Camera: camera sirf Scan/Face screen par, aapke permission " +
                    "ke baad khulta hai. Scan ki photo sirf aapke phone ki gallery " +
                    "(Pictures/Flora) me save hoti hai. Face screen chehra sirf isi " +
                    "phone par match karti hai — photo server par kabhi nahi jaati."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "10. Calls/SMS: Auto Call/SMS sirf aapke ek confirmation ke baad " +
                    "hote hain, aur sirf us number par jo aapne likha hai. Flora " +
                    "kabhi chupke se call, SMS ya post nahi karti."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "11. Retention: Journal entries, Memories me date wali entries aur " +
                    "Auto Log 30 din baad app-start par auto-delete hote hain. Bina " +
                    "date wali purani yaadein manually delete karni hongi (unko " +
                    "auto-delete kabhi nahi karega). Licence aur payment ke record " +
                    "(UTR, amount) alag se rakhe jaate hain."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "12. Bachchon ki suraksha: Flora me koi 18+ ya explicit content nahi hai. " +
                    "Romantic mode sirf baat karne ka tone hai, explicit baat nahi. " +
                    "Flora caring aur professional tareeke se baat karti hai."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("13. Support: flipify.com@gmail.com par sampark kijiye.")
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
                "1. Free plan: roz 10 minute baat, app me maujood saare tools. Pro plan: " +
                    "₹199/month — unlimited baat, saare tools, FCM push " +
                    "(server live hai; push ke liye admin + phone ka token " +
                    "sync zaroori hai). Live wallpaper aur continuous mic " +
                    "isi version me maujood hain (wallpaper system chooser se lagta hai)."
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
