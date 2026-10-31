package com.flora.app.data

import android.content.Context
import kotlinx.coroutines.flow.first

/**
 * P3-04 — Data delete + export backend (fixer wiring).
 *
 * Ye asli calls hain — koi nayi table invent nahi ki gayi, har
 * store ka naam + call maujooda code se verify karke likha gaya
 * hai (DataStore stores; app me Room store nahi hai, isliye
 * "Room calls" yahan DataStore calls hain):
 *
 * - journal → JournalStore.replaceAll(emptyList)
 * - docs → DocsStore.replaceAll(emptyList)
 * - routines → RoutineStore.save(emptyList)
 * - jobs → JobStore.save(emptyList) + JobChainStore.save(emptyList)
 *   (chains jobs family hain — jobs delete par orphan chain na rahe)
 * - memories → MemoriesStore.clearAll
 * - reminders → ReminderStore.save(emptyList)
 * - triggers → TriggerStore.save(emptyList)
 * - watchlist → WatchlistStore.save(emptyList)
 * - post-history → PostHistoryStore.clear
 * - macros → MacroStore.save(emptyList)
 * - fav-songs → FavSongStore.clear
 * - sos → SosStore.clearAll
 * - notifications → NotificationStore.clear
 * - pending-tasks → PendingTaskStore.clearAll
 * - projects → ProjectStore.replaceAllProjects(empty, empty)
 *   (projects dir + index dono saaf hote hain)
 * - project-memory → ProjectMemoryStore.replaceAll(empty)
 *
 * Rules barkaraar:
 * - Delete sirf confirm ke baad ([DataDeleteExport.guardDelete]).
 * - Retention 30 din ([DataDeleteExport.RETENTION_DAYS]) — ye
 *   full-delete flow hai, retention prune alag stores me pehle
 *   se hai; delete-all retention se upar user ka faisla hai.
 * - Chat memory EXEMPT hai — is backend me ChatMemoryStore ko
 *   kabhi haath nahi lagaya jata.
 * - Supabase/server-side rows (licence/UTR) app se delete nahi
 *   hote — unke liye server-side function chahiye (Part 2 scope,
 *   yahan invent nahi kiya gaya). Screen par ye saaf likha hai.
 * - Export = manifest + counts ([DataDeleteExport.ExportBundle])
 *   — poora content dump nahi (wo backup ka kaam hai); manifest
 *   user-tap par share ki ja sakti hai, auto-send kabhi nahi.
 */
class AppDeleteExportBackend(private val context: Context) :
    DataDeleteExport.DeleteExportBackend {

    override suspend fun exportAll(): DataDeleteExport.ExportBundle {
        val counts = linkedMapOf<String, Int>()
        counts["journal"] = runCatching { JournalStore(context).entries.first().size }.getOrDefault(0)
        counts["docs"] = runCatching { DocsStore(context).docs.first().size }.getOrDefault(0)
        counts["routines"] = runCatching { RoutineStore.load(context).size }.getOrDefault(0)
        counts["jobs"] = runCatching { JobStore.load(context).size }.getOrDefault(0)
        counts["memories"] = runCatching { MemoriesStore(context).items.first().size }.getOrDefault(0)
        counts["reminders"] = runCatching { ReminderStore.load(context).size }.getOrDefault(0)
        counts["triggers"] = runCatching { TriggerStore.load(context).size }.getOrDefault(0)
        counts["watchlist"] = runCatching { WatchlistStore.load(context).size }.getOrDefault(0)
        counts["post-history"] = runCatching { PostHistoryStore.list(context).size }.getOrDefault(0)
        counts["macros"] = runCatching { MacroStore.load(context).size }.getOrDefault(0)
        counts["fav-songs"] = runCatching { FavSongStore.list(context).size }.getOrDefault(0)
        counts["sos"] = runCatching { SosStore(context).contacts.first().size }.getOrDefault(0)
        counts["notifications"] = runCatching { NotificationStore.load(context).size }.getOrDefault(0)
        counts["pending-tasks"] = runCatching { PendingTaskStore(context).tasks.first().size }.getOrDefault(0)
        counts["projects"] = runCatching { ProjectStore(context).listProjects().size }.getOrDefault(0)
        counts["project-memory"] = runCatching { ProjectMemoryStore(context).loadAll().size }.getOrDefault(0)
        return DataDeleteExport.manifestFor(counts)
    }

    override suspend fun deleteAll(confirmed: Boolean): DataDeleteExport.DeleteResult {
        val guard = DataDeleteExport.guardDelete(confirmed)
        if (!guard.ok) return guard
        val failed = mutableListOf<String>()
        suspend fun step(name: String, block: suspend () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                failed += name
            }
        }
        // Har step alag try me — ek store fail ho to baaki phir
        // bhi saaf hon, aur fail hue naam result me dikhen.
        step("journal") { JournalStore(context).replaceAll(emptyList()) }
        step("docs") { DocsStore(context).replaceAll(emptyList()) }
        step("routines") { RoutineStore.save(context, emptyList()) }
        step("jobs") {
            JobStore.save(context, emptyList())
            JobChainStore.save(context, emptyList())
        }
        step("memories") { MemoriesStore(context).clearAll() }
        step("reminders") { ReminderStore.save(context, emptyList()) }
        step("triggers") { TriggerStore.save(context, emptyList()) }
        step("watchlist") { WatchlistStore.save(context, emptyList()) }
        step("post-history") { PostHistoryStore.clear(context) }
        step("macros") { MacroStore.save(context, emptyList()) }
        step("fav-songs") { FavSongStore.clear(context) }
        step("sos") { SosStore(context).clearAll() }
        step("notifications") { NotificationStore.clear(context) }
        step("pending-tasks") { PendingTaskStore(context).clearAll() }
        step("projects") {
            val ok = ProjectStore(context).replaceAllProjects(emptyList(), emptyMap())
            if (!ok) throw IllegalStateException("projects replace failed")
        }
        step("project-memory") {
            val ok = ProjectMemoryStore(context).replaceAll(emptyList())
            if (!ok) throw IllegalStateException("project-memory replace failed")
        }
        // Chat memory exempt — yahan kabhi clear nahi hoti.
        return if (failed.isEmpty()) {
            DataDeleteExport.DeleteResult(true, "saare scope stores saaf ho gaye (chat memory exempt)")
        } else {
            DataDeleteExport.DeleteResult(
                false,
                "ye stores saaf nahi ho paye: " + failed.joinToString(", ")
            )
        }
    }
}
