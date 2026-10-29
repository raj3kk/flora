package com.flora.app.planner

import com.flora.app.data.AutoLogStore
import com.flora.app.data.ChatMemoryStore
import com.flora.app.data.ProjectMemoryStore
import com.flora.app.tools.KeyInputGuard
import kotlinx.coroutines.flow.first

/**
 * B5-04 E4 unified search — chat + project + task log ki EK search.
 *
 * B4 ka memoryContext() sirf chat ke last-12 turns tha — ye poori
 * search hai (teeno sources, ranking + dedup ke saath).
 *
 * NAAM-CONFUSION SE BACHO (teeno alag cheezein hain):
 * - SearchTools = EXTERNAL web-search lanes (Tavily/DuckDuckGo).
 * - InAppSearch (automation/) = in-app automation search (kisi
 *   app ke andar search box me type karna).
 * - UnifiedSearch (ye file) = Flora ki APNI memory ki search
 *   (chat + project + task log) — na web hai, na automation.
 *
 * DEDUP / RANKING RULE (F6 + B5-04):
 * - Ek hi event 3 sources se mile to ek baar dikhega: dedup key
 *   = normalized text (lowercase, whitespace-collapse). Best
 *   rank wali entry rehti hai, baaki sources uske `sources`
 *   list me jud jate hain.
 * - Ranking: exact-phrase match (100) > saare query tokens mile
 *   (50 + token count) > koi ek token mile (10). Barabar rank
 *   par naya (ts bada) pehle.
 *
 * B5-12 SAFETY: sensitive text (key-like / forbidden assignment)
 * search result me kabhi nahi aata — scan waqt hi drop hota
 * hai. Result me sirf SNIPPET dikhta hai (max 120 chars),
 * poora sensitive text kabhi nahi.
 *
 * SOURCES (task log = execution proof):
 * - chat: ChatMemoryStore turns (user + Flora).
 * - project: ProjectMemoryStore entries (project-scoped).
 * - tasklog: AutoLogStore items (Auto Log = execution proof;
 *   plan history Room me hai, AutoLog 30-din prune hota hai —
 *   isliye task-log source AutoLog hi hai, Room history nahi).
 *
 * B5 FIX 3 note: AppSearch index is version me NAHI hai
 * (stack-blocked, upar gradle comment + audit log dekho) —
 * search poori tarah store-scan par chalti hai, jo source of
 * truth bhi hai. Index wapas aane par merge() wahi dedup
 * rule se hits ko milayega (koi API change nahi chahiye).
 */
data class UnifiedSearchResult(
    val source: String,
    val sources: List<String>,
    val title: String,
    val snippet: String,
    val ts: Long,
    val rank: Int,
)

object UnifiedSearchLogic {

    const val MAX_SNIPPET_CHARS = 120

    fun isSensitive(text: String): Boolean {
        if (text.isBlank()) return false
        if (KeyInputGuard.looksKeyLike(text)) return true
        if (PlanValidator.containsForbiddenAssignment(text)) return true
        return false
    }

    fun snippet(text: String): String {
        val clean = text.trim().split(Regex("\\s+")).joinToString(" ")
        if (clean.length <= MAX_SNIPPET_CHARS) return clean
        return clean.take(MAX_SNIPPET_CHARS - 3) + "..."
    }

    fun dedupKey(text: String): String =
        text.trim().lowercase().split(Regex("\\s+")).joinToString(" ")

    fun rank(query: String, text: String): Int {
        val q = query.trim().lowercase()
        val t = text.lowercase()
        if (q.isEmpty() || t.isEmpty()) return 0
        if (t.contains(q)) return 100
        val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return 0
        val matched = tokens.count { t.contains(it) }
        if (matched == tokens.size) return 50 + matched
        if (matched > 0) return 10 * matched
        return 0
    }

    fun merge(candidates: List<UnifiedSearchResult>): List<UnifiedSearchResult> {
        val byKey = linkedMapOf<String, UnifiedSearchResult>()
        for (candidate in candidates) {
            val key = dedupKey(candidate.snippet)
            val existing = byKey[key]
            if (existing == null) {
                byKey[key] = candidate
            } else {
                val mergedSources = (existing.sources + candidate.sources).distinct()
                val better = if (candidate.rank > existing.rank ||
                    (candidate.rank == existing.rank && candidate.ts > existing.ts)
                ) {
                    candidate.copy(sources = mergedSources)
                } else {
                    existing.copy(sources = mergedSources)
                }
                byKey[key] = better
            }
        }
        return byKey.values.sortedWith(
            compareByDescending<UnifiedSearchResult> { it.rank }
                .thenByDescending { it.ts }
        ).take(50)
    }
}

class UnifiedSearch(
    private val chatMemoryStore: ChatMemoryStore?,
    private val projectMemoryStore: ProjectMemoryStore?,
    private val autoLogStore: AutoLogStore?,
) {

    suspend fun search(query: String): List<UnifiedSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val candidates = mutableListOf<UnifiedSearchResult>()

        try {
            val turns = chatMemoryStore?.loadAll() ?: emptyList()
            for (turn in turns) {
                if (UnifiedSearchLogic.isSensitive(turn.text)) continue
                val rank = UnifiedSearchLogic.rank(cleanQuery, turn.text)
                if (rank <= 0) continue
                candidates.add(
                    UnifiedSearchResult(
                        source = "chat",
                        sources = listOf("chat"),
                        title = if (turn.fromUser) "Chat (user)" else "Chat (Flora)",
                        snippet = UnifiedSearchLogic.snippet(turn.text),
                        ts = turn.ts,
                        rank = rank,
                    )
                )
            }
        } catch (e: Exception) {
        }

        val projectEntries = try {
            projectMemoryStore?.loadAll() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        for (entry in projectEntries) {
            if (UnifiedSearchLogic.isSensitive(entry.text)) continue
            val rank = UnifiedSearchLogic.rank(cleanQuery, entry.text)
            if (rank <= 0) continue
            candidates.add(
                UnifiedSearchResult(
                    source = "project",
                    sources = listOf("project"),
                    title = "Project memory (${entry.kind})",
                    snippet = UnifiedSearchLogic.snippet(entry.text),
                    ts = entry.ts,
                    rank = rank,
                )
            )
        }

        try {
            val items = autoLogStore?.items?.first() ?: emptyList()
            for ((text, ts) in items) {
                if (UnifiedSearchLogic.isSensitive(text)) continue
                val rank = UnifiedSearchLogic.rank(cleanQuery, text)
                if (rank <= 0) continue
                candidates.add(
                    UnifiedSearchResult(
                        source = "tasklog",
                        sources = listOf("tasklog"),
                        title = "Task log",
                        snippet = UnifiedSearchLogic.snippet(text),
                        ts = ts,
                        rank = rank,
                    )
                )
            }
        } catch (e: Exception) {
        }

        // B5 FIX 3: AppSearch index HATA diya gaya hai — poori
        // 1.1.0 line ko compileSdk 35+ chahiye, pinned stack 34
        // hai (CI runs 40896889152 / 40899312759 / 40900218424).
        // Unified search store-scan-only hai (source of truth
        // wahi tha); AppSearch index tab wapas aayega jab stack
        // upgrade (compileSdk 35+) coordinator se approve ho.
        // Koi jhootha "AppSearch-backed" daava nahi hai.

        return UnifiedSearchLogic.merge(candidates)
    }
}
