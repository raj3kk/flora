package com.flora.app.data

/**
 * P3-04 — Data delete + export (design + app-side contract).
 *
 * Rules (plan se):
 * - Delete/export se pehle user ka CONFIRM zaroori hai — bina
 *   confirm ke [DeleteExportBackend] delete se inkaar karta hai.
 * - Retention 30 din; chat memory is rule se EXEMPT hai
 *   (plan: chat memory exempt — use delete flow me zabardasti
 *   nahi ghaseeta jayega).
 * - Scope: app-side stores + Supabase dono. Supabase wali
 *   delete/export server-side backend se hogi — app sirf
 *   confirm + request bhejega, server ka faisla server ka hai.
 * - Delete irreversible hai — isliye ye file sirf contract +
 *   guard deti hai; asli UI wiring me confirm screen ke baad hi
 *   backend call hoga (call-site fixer/tester ke paas hai).
 *
 * Store naam maujooda app stores se liye gaye hain (koi nayi
 * table invent nahi ki gayi); Supabase table mapping live schema
 * se wiring ke waqt confirm hogi, yahan andaze se nahi likhi gayi.
 */
object DataDeleteExport {

    const val RETENTION_DAYS = 30
    const val CHAT_MEMORY_EXEMPT = true

    /** App-side stores jo delete/export scope me aate hain. */
    val APP_STORES: List<String> = listOf(
        "journal", "docs", "routines", "jobs", "memories",
        "reminders", "triggers", "watchlist", "post-history",
        "macros", "fav-songs", "sos", "notifications",
        "pending-tasks", "projects", "project-memory"
    )

    /** Chat memory alag hai — retention delete scope se bahar. */
    const val CHAT_MEMORY_STORE = "chat-memory"

    data class ExportBundle(val manifest: List<String>, val storeCounts: Map<String, Int>)

    data class DeleteResult(val ok: Boolean, val reason: String)

    /** Backend contract — asli Room/Supabase calls wiring ke waqt lagenge. */
    interface DeleteExportBackend {
        suspend fun exportAll(): ExportBundle
        suspend fun deleteAll(confirmed: Boolean): DeleteResult
    }

    /** Confirm ke bina delete kabhi nahi — guard, backend se pehle. */
    fun guardDelete(confirmed: Boolean): DeleteResult {
        return if (!confirmed) {
            DeleteResult(false, "delete refused: user confirmation missing")
        } else {
            DeleteResult(true, "confirmed — backend may proceed")
        }
    }

    /** Export manifest me sirf store naam + counts — content yahan nahi. */
    fun manifestFor(counts: Map<String, Int>): ExportBundle {
        return ExportBundle(
            manifest = counts.keys.sorted(),
            storeCounts = counts
        )
    }
}
