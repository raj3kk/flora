package com.flora.app.data

/**
 * Catcher report builder (in-app Catcher, G2) — pure, unit-testable.
 *
 * Ek hi full copy-able text banata hai (§3 format). Caller saara
 * input pehle se cap karke deta hai (crash max 4000 chars via
 * CrashLogReader, events max 50, Auto Log max 30) — builder khud
 * koi IO ya server call nahi karta. Aakhri pass me poori report
 * dobara sanitize hoti hai (CatcherSanitize) — taaki kisi section
 * se koi secret galti se bhi na nikle.
 */
object CatcherReport {
    fun buildReport(
        appLine: String,
        androidLine: String,
        timeLine: String,
        licenceLines: List<String>,
        autoMode: Boolean,
        fcmStatus: String,
        note: String,
        crashText: String,
        events: List<String>,
        autoLogLines: List<String>,
        systemLines: List<String>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("FLORA PROBLEM REPORT")
        sb.appendLine("---------------------")
        sb.appendLine("App: $appLine")
        sb.appendLine("Android: $androidLine")
        sb.appendLine("Time: $timeLine")
        if (licenceLines.isEmpty()) {
            sb.appendLine("Licence: pata nahi (check nahi hua)")
        } else {
            licenceLines.forEach { sb.appendLine(it) }
        }
        sb.appendLine("Auto Mode: ${if (autoMode) "ON" else "OFF"}")
        sb.appendLine("FCM: ${if (fcmStatus.isBlank()) "—" else fcmStatus}")
        sb.appendLine("User note: ${if (note.isBlank()) "—" else note}")
        sb.appendLine()
        sb.appendLine("[CRASH — aakhri record]")
        if (crashText.isBlank()) {
            sb.appendLine("Koi crash record nahi hai.")
        } else {
            sb.appendLine(crashText.take(4000))
        }
        sb.appendLine()
        sb.appendLine("[ERROR EVENTS — aakhri ${events.size}]")
        if (events.isEmpty()) {
            sb.appendLine("Koi error event nahi hai.")
        } else {
            events.forEach { sb.appendLine(it) }
        }
        sb.appendLine()
        sb.appendLine("[AUTO LOG — aakhri ${autoLogLines.size}]")
        if (autoLogLines.isEmpty()) {
            sb.appendLine("Koi auto kaam nahi hua hai.")
        } else {
            autoLogLines.forEach { sb.appendLine(it) }
        }
        sb.appendLine()
        sb.appendLine("[SYSTEM STATE]")
        if (systemLines.isEmpty()) {
            sb.appendLine("System state padha nahi ja saka.")
        } else {
            systemLines.forEach { sb.appendLine(it) }
        }
        return CatcherSanitize.sanitize(sb.toString())
    }
}
