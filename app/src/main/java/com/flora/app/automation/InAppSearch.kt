package com.flora.app.automation

import kotlinx.coroutines.delay

/**
 * Phase 6 — Foreground-app context carryover.
 *
 * App khulne ke baad agla voice command usi app ke context me
 * samjha jaaye (jaise khule hue YouTube me "X search karo").
 * Phase 1 ka verified-open yahan kaam aata hai: jab Flora koi
 * app verified kholti hai to uska package + label + waqt yahan
 * darj ho jaata hai. Carryover kabhi guess par nahi chalega:
 * plain "X search karo" sirf tab in-app jaayega jab saamne
 * wali app sach me wahi ho jo Flora se kholi gayi thi; warna
 * purani research-search branch chalegi (behaviour same).
 * Explicit form ("is app me X search karo") me target app
 * dobara verified kholi jaati hai, phir search hota hai.
 */
object ForegroundAppContext {
    @Volatile
    var lastOpenedPkg: String? = null
        private set

    @Volatile
    var lastOpenedLabel: String? = null
        private set

    @Volatile
    var lastOpenedAtMs: Long = 0L
        private set

    fun noteOpened(pkg: String, label: String) {
        lastOpenedPkg = pkg
        lastOpenedLabel = label
        lastOpenedAtMs = System.currentTimeMillis()
    }

    fun matchesForeground(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        return pkg == lastOpenedPkg
    }
}

sealed class InAppSearchOutcome {
    object Done : InAppSearchOutcome()
    object NoSearchBox : InAppSearchOutcome()
    object TypedOnly : InAppSearchOutcome()
}

object InAppSearch {

    suspend fun searchInApp(service: FloraAutomationService, query: String): InAppSearchOutcome {
        service.clickText("Search")
        service.clickText("Khoj")
        delay(800)
        val typed = service.setTextOnFirstEditable(query) || service.typeIntoFocused(query)
        if (!typed) return InAppSearchOutcome.NoSearchBox
        delay(500)
        val submitted = service.clickText("Search") || service.clickText("Go") || service.clickText("Khoj")
        return if (submitted) InAppSearchOutcome.Done else InAppSearchOutcome.TypedOnly
    }
}
