package com.flora.app.data

/**
 * P3-02 — Crash reporting + health check + status page (app side).
 *
 * Server-side health endpoint Part 2 team ka scope hai; ye file
 * app-side status-page model + messaging rules deti hai:
 *
 * - Weak network par bhi status page khulta hai (cached/last-known
 *   state dikhta hai, khaali screen nahi).
 * - Jhootha "no internet" kabhi nahi: jab offline hona CONFIRM
 *   na ho to message "status pata nahi" hota hai, "no internet" nahi.
 * - Crash log privacy: report me personal data nahi hota; user ko
 *   saaf message milta hai; koi crash na ho to khaali screen ki
 *   jagah saaf "sab theek" message hota hai.
 * - Auto-upload kabhi nahi — status sirf dikhta hai; share/copy
 *   sirf user-tap par (Catcher rules wahi).
 */
object HealthStatus {

    enum class Overall { OK, DEGRADED, DOWN, UNKNOWN }

    data class ComponentStatus(val name: String, val state: Overall, val message: String)

    /** Status page kabhi khaali nahi — hamesha kam se kam ye line. */
    const val EMPTY_STATE_MESSAGE =
        "Abhi koi crash report nahi hai — sab theek chal raha hai."

    const val CRASH_PRIVACY_MESSAGE =
        "Crash report me aapka personal data, chat text ya koi key nahi hoti — " +
            "sirf ye ki kaunsa hissa fail hua. Ye report phone par hi rehti hai, " +
            "kahin upload nahi hoti."

    /**
     * Network state ka imaandaar message.
     * [confirmedOffline] sirf tab true jab system ne offline confirm kiya ho.
     * [lastCheckOk] null = kabhi check hi nahi hua.
     */
    fun networkMessage(confirmedOffline: Boolean, lastCheckOk: Boolean?): String {
        return when {
            confirmedOffline -> "Internet nahi mil raha hai — offline kaam chalenge, " +
                "baaki kaam net aate hi dobara karein."
            lastCheckOk == true -> "Server se judav theek hai."
            lastCheckOk == false -> "Server se judav me dikkat lag rahi hai — " +
                "dobara try karein, ye pakka 'no internet' nahi hai."
            else -> "Server ka status abhi pata nahi hai — weak network ho sakta hai."
        }
    }

    /** Cached last-known state se status page model (weak network par bhi). */
    fun statusPage(
        appState: Overall,
        serverState: Overall,
        serverMessage: String
    ): List<ComponentStatus> {
        return listOf(
            ComponentStatus(
                "App",
                appState,
                if (appState == Overall.OK) "App theek chal rahi hai."
                else "App me kuch gadbad dikhi hai — Catcher report dekhein."
            ),
            ComponentStatus(
                "Server",
                serverState,
                if (serverState == Overall.UNKNOWN) networkMessage(false, null)
                else serverMessage
            )
        )
    }
}
