package com.flora.app.automation

/**
 * Fixer (tester D2) — first-result pick ka pure-Kotlin hissa.
 *
 * FloraAutomationService ka DFS AccessibilityNodeInfo par chalta
 * hai (isliye JVM unit test me seedha nahi chalta); pick ka
 * faisla yahan hai taaki wo honestly test ho sake. Service sirf
 * candidates (depth + subtree text, DFS order me) jama karke
 * [pick] ko deti hai.
 *
 * Niyam:
 * 1. Sabse gehra tier hi chuna jata hai (container ki gehrai =
 *    result row ki pehchan).
 * 2. Us tier me DFS order me PEHLA candidate jeet-ta hai —
 *    barabar depth par aakhri visited ka jeetna bug tha (>=
 *    comparison ki wajah se), ab > jaisa first-wins hai.
 * 3. Query-token preference: us tier me pehla candidate jiske
 *    text me query ka token clearly mile, use preference milti
 *    hai. Token chhota ho (3 akshar se kam) to ignore; match
 *    poora shabd ho, ya lamba token (>= 4) kisi shabd ka hissa
 *    ho — taaki chhota token galat shabd se na takraye (jaise
 *    "raj" -> "suraj"). Koi clear match na mile to pehle wale
 *    first-pick par fall back (abort nahi) — YouTube path pehle
 *    jaisa hi rahe, title aksar query se alag shabdon me hota hai.
 * 4. 0 candidate par null — caller abort + failMessage dega.
 *
 * Ye heuristic hai, guarantee nahi: token match ka matlab sirf
 * "title query se milta-julta hai", sahi result ki pakki
 * pehchan nahi.
 */
data class ResultCandidate(val depth: Int, val text: String)

object FirstResultPicker {

    fun queryTokens(query: String?): List<String> {
        if (query.isNullOrBlank()) return emptyList()
        return query.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 }
            .distinct()
    }

    private fun tokenMatches(hayText: String, token: String): Boolean {
        val words = hayText.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
        return words.any { word ->
            word == token || (token.length >= 4 && word.contains(token))
        }
    }

    /**
     * @return chune gaye candidate ka [candidates] me index, ya
     * null jab koi candidate na ho.
     */
    fun pick(candidates: List<ResultCandidate>, query: String?): Int? {
        if (candidates.isEmpty()) return null
        val maxDepth = candidates.maxOf { it.depth }
        val deepest = candidates.withIndex().filter { it.value.depth == maxDepth }
        val tokens = queryTokens(query)
        if (tokens.isNotEmpty()) {
            val hit = deepest.firstOrNull { (_, candidate) ->
                tokens.any { token -> tokenMatches(candidate.text, token) }
            }
            if (hit != null) return hit.index
        }
        return deepest.first().index
    }
}
