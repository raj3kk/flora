package com.flora.app.doctor

import com.flora.app.data.BuilderService
import com.flora.app.tools.KeyInputGuard

/**
 * B4-DOCTOR scan engine (DOC-P1-1 / DOC-P1-2 / DOC-P2-2 / DOC-P2-3 / DOC-P2-4).
 *
 * Pure Kotlin — koi Android dependency nahi, isliye unit-testable
 * hai (KeyInputGuard jaisa). Scan SIRF text par chalta hai
 * (Vercel deploy ka text-only limit — DOC-P0-1 gate isi par hai).
 *
 * SAKHT NIYAM — secret kabhi issue me nahi:
 * - Issue me file + line + pattern ka NAAM hi rehta hai.
 * - File ki koi line, koi token value, koi secret value kabhi
 *   issue, log, preview ya report me nahi aati — sirf pattern-level.
 * - Isliye preview/fix me bhi line ka content kabhi print nahi
 *   hota; user ko file + line number dikhta hai, content wo
 *   editor me khud dekhta hai.
 *
 * DOC-P2-2 paribhasha freeze (coordinator ruling): web/text
 * builder project me "permission" ka matlab SIRF ye teen hain —
 * (a) browser capability requests (geolocation/camera/mic/
 * notification) jo project code maangta hai, (b) third-party
 * API scopes/keys jo project use karta hai, (c) iframe/embed
 * permissions (allow attributes). Android manifest wali
 * paribhasha builder projects par lagu NAHI hoti — scanner
 * sirf in teenon ko scan karta hai, manifest rule kabhi nahi.
 *
 * DOC-P2-3 naap-tol (false positive se bachna):
 * - Dead function: body kam se kam 5 lines ki ho, tabhi flag.
 *   Whitelist: main/init/onload + HTML event attributes me
 *   referenced naam (onclick wagera) kabhi dead nahi.
 * - Duplicate block: normalized lines (blank + comment-only
 *   lines hatakar) ka kam se kam 8 lines ka repeat, tabhi flag.
 * - Duplicate function definition: naam do baar define ho, tabhi.
 *
 * DOC-P2-4 compliance: corpus ke bina jhootha scan NAHI.
 * complianceCorpus khaali hai to state PENDING_CORPUS rehta
 * hai aur compliance category ka koi issue nahi banta — screen
 * saaf likhti hai "corpus pending, scan nahi chal sakta".
 * Corpus mile to fix = remove/replace, KABHI hide nahi; match
 * hua text bhi issue me kabhi quote nahi hota, sirf
 * "corpus-match" pattern naam.
 *
 * Severity: CRITICAL sirf secret-leak class ko milta hai
 * (token shape, secret-bearing key naam, insecure storage me
 * password/token). Pre-deploy gate (DOC-P0-1) SIRF
 * SECURITY + CRITICAL ko rokta hai — baaki issues deploy
 * nahi rokte, Doctor screen par dikhte hain.
 */
enum class DoctorCategory {
    BUG,
    SECURITY,
    NOT_WORKING,
    DEAD_DUPLICATE,
    PERFORMANCE,
    COMPLIANCE
}

enum class DoctorSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class DoctorFixKind {
    NONE,
    HTTPS_UPGRADE,
    REMOVE_LINE,
    REMOVE_BLOCK
}

enum class ComplianceState {
    PENDING_CORPUS,
    READY
}

data class DoctorIssue(
    val category: DoctorCategory,
    val severity: DoctorSeverity,
    val file: String,
    val line: Int,
    val title: String,
    val why: String,
    val impact: String,
    val pattern: String,
    val fixKind: DoctorFixKind = DoctorFixKind.NONE,
    val blockLines: Int = 1,
)

data class DoctorScanResult(
    val issues: List<DoctorIssue>,
    val complianceState: ComplianceState,
    val scannedFiles: Int,
)

data class DoctorFixPreview(
    val change: String,
    val notTouched: String,
    val risk: String,
    val rollback: String,
    val destructive: Boolean,
)

object DoctorScanner {

    /** Compliance reference corpus — production me khaali = pending state. Test se bhara ja sakta hai. */
    var complianceCorpus: Set<String> = emptySet()

    private val deadWhitelist = setOf("main", "init", "onload", "setup", "start")
    private const val MIN_DEAD_BODY_LINES = 5
    private const val MIN_DUPLICATE_BLOCK_LINES = 8
    private const val MIN_DATA_URI_CHARS = 50_000

