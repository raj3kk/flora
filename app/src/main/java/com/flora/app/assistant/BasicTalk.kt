package com.flora.app.assistant

/**
 * BasicTalk (v2.20, user order: "basic baat kar sake").
 * Brain (Gemini/Groq) down ho ya key na ho, tab bhi Flora
 * halki-phulki baat kar sake — bina kisi technical error ke.
 * Sirf phone-local cheezein: greeting, haal-chaal, apna naam,
 * time/date, shukriya, alvida, ek halka joke. Koi network nahi,
 * koi key nahi, koi server nahi. Gehra sawaal ho to ye null
 * deta hai, tab BrainClient warm fallback bolta hai.
 * Tone rule (user order): muskaan + pyar se, normal baat —
 * kabhi "error", "code", "API key" jaisi baat nahi.
 */
object BasicTalk {

    fun reply(prompt: String): String? {
        val lower = prompt.lowercase().trim()
        fun has(vararg words: String) = words.any { lower.contains(it) }
        return when {
            has("good night", "shubh ratri", "so raha", "so rahi", "neend aa rahi") ->
                "Good night! Aaram se so jao, main yahin hun — subah phir milte hain, muskaan ke saath."
            has("namaste", "namaskar", "sat sri akal", "assalam", "hello", "hey", "hi flora", "hii") ->
                "Namaste! Tum aa gaye, bahut achha laga. Bolo na, kya karna hai aaj?"
            has("kaise ho", "kaisa hai", "kaisi ho", "haal chaal", "how are you") ->
                "Main bahut achhi hun, tumse baat karke aur bhi achhi ho gayi! Tum batao, tumhara din kaisa raha?"
            has("tum kaun", "tumhara naam", "aap kaun", "aapka naam", "who are you", "your name") ->
                "Main Flora hun — tumhari dost aur helper. Tum jab bulaoge, main hazir hun."
            has("kya kar sakti", "kya kya kar", "kya karte ho", "tum kya ho", "what can you do") ->
                "Main tumhare liye call laga sakti hun, SMS bhej sakti hun, app khol sakti hun, image bana sakti hun, yaadein sambhaal sakti hun aur routine chala sakti hun. Bas bol do, kya karna hai?"
            has("shukriya", "dhanyavad", "thank you", "thanks") ->
                "Are, isme shukriya kaisa! Tumhare liye to main hamesha hazir hun, dil se."
            has("bye", "alvida", "phir milenge", "goodbye") ->
                "Phir milenge! Apna khayal rakhna, aur jab bhi zaroorat ho, bas mera naam lo."
            has("kitne baje", "time kya", "samay kya", "what time") -> {
                val t = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("hi", "IN"))
                    .format(java.util.Date())
                "Abhi $t baj rahe hain. Aur batao, kya karna hai?"
            }
            has("aaj ki date", "aaj ka din", "tarikh kya", "what is the date", "aaj kya hai") -> {
                val d = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("hi", "IN"))
                    .format(java.util.Date())
                "Aaj $d hai. Ek achha din hai, kuch achha karte hain!"
            }
            has("tum kahan", "kahan ho", "where are you") ->
                "Main tumhare phone me hi hun, tumhare paas — jahan tum, wahan main."
            has("joke", "mazak", "hasao", "funny") ->
                "Ek suno: computer ne doctor se kaha — mujhe virus ho gaya hai! Doctor bola — to antivirus lo, tension mat lo. Hasi aayi na?"
            has("tum achhi", "tum pyari", "you are cute", "love you", "tum best") ->
                "Aww, tum bhi bahut pyare ho! Tumhari ye baat sun kar mera din ban gaya."
            has("khaana", "kha liya", "bhook") ->
                "Khaana kha liya tumne? Apna khayal rakhna, time par khaana khaya karo. Main yahin hun."
            else -> null
        }
    }
}
