package com.flora.app.data

/**
 * P3-15 — Per-release testing checklist (ek gate me bandha hua).
 *
 * Parts maujood the (CI + phone gate + server checks) par ek
 * single gate nahi tha. Ye file wo gate hai: har release par
 * saare BLOCKING items pass hone chahiye — phone-side + server-side
 * dono ke bina release nahi. Ye checklist code + CI + phone +
 * server ko ek jagah bandhti hai; Part 4 flow is gate ko chalata hai.
 */
object ReleaseChecklist {

    enum class Gate { CODE, CI, SERVER, PHONE }

    data class Item(val id: String, val gate: Gate, val description: String, val blocking: Boolean)

    val ITEMS: List<Item> = listOf(
        Item("rel-01", Gate.CI, "CI green (build + unit tests + drift check)", true),
        Item("rel-02", Gate.CODE, "Koi secret/key/password value commit me nahi", true),
        Item("rel-03", Gate.CODE, "Catcher rules intact (no auto-upload, sanitizer on)", true),
        Item("rel-04", Gate.SERVER, "Server live gates pass (licence/usage/push jahan lagu)", true),
        Item("rel-05", Gate.SERVER, "version.json + website wording accurate", true),
        Item("rel-06", Gate.PHONE, "Release APK phone par install + upgrade test pass", true),
        Item("rel-07", Gate.PHONE, "Voice lanes phone test pass (STT/TTS/wake)", true),
        Item("rel-08", Gate.PHONE, "Catcher report phone test pass (open-note-copy-crash)", true),
        Item("rel-09", Gate.PHONE, "Accessibility spot-check (labels + dark/light theme)", true),
        Item("rel-10", Gate.PHONE, "Backup/restore spot-check (fail par data na khoye)", true),
        Item("rel-11", Gate.CODE, "Changelog likha gaya, versionCode badha", true),
        Item("rel-12", Gate.PHONE, "User phone sign-off (bina iske release nahi)", true)
    )

    /** Saare blocking items pass hon tabhi release gate pass. */
    fun gatePass(results: Map<String, Boolean>): Boolean {
        return ITEMS.filter { it.blocking }.all { results[it.id] == true }
    }

    fun missing(results: Map<String, Boolean>): List<String> {
        return ITEMS.filter { it.blocking && results[it.id] != true }.map { it.id }
    }
}
