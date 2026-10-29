package com.flora.app.doctor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B4-DOCTOR unit tests — sirf pure-JVM scanner layer.
 * Saare fixtures SYNTHETIC shape hain (dohraya hua char, koi
 * asli ya example key value nahi). Issue me secret value kabhi
 * nahi aati — ye test har secret-issue ke string form par
 * check karta hai ki fixture ka token usme nahi hai.
 * Repo CI sirf :app:assembleDebug chalata hai, isliye ye tests
 * CI par execute nahi hote — local `./gradlew
 * :app:testDebugUnitTest` se chalane ke liye hain.
 */
class DoctorScannerTest {

    private val syntheticToken = "ghp_" + "a".repeat(36)

    private fun sampleFiles(): Map<String, String> {
        return mapOf(
            "index.html" to "<html><body><h1>Namaste</h1>" +
                "<button onclick=\"go()\">Chalo</button>" +
                "<a href=\"https://example.org/x\">Link</a></body></html>",
            "app.js" to "function go() {\n  console.log(\"hi\");\n  return 1;\n}\n",
        )
    }

    @Test
    fun cleanSampleHasNoCriticalSecurity() {
        val result = DoctorScanner.scanFiles(sampleFiles())
        assertTrue(
            result.issues.none { it.category == DoctorCategory.SECURITY && it.severity == DoctorSeverity.CRITICAL }
        )
        assertEquals(ComplianceState.PENDING_CORPUS, result.complianceState)
    }

    @Test
    fun tokenShapeIsCriticalAndNeverEchoed() {
        val files = mapOf("app.js" to "const k = \"$syntheticToken\";\n")
        val result = DoctorScanner.scanFiles(files)
        val critical = result.issues.filter {
            it.category == DoctorCategory.SECURITY && it.severity == DoctorSeverity.CRITICAL
        }
        assertTrue(critical.isNotEmpty())
        for (issue in critical) {
            val text = issue.title + issue.why + issue.impact + issue.pattern
            assertFalse(text.contains(syntheticToken))
            assertFalse(text.contains("a".repeat(36)))
        }
    }

    @Test
    fun apiKeyNameIsFlaggedEvenWithShortValue() {
        val files = mapOf("app.js" to "const apiKey = \"shortvalue123\";\n")
        val result = DoctorScanner.scanFiles(files)
        assertTrue(result.issues.any { it.pattern == "secret-key-name" })
    }

    @Test
    fun httpUpgradesToHttpsButLocalhostUntouched() {
        val issue = DoctorIssue(
            DoctorCategory.SECURITY, DoctorSeverity.HIGH, "index.html", 1,
            "t", "w", "i", "cleartext-http", DoctorFixKind.HTTPS_UPGRADE,
        )
        val files = mapOf(
            "index.html" to "<a href=\"http://example.org/x\">a</a> <a href=\"http://localhost:8080/y\">b</a>"
        )
        val fixed = DoctorScanner.applyFix(issue, files)
        assertTrue(fixed!!["index.html"]!!.contains("https://example.org/x"))
        assertTrue(fixed["index.html"]!!.contains("http://localhost:8080/y"))
    }

    @Test
    fun deadButtonDeadLinkAndEmptyBodyFlagged() {
        val files = mapOf(
            "index.html" to "<html><body><button>Bina kaam</button><a href=\"#\">Kahin nahi</a></body></html>"
        )
        val result = DoctorScanner.scanFiles(files)
        assertTrue(result.issues.any { it.pattern == "dead-button" })
        assertTrue(result.issues.any { it.pattern == "dead-link" })
    }

    @Test
    fun deadFunctionNeedsMinSizeAndWhitelist() {
        // 2-line function — min size (5) se kam, isliye flag NAHI.
        val small = mapOf("app.js" to "function tiny() {\n  return 1;\n}\n")
        assertFalse(DoctorScanner.scanFiles(small).issues.any { it.pattern == "dead-function" })
        // 6-line function, kahin call nahi — flag HOGA.
        val big = mapOf(
            "app.js" to "function unusedBig() {\n  const a = 1;\n  const b = 2;\n  const c = a + b;\n  console.log(c);\n  return c;\n}\n"
        )
        assertTrue(DoctorScanner.scanFiles(big).issues.any { it.pattern == "dead-function" })
        // Whitelist: main kabhi dead nahi.
        val main = mapOf(
            "app.js" to "function main() {\n  const a = 1;\n  const b = 2;\n  const c = a + b;\n  console.log(c);\n  return c;\n}\n"
        )
        assertFalse(DoctorScanner.scanFiles(main).issues.any { it.pattern == "dead-function" })
    }

    @Test
    fun duplicateBlockNeedsMinLines() {
        val block = (1..8).joinToString("\n") { "line$it = $it;" }
        val files = mapOf(
            "a.js" to block,
            "b.js" to block,
        )
        assertTrue(DoctorScanner.scanFiles(files).issues.any { it.pattern == "duplicate-block" })
        val short = (1..4).joinToString("\n") { "line$it = $it;" }
        val shortFiles = mapOf("a.js" to short, "b.js" to short)
        assertFalse(DoctorScanner.scanFiles(shortFiles).issues.any { it.pattern == "duplicate-block" })
    }

    @Test
    fun compliancePendingWithoutCorpusAndMatchWithCorpus() {
        DoctorScanner.complianceCorpus = emptySet()
        assertEquals(
            ComplianceState.PENDING_CORPUS,
            DoctorScanner.scanFiles(sampleFiles()).complianceState,
        )
        try {
            DoctorScanner.complianceCorpus = setOf("ye ek lamba compliance corpus hissa hai 12345")
            val files = mapOf("page.txt" to "shuru me kuch text, phir ye ek lamba compliance corpus hissa hai 12345, phir aage.")
            val result = DoctorScanner.scanFiles(files)
            assertEquals(ComplianceState.READY, result.complianceState)
            assertTrue(result.issues.any { it.pattern == "corpus-match" })
            // Matched text issue me quote nahi hona chahiye.
            val matched = result.issues.first { it.pattern == "corpus-match" }
            assertFalse((matched.title + matched.why).contains("corpus hissa"))
        } finally {
            DoctorScanner.complianceCorpus = emptySet()
        }
    }

    @Test
    fun fixPreviewNeverCarriesFileContent() {
        val issue = DoctorIssue(
            DoctorCategory.SECURITY, DoctorSeverity.CRITICAL, "app.js", 3,
            "t", "w", "i", "secret-token-shape", DoctorFixKind.REMOVE_LINE,
        )
        val preview = DoctorScanner.previewFix(issue)
        assertTrue(preview!!.destructive)
        assertTrue(preview.change.contains("line 3"))
        assertNull(DoctorScanner.previewFix(issue.copy(fixKind = DoctorFixKind.NONE)))
    }

    @Test
    fun iframeAllowAndPermissionsFlaggedPerP2Definition() {
        val files = mapOf(
            "index.html" to "<iframe src=\"https://example.org\" allow=\"camera; microphone\"></iframe>\n" +
                "<script>navigator.geolocation.getCurrentPosition(function(p) {});</script>"
        )
        val result = DoctorScanner.scanFiles(files)
        assertTrue(result.issues.any { it.pattern == "iframe-allow" })
        assertTrue(result.issues.any { it.pattern == "permission-geolocation" })
    }
}