    private val keyNameAssign = Regex("(?i)\\b(api[_-]?key|apiKey|secret[_-]?key|access[_-]?token|auth[_-]?token)\\s*[:=]")
    private val placeholderValue = Regex("(?i)YOUR|EXAMPLE|PLACEHOLDER|TODO|XXX|CHANGEME")
    private val genericLongToken = Regex("^[A-Za-z0-9_\\-]{40,}$")
    private val functionDef = Regex("\\bfunction\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
    private val constArrowFn = Regex("\\bconst\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(\\([^)]*\\)|[A-Za-z_][A-Za-z0-9_]*)\\s*=>")
    private val htmlEventRef = Regex("(?i)on\\w+\\s*=\\s*\"([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
    private val onclickCall = Regex("(?i)on\\w+\\s*=\\s*\"([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
    private val iframeAllow = Regex("(?i)<iframe[^>]*\\ballow\\s*=\\s*\"([^\"]*)\"")
    private val dataUriStart = Regex("data:[A-Za-z]+/[A-Za-z0-9+\\-\\.]+;base64,")

    fun scanFiles(files: Map<String, String>): DoctorScanResult {
        val out = mutableListOf<DoctorIssue>()
        val defined = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        val definedBodyLines = mutableMapOf<String, Int>()
        for ((name, content) in files) {
            collectFunctions(name, content, defined, definedBodyLines)
        }
        val htmlRefs = mutableSetOf<String>()
        for ((_, content) in files) {
            for (m in htmlEventRef.findAll(content)) htmlRefs += m.groupValues[1]
        }
        for ((name, content) in files) {
            out += scanOneFile(name, content, files, defined, definedBodyLines, htmlRefs)
        }
        out += duplicateBlocks(files)
        out += duplicateDefinitions(defined)
        val complianceState = if (complianceCorpus.isEmpty()) {
            ComplianceState.PENDING_CORPUS
        } else {
            ComplianceState.READY
        }
        if (complianceState == ComplianceState.READY) {
            out += complianceMatches(files)
        }
        val sorted = out.sortedWith(compareBy({ it.file }, { it.line }, { it.category.name }))
        return DoctorScanResult(sorted, complianceState, files.size)
    }

    private fun collectFunctions(
        name: String,
        content: String,
        defined: MutableMap<String, MutableList<Pair<String, Int>>>,
        bodyLines: MutableMap<String, Int>,
    ) {
        val lines = content.lines()
        for ((index, line) in lines.withIndex()) {
            val fnName = functionDef.find(line)?.groupValues?.get(1)
                ?: constArrowFn.find(line)?.groupValues?.get(1)
                ?: continue
            defined.getOrPut(fnName) { mutableListOf() } += (name to (index + 1))
            bodyLines["$name:$fnName:${index + 1}"] = bodyLineCount(lines, index)
        }
    }

    /** Definition line se brace-depth gin kar body ki lines (min 1). */
    private fun bodyLineCount(lines: List<String>, start: Int): Int {
        var depth = 0
        var seenOpen = false
        for (i in start until lines.size) {
            for (ch in lines[i]) {
                if (ch == '{') {
                    depth += 1
                    seenOpen = true
                }
                if (ch == '}') depth -= 1
            }
            if (seenOpen && depth <= 0) return i - start + 1
        }
        return lines.size - start
    }

    private fun scanOneFile(
        name: String,
        content: String,
        allFiles: Map<String, String>,
        defined: Map<String, List<Pair<String, Int>>>,
        bodyLines: Map<String, Int>,
        htmlRefs: Set<String>,
    ): List<DoctorIssue> {
        val out = mutableListOf<DoctorIssue>()
        val lines = content.lines()
        val lowerContent = content.lowercase()
        for ((index, line) in lines.withIndex()) {
            val lineNo = index + 1
            val lower = line.lowercase()
            out += securityForLine(name, lineNo, line, lower)
            if (lower.contains("http://") && !lower.contains("http://localhost") &&
                !lower.contains("http://127.0.0.1")
            ) {
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, lineNo,
                    "Bina encryption wala link (http) mila hai",
                    "Ye link encrypted nahi hai — beech me koi padh/badalsakta hai.",
                    "Data leak ya badla hua page dikh sakta hai.",
                    "cleartext-http", DoctorFixKind.HTTPS_UPGRADE,
                )
            }
            if (Regex("(?i)<a\\b[^>]*href\\s*=\\s*\"(#|\\s*)\"").containsMatchIn(line)) {
                out += DoctorIssue(
                    DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, lineNo,
                    "Dead link mila hai (kahin nahi jata)",
                    "Link ka address khaali ya # hai — dabane par kuch nahi hoga.",
                    "User atak jayega, lagega button kharab hai.",
                    "dead-link",
                )
            }
            if (Regex("(?i)<button\\b").containsMatchIn(line) &&
                !lower.contains("onclick") && !lower.contains("type=\"submit\"")
            ) {
                val id = Regex("(?i)id\\s*=\\s*\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: ""
                val hasListener = id.isNotBlank() && allFiles.values.any { c ->
                    c.contains("getElementById(\"$id\")") || c.contains("getElementById('$id')") ||
                        c.contains("querySelector(\"#$id\")") || c.contains("querySelector('#$id')")
                }
                if (!hasListener) {
                    out += DoctorIssue(
                        DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, lineNo,
                        "Dead button lag raha hai (koi kaam juda nahi)",
                        "Is button par na click ka kaam hai, na submit — dabane par kuch nahi hoga.",
                        "User ko lagega feature toota hua hai.",
                        "dead-button",
                    )
                }
            }
            if (lower.contains("fetch(") &&
                (lower.contains("example.com") || lower.contains("placeholder") || lower.contains("todo"))
            ) {
                out += DoctorIssue(
                    DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, lineNo,
                    "API call adhura lag raha hai (sample address)",
                    "Ye call sample/adhure address par ja raha hai — asli data nahi aayega.",
                    "Screen khaali ya error dikhayegi.",
                    "placeholder-api",
                )
            }
            if (lower.contains(".innerhtml") && lower.contains("=")) {
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.MEDIUM, name, lineNo,
                    "innerHTML se page badla ja raha hai",
                    "Is raaste se galat code bhi page me ghus sakta hai.",
                    "Suraksha ka khatra ho sakta hai.",
                    "inner-html",
                )
            }
            if (lower.contains("document.write(")) {
                out += DoctorIssue(
                    DoctorCategory.PERFORMANCE, DoctorSeverity.MEDIUM, name, lineNo,
                    "document.write mila hai (page ko dheema karta hai)",
                    "Ye purana tareeka page ko rok kar likhta hai.",
                    "Page dheere khulega.",
                    "document-write",
                )
            }
            if (lower.contains("javascript:")) {
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, lineNo,
                    "javascript: link mila hai",
                    "Ye link code chalata hai, seedha page nahi kholta.",
                    "Galat code chalne ka khatra.",
                    "javascript-url",
                )
            }
            for (call in onclickCall.findAll(line)) {
                val called = call.groupValues[1]
                if (called !in defined) {
                    // defined map me naam na ho = function kahin bani hi nahi.
                    out += DoctorIssue(
                        DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, lineNo,
                        "Click par jo kaam bulaya gaya hai, wo bana hi nahi",
                        "Button dabane par wo kaam milega hi nahi — naam ka function project me nahi hai.",
                        "Click karne par error aayega, kuch nahi hoga.",
                        "undefined-handler",
                    )
                }
            }
        }
        // Empty screen: khaali file ya khaali body.
        if (content.isBlank()) {
            out += DoctorIssue(
                DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, 1,
                "Ye file khaali hai",
                "Isme kuch likha hi nahi hai.",
                "Is file ka screen khaali dikhega.",
                "empty-file",
            )
        } else if (Regex("(?is)<body>\\s*</body>").containsMatchIn(content)) {
            out += DoctorIssue(
                DoctorCategory.NOT_WORKING, DoctorSeverity.MEDIUM, name, 1,
                "Page ka body khaali hai",
                "Page ke andar dikhane ko kuch nahi hai.",
                "Khulta page khaali dikhega.",
                "empty-body",
            )
        }
        // Unbalanced delimiters (heuristic — strings ke andar ke
        // brackets bhi gin liye jate hain, isliye ye MEDIUM hi hai,
        // CRITICAL kabhi nahi; gate isse nahi rukta).
        val opens = content.count { it == '{' || it == '(' || it == '[' }
        val closes = content.count { it == '}' || it == ')' || it == ']' }
        if (opens != closes) {
            out += DoctorIssue(
                DoctorCategory.BUG, DoctorSeverity.MEDIUM, name, lines.size,
                "Brackets ka hisaab barabar nahi lag raha",
                "Khulne wale aur band hone wale brackets ki ginti alag hai.",
                "Code chal nahi sakta ya aadha chalega.",
                "unbalanced-delimiters",
            )
        }
        // Data-URI bhaari asset (ek line se badi bhi ho sakti hai —
        // isliye poore content par check).
        val dataMatch = dataUriStart.find(content)
        if (dataMatch != null) {
            val segment = content.substring(dataMatch.range.first)
                .takeWhile { it != '"' && it != '\'' && it != '<' }
            if (segment.length > MIN_DATA_URI_CHARS) {
                val lineNo = content.substring(0, dataMatch.range.first).count { it == '\n' } + 1
                out += DoctorIssue(
                    DoctorCategory.PERFORMANCE, DoctorSeverity.MEDIUM, name, lineNo,
                    "Page ke andar bahut bada image/data chipka hua hai",
                    "Itna bada data page ke saath hi load hoga.",
                    "Page bahut dheere khulega.",
                    "large-data-uri",
                )
            }
        }
        // Iframe allow (P2-2(c)) + browser capability requests (P2-2(a)).
        for (m in iframeAllow.findAll(content)) {
            val allowed = m.groupValues[1].lowercase()
            if (allowed.contains("camera") || allowed.contains("microphone") || allowed.contains("geolocation")) {
                val lineNo = content.substring(0, m.range.first).count { it == '\n' } + 1
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, lineNo,
                    "Embed me camera/mic/location ki permission maangi gayi hai",
                    "Bahari page ko device ki permission mil rahi hai.",
                    "Bahari page camera/mic/location use kar sakta hai.",
                    "iframe-allow",
                )
            }
        }
        if (lowerContent.contains("navigator.geolocation")) {
            out += DoctorIssue(
                DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, 1,
                "Location ki permission maangi ja rahi hai",
                "Code user ki location maang raha hai.",
                "User ki location bahar ja sakti hai.",
                "permission-geolocation",
            )
        }
        if (lowerContent.contains("getusermedia")) {
            out += DoctorIssue(
                DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, 1,
                "Camera/mic ki permission maangi ja rahi hai",
                "Code camera ya mic maang raha hai.",
                "Camera/mic bahari kaam me use ho sakta hai.",
                "permission-media",
            )
        }
        if (lowerContent.contains("notification.requestpermission")) {
            out += DoctorIssue(
                DoctorCategory.SECURITY, DoctorSeverity.HIGH, name, 1,
                "Notification ki permission maangi ja rahi hai",
                "Code notification bhejne ki permission maang raha hai.",
                "User ko extra notifications mil sakte hain.",
                "permission-notification",
            )
        }
        // Dead functions (P2-3): min size + whitelist; HTML refs + calls gin kar.
        val allText = allFiles.values.joinToString("\n")
        for ((fnName, defs) in defined) {
            if (fnName in deadWhitelist || fnName in htmlRefs) continue
            for ((defFile, defLine) in defs) {
                if (defFile != name) continue
                val body = bodyLines["$name:$fnName:$defLine"] ?: 0
                if (body < MIN_DEAD_BODY_LINES) continue
                val callPattern = Regex("\\b$fnName\\s*\\(")
                val calls = callPattern.findAll(allText).count()
                // Har definition khud ek match deti hai — kul matches
                // definitions se zyada na hon to koi call nahi, to dead.
                val totalDefs = defined[fnName]?.size ?: 0
                if (calls <= totalDefs) {
                    out += DoctorIssue(
                        DoctorCategory.DEAD_DUPLICATE, DoctorSeverity.LOW, name, defLine,
                        "Ye kaam (function) kahin use nahi ho raha",
                        "Ye code likha to hai, par project me kahin se bulaya nahi jata.",
                        "Bekaar code — padhna mushkil, galti chhup sakti hai.",
                        "dead-function",
                    )
                }
            }
        }
        return out
    }

