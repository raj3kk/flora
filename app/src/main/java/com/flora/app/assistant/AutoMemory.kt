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

    /**
     * Naye facts save karo; kitne badle (save/update), wapas do.
     * U1 dedup: bilkul wahi fact dobara save nahi hota.
     * U3 sudhaar: nayi baat purani baat ki jagah leti hai —
     * jaise "mera naam X hai" ke baad "mera naam Y hai" aaye to
     * "Naam: X" ki jagah "Naam: Y" ho jaata hai (dono saath
     * nahi rehte, warna brain ko ulta-seedha context milta).
     * Sirf "Key: value" wale facts ko key se milaya jaata hai —
     * bina colon wale free notes ko kabhi overwrite nahi kiya jaata.
     */
    suspend fun learn(store: MemoriesStore, text: String): Int {
        val facts = extractFacts(text)
        if (facts.isEmpty()) return 0
        val existing = try {
            store.items.first()
        } catch (e: Exception) {
            emptyList()
        }
        var changed = 0
        for (fact in facts) {
            if (existing.any { it.lowercase() == fact.lowercase() }) continue
            val key = fact.substringBefore(":").trim().lowercase()
            val keyIdx = existing.indexOfFirst {
                it.contains(":") &&
                    it.substringBefore(":").trim().lowercase() == key &&
                    key.isNotEmpty()
            }
            try {
                if (keyIdx >= 0) {
                    store.update(keyIdx, fact)
                } else {
                    store.add(fact)
                }
                changed++
            } catch (e: Exception) {
            }
        }
        return changed
    }
}
