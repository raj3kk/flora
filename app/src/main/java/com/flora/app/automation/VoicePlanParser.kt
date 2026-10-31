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
        // Voice-fix: video verbs — gaana chain hi reuse hota hai
        // (parseMedia me APP_OPEN + IN_APP_SEARCH + TAP_FIRST_RESULT,
        // wahi failMessage); koi alag video chain nahi bani.
        "video chalao",
        "video play karo",
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
                    // " kholo to " separator ke andar hi " kholo" hota hai,
                    // isliye substringBefore se `first` seedha app naam hai
                    // (jaise "youtube") — endsWith(" kholo") yahan kabhi
                    // sach nahi hota (CI run 41024742806: compoundKholoToMedia
                    // isi wajah se galat plan banata tha). "khol k" jaisa hi.
                    " kholo to " -> if (first in OWN_SCREEN_NAMES || first.isEmpty()) null else first
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

        // 1b) Voice-fix: "<app> kholo <media verb>" — plain " kholo "
        // upar wale seps me nahi hai, isliye "youtube kholo video
        // chalao" yahan tak aata hai. Sirf tab jab rest me koi media
        // verb ho; warna neeche ke raaste waisi hi chalenge.
        // "aur/phir/khol k/kholo to" wale vakya upar hi faisla ho
        // chuke hote hain, isliye yahan unka hijack nahi hota.
        if (lower.contains(" kholo ")) {
            val first = lower.substringBefore(" kholo ").trim()
            val rest = lower.substringAfter(" kholo ").trim()
            if (first.isNotEmpty() && rest.isNotEmpty() &&
                MEDIA_VERBS.any { rest.contains(it) }
            ) {
                // Fixer: §1b me aate hi faisla isi branch ka hai —
                // own-screen naam (camera/yaadein/memories) par bhi null,
                // warna §3 us naam ko chhod kar YouTube plan bana deta.
                if (first in OWN_SCREEN_NAMES) return null
                val matches = resolveApp(first)
                if (matches.size == 1) {
                    val (label, pkg) = matches[0]
                    val openStep = VoicePlanStep(
                        title = "$label kholo",
                        executor = VoicePlanExecutor.APP_OPEN,
                        input = mapOf("label" to label, "package" to pkg),
                        successCheck = "App verified khuli.",
                    )
                    val restMedia = parseMedia(rest, rest, resolveApp, forceApp = (label to pkg))
                    if (restMedia != null) {
                        // YouTube → poori chain (open + search + tap);
                        // non-YouTube → sirf open + search (parseMedia
                        // non-YouTube forceApp par tap step banata hi nahi).
                        return VoicePlan(
                            goal = question,
                            steps = listOf(openStep) + restMedia.steps.dropWhile {
                                it.executor == VoicePlanExecutor.APP_OPEN
                            },
                        )
                    }
                    // Bare media verb, koi title nahi (jaise
                    // "youtube kholo video chalao"): search ke liye
                    // query nahi hai, isliye sirf YouTube kholo;
                    // non-YouTube par null (maujooda raasta sambhalega).
                    if (rest in MEDIA_VERBS && label.equals("YouTube", ignoreCase = true)) {
                        return VoicePlan(goal = question, steps = listOf(openStep))
                    }
                    return null
                }
                // Fixer: §1 ke aur-compound niyam jaisa — app resolve
                // na ho (0) ya ambiguous ho (>1) to null; §3 par
                // fall-through se naam wala app chhup kar YouTube
                // plan nahi banega (size == 1 ka block upar hamesha
                // return karta hai, isliye yahan sirf != 1 bachta hai).
                return null
            }
        }

        // Cycle 1 — generic "<app> me <cheez> ..." (§1c se
        // PEHLE, warna §1c bare-open iska app naam "spotify me
        // arijit" samajh kar null de dega). Deictic ("is app")
        // par fall-through — wo maujooda foreground branch ka
        // kaam hai, iska nahi. Sirf search/kholo/scroll rest hi
        // claim hota hai; baaki rest (jaise "volume badhao") par
        // fall-through, taaki maujooda branch ka behaviour na toote.
        // App resolve ka null-contract §1b jaisa hai: 0 ya >1
        // match par null (galat app kabhi nahi khulni chahiye).
        if (lower.contains(" me ")) {
            val appPart = lower.substringBefore(" me ").trim()
            val restMe = lower.substringAfter(" me ").trim()
            val deicticMe = appPart in setOf("is app", "is", "iss app", "yahan", "ye", "yeh")
            if (appPart.isNotEmpty() && restMe.isNotEmpty() && !deicticMe) {
                val scrollDir = parseScrollDirection(restMe)
                val meQuery = parseSearchQuery(restMe)
                val kholoQuery = if (meQuery == null && restMe.endsWith(" kholo")) {
                    restMe.removeSuffix(" kholo").trim().ifEmpty { null }
                } else {
                    null
                }
                if (scrollDir != null || meQuery != null || kholoQuery != null) {
                    if (appPart in OWN_SCREEN_NAMES) return null
                    val matches = resolveApp(appPart)
                    if (matches.size != 1) return null
                    val (label, pkg) = matches[0]
                    val openStep = openStep(label, pkg)
                    if (scrollDir != null) {
                        return VoicePlan(
                            goal = question,
                            steps = listOf(
                                openStep,
                                VoicePlanStep(
                                    title = "$label me scroll karo",
                                    executor = VoicePlanExecutor.SCROLL,
                                    input = mapOf("forward" to scrollDir.toString()),
                                    successCheck = "Screen scroll hui.",
                                ),
                            ),
                        )
                    }
                    val query = meQuery ?: kholoQuery
                    if (query != null) {
                        val searchStep = searchStep(label, query)
                        val steps = if (kholoQuery != null) {
                            listOf(
                                openStep,
                                searchStep,
                                VoicePlanStep(
                                    title = "Pehla result kholo",
                                    executor = VoicePlanExecutor.TAP_FIRST_RESULT,
                                    input = mapOf("package" to pkg),
                                    successCheck = "$label saamne ho aur pehla result khula ho.",
                                    failMessage = "Search khol diya hai, pehla result khud tap karo (auto tap yahan ruka).",
                                ),
                            )
                        } else {
                            listOf(openStep, searchStep)
                        }
                        return VoicePlan(goal = question, steps = steps)
                    }
                }
            }
        }

        // Cycle 1 — bare open: "<app> kholo" (poora vakya sirf
        // app + kholo). " me " wala vakya upar §1e me hi faisla
        // ho chuka hai; yahan app naam me " me " ho to skip
        // (fall-through), null nahi — warna "is app me ..." wali
        // maujooda branch hijack ho jayegi. Deictic ("ye kholo")
        // par bhi skip; own-screen par null (§1b jaisa).
        if (lower.endsWith(" kholo")) {
            val appName = lower.removeSuffix(" kholo").trim()
            if (appName.isNotEmpty() && " me " !in appName &&
                appName !in setOf("ye", "yeh", "is", "yahan", "is app", "iss app")
            ) {
                if (appName in OWN_SCREEN_NAMES) return null
                val matches = resolveApp(appName)
                if (matches.size != 1) return null
                val (label, pkg) = matches[0]
                return VoicePlan(goal = question, steps = listOf(openStep(label, pkg)))
            }
        }

        // Cycle 1 — "<app> kholo <cheez> search karo/khojo".
        // Media verb wala vakya §1b ka hai (upar hi faisla ho
        // chuka); yahan sirf search-suffix rest claim hota hai.
        // Null-contract §1b jaisa: resolve 0/ambiguous -> null.
        if (lower.contains(" kholo ")) {
            val first = lower.substringBefore(" kholo ").trim()
            val rest = lower.substringAfter(" kholo ").trim()
            val q = if (rest.isNotEmpty()) parseSearchQuery(rest) else null
            if (first.isNotEmpty() && q != null && " me " !in first &&
                first !in setOf("ye", "yeh", "is", "yahan", "is app", "iss app")
            ) {
                if (first in OWN_SCREEN_NAMES) return null
                val matches = resolveApp(first)
                if (matches.size != 1) return null
                val (label, pkg) = matches[0]
                return VoicePlan(
                    goal = question,
                    steps = listOf(openStep(label, pkg), searchStep(label, q)),
                )
            }
        }

        // 2) Follow-up in-app search — sirf tab jab Flora se kholi gayi
        // app saamne ho (ForegroundAppContext ka maujooda niyam).
        if (foregroundApp != null) {
            // Cycle 1 — foreground app me seedha scroll (koi app
            // naam nahi, isliye koi resolve nahi, koi galat app nahi).
            val scrollDir = parseScrollDirection(lower)
            if (scrollDir != null) {
                return VoicePlan(
                    goal = question,
                    steps = listOf(
                        VoicePlanStep(
                            title = "Scroll karo",
                            executor = VoicePlanExecutor.SCROLL,
                            input = mapOf("forward" to scrollDir.toString()),
                            successCheck = "Screen scroll hui.",
                        ),
                    ),
                )
            }
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

    // Cycle 1 helpers — §1/§1b ke maujooda step shapes se
    // bilkul wahi title/successCheck, taaki generic plans ka
    // runner behaviour YouTube plans jaisa hi rahe (koi naya
    // executor shape nahi, sirf app naam badalta hai).
    private fun openStep(label: String, pkg: String): VoicePlanStep =
        VoicePlanStep(
            title = "$label kholo",
            executor = VoicePlanExecutor.APP_OPEN,
            input = mapOf("label" to label, "package" to pkg),
            successCheck = "App verified khuli.",
        )

    private fun searchStep(label: String, query: String): VoicePlanStep =
        VoicePlanStep(
            title = "$label me '$query' search karo",
            executor = VoicePlanExecutor.IN_APP_SEARCH,
            input = mapOf("query" to query),
            successCheck = "Search box me query type + submit hua.",
            fallback = "",
        )

    private fun parseScrollDirection(rest: String): Boolean? = when (rest.trim()) {
        "neeche scroll karo", "aage scroll karo", "scroll karo" -> true
        "upar scroll karo", "peeche scroll karo" -> false
        else -> null
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

        val ytMatches = if (forceApp == null) resolveApp("youtube") else emptyList()
        val yt = ytMatches.firstOrNull {
            it.first.equals("YouTube", ignoreCase = true)
        } ?: ytMatches.firstOrNull()
        val steps = mutableListOf<VoicePlanStep>()
        if (forceApp != null) {
            steps += VoicePlanStep(
                title = "${forceApp.first} kholo",
                executor = VoicePlanExecutor.APP_OPEN,
                input = mapOf("label" to forceApp.first, "package" to forceApp.second),
                successCheck = "App verified khuli.",
            )
        } else if (yt != null) {
            steps += VoicePlanStep(
                title = "YouTube kholo",
                executor = VoicePlanExecutor.APP_OPEN,
                input = mapOf("label" to yt.first, "package" to yt.second),
                successCheck = "App verified khuli.",
            )
        }
        steps += VoicePlanStep(
            title = "'$song' search karo",
            executor = VoicePlanExecutor.IN_APP_SEARCH,
            input = mapOf("query" to song),
            successCheck = "Search box me gaana type + submit hua.",
            fallback = "SEARCH_URL:" + youtubeSearchUrl(song),
        )
        // Batch 2 — TAP step sirf tab jab YouTube app maujood ho
        // (APP_OPEN step hai). YouTube app na ho to plan search par
        // khatam hota hai; browser results par tap out-of-scope hai.
        val tapPkg = when {
            forceApp != null && forceApp.first.equals("YouTube", ignoreCase = true) ->
                forceApp.second
            forceApp == null && yt != null -> yt.second
            else -> null
        }
        if (tapPkg != null) {
            steps += VoicePlanStep(
                title = "Pehla result play karo",
                executor = VoicePlanExecutor.TAP_FIRST_RESULT,
                input = mapOf("package" to tapPkg),
                successCheck = "YouTube saamne ho aur pehla result khula ho.",
                failMessage = "Search khol diya hai, pehla result khud tap karo (auto-play chain yahan ruka).",
            )
        }
        return VoicePlan(goal = original, steps = steps)
    }
}
