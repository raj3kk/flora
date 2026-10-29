package com.flora.app.tools

/**
 * B1 G5/G7 — intent routing layer (exact-phrase branches se aage).
 *
 * Teen raaste:
 * - OpenApp: "<naam> kholo" / "open <naam>" (app ka naam) — VerifiedAppLauncher.
 * - OpenBrowser: "browser me ...", "chrome me ...", "website ..." — Flora browser.
 * - Research: live sawal — "search karo" / "dhoondho" bole bina bhi,
 *   jab sawal me live nishan ho (aaj / abhi / taaza / latest /
 *   mausam / khabar / price / rate / score / kab / kya hai + "?").
 * - BrainChat: baaki sab — saadharan baat-cheet, brain se.
 *
 * Aakhri fallback ka niyam (G5): live sawal par brain.ask seedha
 * jawab na de — pehle research lane (Tavily + DuckDuckGo +
 * brain-proxy fallback) chale; lane fail ho to hi brain-proxy
 * (G7: lane-fail -> brain-proxy) imaandaar prefix ke saath.
 * Browser-fail -> research lane (G7) BrowserScreen me hai.
 */
object IntentRouter {

    sealed class Route {
        data class OpenApp(val name: String) : Route()
        data class OpenBrowser(val site: String) : Route()
        data class Research(val query: String) : Route()
        data class BrainChat(val query: String) : Route()
    }

    private val LIVE_WORDS = listOf(
        "aaj", "abhi", "taaza", "taza", "latest", "current", "live",
        "mausam", "weather", "khabar", "news", "price", "rate",
        "score", "result", "bhav", "daam", "breaking",
    )

    private val RESEARCH_VERBS = listOf(
        "search karo", "dhoondho", "dhoondh", "khojo", "khoj karo",
        "search kar", "pata karo", "pata lagao", "batao aaj",
    )

    private val QUESTION_WORDS = listOf(
        "kya hai", "kya hoga", "kab hai", "kab hoga", "kitna hai",
        "kitne hai", "kaun hai", "kahan hai", "kaisa hai",
        "what is", "when is", "how much",
    )

    /** Research lane ke imaandaar fail strings (SearchTools.search se aate hain). */
    fun isLaneFailure(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("Search nahi ho paya") ||
            t.startsWith("Is sawal ka seedha jawab nahi mila") ||
            t.startsWith("Search ka jawab samajh nahi aaya") ||
            t.startsWith("Study search nahi ho paya")
    }

    fun route(raw: String): Route {
        val lower = raw.trim().lowercase()
        if (lower.isEmpty()) return Route.BrainChat(raw)
        // App kholna: exact-phrase se aage — "<naam> kholo" / "open <naam>".
        if (lower.endsWith(" kholo")) {
            val name = lower.removeSuffix(" kholo").trim()
            if (name.isNotEmpty() && name !in setOf("browser", "chrome", "github", "vercel")) {
                return Route.OpenApp(name)
            }
        }
        if (lower.startsWith("open ") || lower.startsWith("kholo ")) {
            val name = lower.removePrefix("open ").removePrefix("kholo ").trim()
            if (name.isNotEmpty()) return Route.OpenApp(name)
        }
        // Browser: page dikhana / form — Flora browser.
        if (lower.startsWith("browser me ") || lower.startsWith("chrome me ") ||
            lower.startsWith("website ") || lower.startsWith("website kholo ")
        ) {
            return Route.OpenBrowser(raw.trim())
        }
        // Research: verb bole to seedha; live nishan + sawal ho to bhi,
        // bina "search karo" bole (G5 ka asal fix).
        if (RESEARCH_VERBS.any { lower.contains(it) }) {
            return Route.Research(raw.trim())
        }
        val hasLive = LIVE_WORDS.any { lower.contains(it) }
        val hasQuestion = QUESTION_WORDS.any { lower.contains(it) } || lower.endsWith("?")
        if (hasLive && hasQuestion) return Route.Research(raw.trim())
        if (hasLive && (lower.startsWith("aaj ") || lower.startsWith("abhi "))) {
            return Route.Research(raw.trim())
        }
        return Route.BrainChat(raw.trim())
    }
}
