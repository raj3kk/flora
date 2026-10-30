package com.flora.app.automation

/**
 * Planner-first routing, Batch 1 (2026-10-30) — deterministic parser.
 *
 * Pure Kotlin, koi Android import nahi (isliye JVM unit tests me
 * seedha chalta hai). Resolver lambda ke zariye app dhoondhta hai
 * (FloraApp se VerifiedAppLauncher.findByName milta hai; tests me
 * fake resolver). Jo pattern poori tarah samajh na aaye, us par
 * null lautaata hai — tab maujooda branches/Brain raasta chalta hai
 * (kisi aur command ka behaviour nahi badlega).
 *
 * Samajh-pehle: app resolve (0 → null, >1 → null — maujooda
 * branch ek baar poochhta hai), foreground context, gaane ka naam
 * na mile to null (maujooda branch ek baar poochhti hai).
 */
object VoicePlanParser {

    private val MEDIA_VERBS = listOf(
        "gaana bajao",
        "gaana chalao",
        "gana bajao",
        "gana chalao",
        "song bajao",
        "song play",
        "gaana sunao",
        "gana sunao",
    )

    private val OWN_SCREEN_NAMES = setOf("camera", "yaadein", "memories")

    private fun youtubeSearchUrl(song: String): String =
        "https://www.youtube.com/results?search_query=" + song.trim().replace(" ", "+")

    /**
     * @param question asli vakya (original case — gaane ka naam isi se niklega)
     * @param resolveApp app display naam -> list of (label, packageName)
     * @param foregroundApp Flora se kholi gayi saamne wali app (pkg, label) ya null
     */
    fun parse(
        question: String,
        resolveApp: (String) -> List<Pair<String, String>>,
        foregroundApp: Pair<String, String>? = null,
    ): VoicePlan? {
        val lower = question.lowercase().trim()
        if (lower.isEmpty()) return null

        // 1) Compound PEHLE — warna standalone media check poore vakya
        // ko gaane ka naam samajh lega (jaise "youtube kholo aur
        // arijit ka gaana bajao" me song = "youtube kholo aur arijit ka"
        // galat hota). App naam mila (chahe resolve ho ya na ho) to
        // yahi raasta faisla karega — null matlab maujooda compound
        // branch sambhalegi (uski recurse + not-found/ambiguity lines).
        val sep = when {
            lower.contains(" aur ") -> " aur "
            lower.contains(" phir ") -> " phir "
            lower.contains("khol k") -> "khol k"
            lower.contains("kholke") -> "kholke"
            lower.contains(" kholo to ") -> " kholo to "
            else -> null
        }
        if (sep != null) {
            val first = lower.substringBefore(sep).trim()
            val rest = lower.substringAfter(sep).trim()
            if (first.isNotEmpty() && rest.isNotEmpty()) {
                val appName = when (sep) {
                    "khol k", "kholke" -> if (first in OWN_SCREEN_NAMES) null else first
                    " kholo to " -> {
                        if (!first.endsWith(" kholo")) null
                        else {
                            val n = first.removeSuffix(" kholo").trim()
                            if (n in OWN_SCREEN_NAMES || n.isEmpty()) null else n
                        }
                    }
                    else -> {
                        when {
                            first.startsWith("open ") || first.startsWith("kholo ") ->
                                first.substringAfter(" ").trim()
                            first.endsWith(" kholo") &&
                                first !in setOf("camera kholo", "yaadein kholo", "memories kholo") &&
                                !first.startsWith("chrome me ") && !first.startsWith("website ") ->
                                first.removeSuffix(" kholo").trim()
                            else -> ""
                        }.ifEmpty { null }
                    }
                }
                if (appName != null) {
                    val matches = resolveApp(appName)
                    if (matches.size != 1) return null
                    val (label, pkg) = matches[0]
                    val openStep = VoicePlanStep(
                        title = "$label kholo",
                        executor = VoicePlanExecutor.APP_OPEN,
                        input = mapOf("label" to label, "package" to pkg),
                        successCheck = "App verified khuli.",
                    )
                    // Rest: media phrase → media chain (sirf YouTube par, Batch 1).
                    val restMedia = parseMedia(rest, rest, resolveApp, forceApp = (label to pkg))
                    if (restMedia != null && label.equals("YouTube", ignoreCase = true)) {
                        return VoicePlan(
                            goal = question,
                            steps = listOf(openStep) + restMedia.steps.dropWhile {
                                it.executor == VoicePlanExecutor.APP_OPEN
                            },
                        )
                    }
                    // Rest: "<q> search karo" → open + in-app search.
                    val restQuery = parseSearchQuery(rest)
                    if (restQuery != null) {
                        return VoicePlan(
                            goal = question,
                            steps = listOf(
                                openStep,
                                VoicePlanStep(
                                    title = "$label me '$restQuery' search karo",
                                    executor = VoicePlanExecutor.IN_APP_SEARCH,
                                    input = mapOf("query" to restQuery),
                                    successCheck = "Search box me query type + submit hua.",
                                    fallback = "",
                                ),
                            ),
                        )
                    }
                    return null
                }
                // App naam nahi mila (jaise "tum aur hum gaana bajao")
                // → neeche media check par jaane do, wahi sahi raasta hai.
            }
        }

        // 2) Follow-up in-app search — sirf tab jab Flora se kholi gayi
        // app saamne ho (ForegroundAppContext ka maujooda niyam).
        if (foregroundApp != null) {
            val q = parseSearchQuery(lower)
            if (q != null) {
                return VoicePlan(
                    goal = question,
                    steps = listOf(
                        VoicePlanStep(
                            title = "${foregroundApp.second} me '$q' search karo",
                            executor = VoicePlanExecutor.IN_APP_SEARCH,
                            input = mapOf("query" to q),
                            successCheck = "Search box me query type + submit hua.",
                            fallback = "",
                        ),
                    ),
                )
            }
        }

        // 3) Standalone media phrase — position-independent
        // (vakya-start ki shart nahi).
        return parseMedia(lower, question, resolveApp)
    }

