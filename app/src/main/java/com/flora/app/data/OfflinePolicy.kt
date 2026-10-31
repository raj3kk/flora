package com.flora.app.data

/**
 * P3-09 — Offline / weak-network mode (policy, BasicTalk ke upar).
 *
 * BasicTalk offline HAVE hai. Ye file baaki kaam ka policy deti hai:
 *
 * - Offline chalnewale kaam: wake + basic on-device tools.
 * - LLM/server wale kaam offline NAHI chalte — aur wo "net aate
 *   hi" queue me NAHI atakte. User ko saaf bataya jayega ki ye
 *   kaam ruk gaya hai, net aane par use dobara kehna hoga.
 *   Chupchaap queue karke baad me chalana silent action hota —
 *   isliye mana hai.
 * - Weak network par jhootha "no internet" nahi: jab offline
 *   confirm na ho to message "judav kamzor" wala hota hai.
 */
object OfflinePolicy {

    enum class WorkClass { OFFLINE_OK, NEEDS_NET }

    /** On-device kaam jo bina net ke chalte hain. */
    val OFFLINE_WORKS: Set<String> = setOf(
        "wake", "basic-talk", "local-reminder-read", "local-notes-read",
        "torch", "volume", "battery-read", "time-read"
    )

    fun classify(work: String): WorkClass {
        return if (work in OFFLINE_WORKS) WorkClass.OFFLINE_OK else WorkClass.NEEDS_NET
    }

    /** LLM kaam offline mile to user ko ye saaf message milega. */
    fun offlineBlockedMessage(work: String): String {
        return "Ye kaam ('$work') bina internet ke nahi ho sakta. " +
            "Ye queue me nahi atka hai — net aane par dobara kehna hoga, " +
            "tabhi chalega."
    }

    /** Weak network par — confirm kiye bina "no internet" mat kaho. */
    fun weakNetworkMessage(): String {
        return "Judav kamzor lag raha hai — dobara try karein. " +
            "Ye pakka 'no internet' nahi hai."
    }
}
