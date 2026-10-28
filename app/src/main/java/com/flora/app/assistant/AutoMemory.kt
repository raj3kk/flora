package com.flora.app.assistant

import com.flora.app.data.MemoriesStore
import kotlinx.coroutines.flow.first

/**
 * AutoMemory (v2.22, chunk 4) — user jo apne baare me batata
 * hai, Flora use yaad rakhti hai (phone par hi, Memories
 * screen me dikhta hai, user mita sakta hai).
 * Sirf saaf statements seekhe jate hain — commands, sawaal
 * aur chhoti baatein nahi. Duplicate dobara save nahi hota.
 */
object AutoMemory {

    private val patterns = listOf(
        Regex("^mera naam (.+) hai$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Naam: ${m.groupValues[1]}" },
        Regex("^my name is (.+)$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Naam: ${m.groupValues[1]}" },
        Regex("^mujhe (.+) pasand hai$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Pasand: ${m.groupValues[1]}" },
        Regex("^i like (.+)$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Pasand: ${m.groupValues[1]}" },
        Regex("^(meri|mere) (\\w+) ka naam (.+) hai$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "${m.groupValues[2]} ka naam: ${m.groupValues[3]}" },
        Regex("^main (.+) me rehta hun$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Rehta hai: ${m.groupValues[1]}" },
        Regex("^i live in (.+)$", RegexOption.IGNORE_CASE) to { m: MatchResult -> "Rehta hai: ${m.groupValues[1]}" },
    )

    fun extractFacts(text: String): List<String> {
        val raw = text.trim()
        // Sawaal kabhi yaad nahi hota ("mera naam kya hai?" fact nahi hai).
        if (raw.endsWith("?")) return emptyList()
        val clean = raw.trimEnd('.', '!', ' ')
        if (clean.length < 10) return emptyList()
        val out = mutableListOf<String>()
        for ((regex, format) in patterns) {
            val m = regex.find(clean)
            if (m != null) {
                val fact = format(m).trim()
                if (fact.length in 5..120) out.add(fact)
            }
        }
        return out
    }

    /** Naye facts save karo; kitne save hue, wapas do. */
    suspend fun learn(store: MemoriesStore, text: String): Int {
        val facts = extractFacts(text)
        if (facts.isEmpty()) return 0
        val existing = try {
            store.items.first().map { it.lowercase() }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
        var added = 0
        for (fact in facts) {
            if (fact.lowercase() !in existing) {
                try {
                    store.add(fact)
                    added++
                } catch (e: Exception) {
                }
            }
        }
        return added
    }
}