    private fun parseSearchQuery(lower: String): String? {
        val q = when {
            lower.endsWith(" search karo") -> lower.removeSuffix(" search karo").trim()
            lower.endsWith(" khojo") -> lower.removeSuffix(" khojo").trim()
            lower.startsWith("search karo ") -> lower.removePrefix("search karo ").trim()
            else -> null
        }
        return q?.ifEmpty { null }
    }

    /**
     * Media phrase dhoondhta hai. forceApp diya ho to APP_OPEN step
     * usi app ka banega (compound rest ke liye); warna YouTube
     * resolve karke, na mile to SEARCH_URL se shuru hoga.
     * Gaane ka naam na mile to null (maujooda branch ek baar poochhti hai).
     */
    private fun parseMedia(
        lower: String,
        original: String,
        resolveApp: (String) -> List<Pair<String, String>>,
        forceApp: Pair<String, String>? = null,
    ): VoicePlan? {
        var verb: String? = null
        var verbIndex = -1
        for (v in MEDIA_VERBS) {
            val i = lower.indexOf(v)
            if (i >= 0 && (verbIndex < 0 || i < verbIndex)) {
                verb = v
                verbIndex = i
            }
        }
        if (verb == null) return null
        // Safety: lowercase se length badli ho to index original par
        // lagu na ho — aise input par null (maujooda raasta sambhalega).
        if (verbIndex + verb.length > original.length) return null
        // Song: verb ke baad wala hissa, warna verb se pehle wala.
        val after = original.substring(verbIndex + verb.length).trim()
        val before = original.substring(0, verbIndex).trim()
        val song = (after.ifEmpty { before }).trim()
        if (song.isEmpty()) return null

        val steps = mutableListOf<VoicePlanStep>()
        if (forceApp != null) {
            steps += VoicePlanStep(
                title = "${forceApp.first} kholo",
                executor = VoicePlanExecutor.APP_OPEN,
                input = mapOf("label" to forceApp.first, "package" to forceApp.second),
                successCheck = "App verified khuli.",
            )
        } else {
            val yt = resolveApp("youtube").firstOrNull {
                it.first.equals("YouTube", ignoreCase = true)
            } ?: resolveApp("youtube").firstOrNull()
            if (yt != null) {
                steps += VoicePlanStep(
                    title = "YouTube kholo",
                    executor = VoicePlanExecutor.APP_OPEN,
                    input = mapOf("label" to yt.first, "package" to yt.second),
                    successCheck = "App verified khuli.",
                )
            }
        }
        steps += VoicePlanStep(
            title = "'$song' search karo",
            executor = VoicePlanExecutor.IN_APP_SEARCH,
            input = mapOf("query" to song),
            successCheck = "Search box me gaana type + submit hua.",
            fallback = "SEARCH_URL:" + youtubeSearchUrl(song),
        )
        return VoicePlan(goal = original, steps = steps)
    }
}
