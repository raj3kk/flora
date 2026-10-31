package com.flora.app.data

/**
 * P3-01 — Catcher full-coverage hooks, ek jagah.
 *
 * Har hook sirf [CatcherEvents] me ek sanitized action line likhta hai:
 * kabhi throw nahi, kabhi behaviour nahi badalta, kabhi auto-upload
 * nahi, kabhi chat full text nahi, kabhi secret/UTR-poora/email nahi
 * (CatcherSanitize aakhri safety net hai, pehla darwaza nahi —
 * callers ko bhi detail me sensitive value kabhi nahi bhejni hai).
 *
 * COVERED pehle se (yahan dobara nahi): crash, BrainClient fail,
 * plan-step fail, system STT/TTS fail, FCM token-sync fail.
 * Ye file NOT-covered surfaces ke hooks deti hai:
 *
 * - DB/storage errors: stores jo pehle silently swallow karte the,
 *   unke catch block se [dbError]/[storageError] bulao.
 * - Network history: sirf snapshot tha — [networkError] har
 *   failed call ka safe code darj karta hai (URL kabhi nahi).
 * - Lanes: Deepgram/Kokoro/wakeword/mic — [laneError].
 * - Features: builder/browser/routines/reminders/triggers/markets/
 *   docs/pictures/driving/typing/keys/search/home-sync/WhatsApp-
 *   reply — [featureError]. Ye sirf failure record karta hai,
 *   feature ka behaviour nahi badalta.
 * - Snapshot se bahar permission denials: [permissionDenied].
 * - Licence-client/gate errors: SIRF record ([licenceRecord]) —
 *   Round 3 licence domain deliberately untouched tha, isliye
 *   yahan bhi licence behaviour me koi badlav nahi.
 * - Server-side surfacing: Catcher app-only hai — server error
 *   sirf tab aayega jab app use BrainClient/API hook se record
 *   kare ([serverSurfaced]); server ka raw body kabhi nahi.
 * - FCM delivery failure: on-device se capture NAHI ho sakta by
 *   design — isko implement karne ki koshish mat karo. Sirf
 *   panel-side evidence (ZP-F13) note hota hai ([fcmDeliveryNote]).
 *
 * Wiring note (fixer/tester ke liye): call sites ki list
 * docs/FLORA_PART3_IMPLEMENTATION.md me hai. Har hook ka
 * default sink CatcherEvents hai; tests [sink] override karte hain.
 */
object CatcherHooks {

    /** Test seam — production me hamesha CatcherEvents.record. */
    internal var sink: (String, String) -> Unit = { category, detail ->
        CatcherEvents.record(category, detail)
    }

    private fun emit(category: String, detail: String) {
        try {
            sink(category, detail)
        } catch (e: Exception) {
        }
    }

    /** Features jin par [featureError] lagta hai (plan P3-01 list). */
    val FEATURES: Set<String> = setOf(
        "builder", "browser", "routines", "reminders", "triggers",
        "markets", "docs", "pictures", "driving", "typing", "keys",
        "search", "home-sync", "whatsapp-reply"
    )

    /** Lanes jin par [laneError] lagta hai. */
    val LANES: Set<String> = setOf(
        "deepgram", "kokoro", "wakeword", "mic", "system-stt", "system-tts"
    )

    fun dbError(store: String, op: String, code: String) {
        emit("DB", "store=$store op=$op code=$code")
    }

    fun storageError(op: String, code: String) {
        emit("STORAGE", "op=$op code=$code")
    }

    /** [kind] jaise "timeout"/"http-5xx"/"no-route" — URL kabhi nahi. */
    fun networkError(lane: String, kind: String, code: String) {
        emit("NET", "lane=$lane kind=$kind code=$code")
    }

    fun laneError(lane: String, code: String) {
        emit("LANE", "lane=$lane code=$code")
    }

    fun featureError(feature: String, step: String, code: String) {
        emit("FEATURE", "feature=$feature step=$step code=$code")
    }

    /** Sirf permission ka naam + surface — user data kabhi nahi. */
    fun permissionDenied(permission: String, surface: String) {
        emit("PERMISSION", "denied=$permission surface=$surface")
    }

    /**
     * Licence-client/gate error — SIRF record. Is function se
     * licence ka faisla, gate ya retry behaviour kabhi nahi badalta.
     */
    fun licenceRecord(code: String) {
        emit("LICENCE", "code=$code")
    }

    /** Server error sirf safe code ke saath (raw body kabhi nahi). */
    fun serverSurfaced(kind: String, code: String) {
        emit("SERVER", "kind=$kind code=$code")
    }

    /**
     * FCM delivery failure on-device se capture nahi ho sakta.
     * Ye sirf ye note darj karta hai taaki Catcher report padhne
     * wala galat daava na kare — asli evidence panel-side (ZP-F13) hai.
     */
    fun fcmDeliveryNote() {
        emit(
            "FCM",
            "delivery failure not capturable on-device; panel-side evidence only (ZP-F13)"
        )
    }
}