    private fun securityForLine(
        name: String,
        lineNo: Int,
        line: String,
        lower: String,
    ): List<DoctorIssue> {
        val out = mutableListOf<DoctorIssue>()
        // Insecure storage: password/token/secret local/session storage me.
        if ((lower.contains("localstorage.setitem") || lower.contains("sessionstorage.setitem")) &&
            (lower.contains("password") || lower.contains("token") || lower.contains("secret"))
        ) {
            out += DoctorIssue(
                DoctorCategory.SECURITY, DoctorSeverity.CRITICAL, name, lineNo,
                "Password/token seedha browser storage me rakha ja raha hai",
                "Ye storage koi bhi page ka code padh sakta hai.",
                "Koi bhi password/token chura sakta hai.",
                "insecure-storage", DoctorFixKind.REMOVE_LINE,
            )
        }
        // Secret-bearing key naam (note (h): validator ki key-denylist
        // apiKey jaisi key nahi pakadti — doctor yahan pakadta hai).
        val keyAssign = keyNameAssign.find(line)
        if (keyAssign != null) {
            val rhs = line.substring(keyAssign.range.last + 1).trim().trim('"', '\'', ' ', ',', ';')
            if (rhs.isNotEmpty() && !placeholderValue.containsMatchIn(rhs)) {
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.CRITICAL, name, lineNo,
                    "Code me hi secret wali key likhi hui hai",
                    "Key code ke andar likhi hai — jo code dekhega, key bhi dekh lega.",
                    "Key leak ho jayegi, koi bhi use kar sakta hai.",
                    "secret-key-name", DoctorFixKind.REMOVE_LINE,
                )
            }
        }
        // Token shapes — sirf pattern naam, value kabhi nahi.
        val tokens = line.split(Regex("[\\s\"'`,;()\\[\\]{}<>]+")).filter { it.isNotBlank() }
        for (rawToken in tokens) {
            val token = rawToken.trim('.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}', '<', '>')
            if (token.isEmpty()) continue
            var serviceShape = false
            for (service in BuilderService.values()) {
                if (KeyInputGuard.matchesServiceShape(service, token)) serviceShape = true
            }
            val nearKeyWord = lower.contains("key") || lower.contains("token") || lower.contains("secret")
            val longToken = genericLongToken.matches(token) && token.any { it.isDigit() } && token.any { it.isLetter() }
            if (serviceShape || (nearKeyWord && longToken)) {
                out += DoctorIssue(
                    DoctorCategory.SECURITY, DoctorSeverity.CRITICAL, name, lineNo,
                    "Code me key/token jaisi cheez likhi hui hai",
                    "Ye key/token code me khula pada hai — deploy hote hi sabke saamne aa jayega.",
                    "Key leak ho jayegi, koi bhi use kar sakta hai. Iske saath deploy kabhi nahi.",
                    "secret-token-shape", DoctorFixKind.REMOVE_LINE,
                )
                break
            }
        }
        return out
    }

    private fun duplicateBlocks(files: Map<String, String>): List<DoctorIssue> {
        val out = mutableListOf<DoctorIssue>()
        val seen = mutableMapOf<String, Pair<String, Int>>()
        for ((name, content) in files) {
            val normalized = content.lines()
                .mapIndexed { i, l -> (i + 1) to l.trim() }
                .filter { (_, l) -> l.isNotEmpty() && !l.startsWith("//") && !l.startsWith("/*") && !l.startsWith("*") }
            for (start in normalized.indices) {
                if (start + MIN_DUPLICATE_BLOCK_LINES > normalized.size) break
                val block = normalized.subList(start, start + MIN_DUPLICATE_BLOCK_LINES)
                    .joinToString("\n") { it.second }
                val first = seen[block]
                if (first == null) {
                    seen[block] = (name to normalized[start].first)
                } else if (first.first != name || first.second != normalized[start].first) {
                    out += DoctorIssue(
                        DoctorCategory.DEAD_DUPLICATE, DoctorSeverity.LOW, name, normalized[start].first,
                        "Ye hissa doosri jagah bhi waisa ka waisa likha hai",
                        "Ek hi code do jagah hai — ek jagah sudhaar doosri par nahi pahunchega.",
                        "Do jagah alag-alag version ban jayenge, galti pakadna mushkil.",
                        "duplicate-block", DoctorFixKind.REMOVE_BLOCK,
                        blockLines = MIN_DUPLICATE_BLOCK_LINES,
                    )
                    // Ek file me ek hi duplicate-block report kaafi hai (shor se bachna).
                    break
                }
            }
        }
        return out
    }

    private fun duplicateDefinitions(
        defined: Map<String, List<Pair<String, Int>>>,
    ): List<DoctorIssue> {
        val out = mutableListOf<DoctorIssue>()
        for ((fnName, defs) in defined) {
            if (defs.size > 1) {
                val second = defs[1]
                out += DoctorIssue(
                    DoctorCategory.DEAD_DUPLICATE, DoctorSeverity.MEDIUM, second.first, second.second,
                    "Ye kaam do baar banaya gaya hai",
                    "Ek hi naam ka kaam do jagah define hai — kaunsa chalega, pakka nahi.",
                    "Galat wala chal sakta hai, samajhna mushkil.",
                    "duplicate-function",
                )
            }
        }
        return out
    }

    private fun complianceMatches(files: Map<String, String>): List<DoctorIssue> {
        val out = mutableListOf<DoctorIssue>()
        for ((name, content) in files) {
            for (entry in complianceCorpus) {
                if (entry.length < 20) continue
                val idx = content.indexOf(entry)
                if (idx >= 0) {
                    val lineNo = content.substring(0, idx).count { it == '\n' } + 1
                    // Matched text kabhi quote nahi — sirf pattern naam.
                    out += DoctorIssue(
                        DoctorCategory.COMPLIANCE, DoctorSeverity.HIGH, name, lineNo,
                        "Compliance corpus se milta hua hissa mila hai",
                        "Ye hissa reference corpus se milta hai — creator ka ho sakta hai.",
                        "Fix = hatana ya apne shabdon me badalna, kabhi chhupana nahi.",
                        "corpus-match", DoctorFixKind.REMOVE_LINE,
                    )
                    break
                }
            }
        }
        return out
    }

    /** Fix preview — user-bhasha, kya badlega / kya NAHI / risk / wapas. Content kabhi nahi. */
    fun previewFix(issue: DoctorIssue): DoctorFixPreview? {
        return when (issue.fixKind) {
            DoctorFixKind.HTTPS_UPGRADE -> DoctorFixPreview(
                "Sirf is line ka http link https ho jayega.",
                "Baaki lines, baaki files, koi naam ya design nahi chhedega.",
                "Agar wo server https par na chale to link toot sakta hai — tab rollback se wapas.",
                "Save se pehle wala version ProjectStore me rahega — ek tap me wapas.",
                false,
            )
            DoctorFixKind.REMOVE_LINE -> DoctorFixPreview(
                "Sirf file ki line ${issue.line} hatayi jayegi (pattern: ${issue.pattern}).",
                "Baaki lines aur baaki files nahi chhedegi.",
                "Agar wo line code ke liye zaroori thi to feature toot sakta hai — isliye ye destructive hai, har baar confirm ke baad hi hoga.",
                "Save se pehle wala version ProjectStore me rahega — ek tap me wapas.",
                true,
            )
            DoctorFixKind.REMOVE_BLOCK -> DoctorFixPreview(
                "Sirf line ${issue.line} se ${issue.blockLines} lines ka dohra hissa hataya jayega.",
                "Pehli wali copy aur baaki files nahi chhedegi.",
                "Agar doosri copy alag kaam kar rahi thi to feature toot sakta hai — destructive hai, har baar confirm ke baad hi hoga.",
                "Save se pehle wala version ProjectStore me rahega — ek tap me wapas.",
                true,
            )
            DoctorFixKind.NONE -> null
        }
    }

    /**
     * Fix lagao — files map par, sirf us issue ki file/line.
     * Null = is issue ka auto-fix nahi (insaan ko tay karna hai).
     * REMOVE_* destructive hain — caller (screen) har baar confirm
     * lekar hi ye call karega; ye function khud confirm nahi maangta.
     */
    fun applyFix(issue: DoctorIssue, files: Map<String, String>): Map<String, String>? {
        val content = files[issue.file] ?: return null
        val lines = content.lines().toMutableList()
        return when (issue.fixKind) {
            DoctorFixKind.HTTPS_UPGRADE -> {
                if (issue.line < 1 || issue.line > lines.size) return null
                val fixed = lines[issue.line - 1]
                    .replace("http://localhost", "§LOCAL§")
                    .replace("http://127.0.0.1", "§LOOP§")
                    .replace("http://", "https://")
                    .replace("§LOCAL§", "http://localhost")
                    .replace("§LOOP§", "http://127.0.0.1")
                if (fixed == lines[issue.line - 1]) return null
                lines[issue.line - 1] = fixed
                files + (issue.file to lines.joinToString("\n"))
            }
            DoctorFixKind.REMOVE_LINE -> {
                if (issue.line < 1 || issue.line > lines.size) return null
                lines.removeAt(issue.line - 1)
                files + (issue.file to lines.joinToString("\n"))
            }
            DoctorFixKind.REMOVE_BLOCK -> {
                if (issue.line < 1 || issue.line > lines.size) return null
                repeat(issue.blockLines) {
                    if (lines.size >= issue.line) lines.removeAt(issue.line - 1)
                }
                files + (issue.file to lines.joinToString("\n"))
            }
            DoctorFixKind.NONE -> null
        }
    }
}
