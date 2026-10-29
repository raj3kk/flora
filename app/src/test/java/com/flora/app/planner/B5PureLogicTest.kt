package com.flora.app.planner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B5 unit tests — sirf pure-JVM logic (koi Android class nahi).
 * Fixtures SYNTHETIC shape hain (dohraya hua char, koi asli ya
 * example key value nahi). HONEST LIMIT: repo CI sirf
 * :app:assembleDebug chalata hai, isliye ye tests CI par execute
 * nahi hote; VM me JDK nahi hai, isliye yahan bhi run nahi hue
 * — "tests pass" ka daava nahi, execution PENDING (local
 * ./gradlew :app:testDebugUnitTest JDK machine par).
 */
class B5PureLogicTest {

    // Synthetic shapes — repeated chars, never a real value.
    private val githubShape = "ghp_" + "a".repeat(36)
    private val longToken = "b".repeat(48)

    @Test
    fun selfMapSafetyDropsSecretValue() {
        assertTrue(SelfMapSafety.containsSecretValue("token: $githubShape"))
        assertTrue(SelfMapSafety.containsSecretValue("Bearer $longToken"))
        assertTrue(SelfMapSafety.containsSecretValue("ye line $githubShape rakhti hai"))
        assertNull(SelfMapSafety.feature(SelfMapFeature("notes", "token: $githubShape")))
    }

    @Test
    fun selfMapSafetyKeepsStructuralNames() {
        // Structural naam (value nahi) survive karte hain.
        assertFalse(SelfMapSafety.containsSecretValue("BuilderTokens"))
        assertFalse(SelfMapSafety.containsSecretValue("ProjectStore se project files ka store"))
        val kept = SelfMapSafety.service(SelfMapService("github", "Builder-project repo create + file push (GitHubClient, B3)."))
        assertTrue(kept != null)
        assertEquals("github", kept?.name)
    }

    @Test
    fun quotaLogicCodeGenSetAndGates() {
        assertTrue(AiBuildQuotaLogic.CODE_GEN_TOOLS.contains(PlanTool.GITHUB_PUSH_FILE))
        assertTrue(AiBuildQuotaLogic.CODE_GEN_TOOLS.contains(PlanTool.VERCEL_DEPLOY))
        assertTrue(AiBuildQuotaLogic.CODE_GEN_TOOLS.contains(PlanTool.PROJECT_SAVE_FILE))
        assertFalse(AiBuildQuotaLogic.CODE_GEN_TOOLS.contains(PlanTool.GITHUB_CREATE_REPO))
        assertFalse(AiBuildQuotaLogic.CODE_GEN_TOOLS.contains(PlanTool.NOTE))
        assertNull(AiBuildQuotaLogic.buildGateMessage(4))
        assertTrue(AiBuildQuotaLogic.buildGateMessage(5) != null)
        assertTrue(AiBuildQuotaLogic.buildGateMessage(5)?.contains("Pro") == true)
        assertNull(AiBuildQuotaLogic.projectGateMessage(2))
        assertTrue(AiBuildQuotaLogic.projectGateMessage(3) != null)
        assertTrue(AiBuildQuotaLogic.isNewDay("2026-10-28", "2026-10-29"))
        assertFalse(AiBuildQuotaLogic.isNewDay("2026-10-29", "2026-10-29"))
    }

    @Test
    fun learnedLogicNormalizeMatchAndReliability() {
        assertEquals("mera todo app banao", LearnedEngineLogic.normalize("  Mera   Todo  App Banao "))
        val pattern = LearnedPattern(
            id = "lp-1",
            normalizedCommand = "mera todo app banao",
            tool = PlanTool.NOTE.name,
            actionLabel = "Todo note",
            input = emptyMap(),
            successCount = 0,
            failCount = 0,
            lastUsedMs = 0L,
            createdMs = 0L,
        )
        assertTrue(LearnedEngineLogic.matches(pattern, "Mera todo app banao"))
        assertTrue(LearnedEngineLogic.isReliable(pattern))
        assertFalse(LearnedEngineLogic.isReliable(pattern.copy(failCount = 3)))
        assertTrue(LearnedEngineLogic.isSafeOfflineTool(PlanTool.NOTE.name))
        assertTrue(LearnedEngineLogic.isSafeOfflineTool(PlanTool.MACRO_UI_STEP.name))
        assertFalse(LearnedEngineLogic.isSafeOfflineTool(PlanTool.VERCEL_DEPLOY.name))
    }

    @Test
    fun unifiedSearchRankSnippetDedupAndSensitive() {
        assertEquals(100, UnifiedSearchLogic.rank("todo app", "Mera todo app banao"))
        assertTrue(UnifiedSearchLogic.rank("todo", "kuch aur") == 0)
        assertTrue(UnifiedSearchLogic.snippet("a".repeat(200)).length <= 120)
        assertTrue(UnifiedSearchLogic.isSensitive("token: $githubShape"))
        assertFalse(UnifiedSearchLogic.isSensitive("kal kaam poora karna hai"))
        val first = UnifiedSearchResult("chat", listOf("chat"), "Chat", "mera todo app", 100L, 100)
        val dup = UnifiedSearchResult("project", listOf("project"), "Project", "mera todo app", 200L, 50)
        val merged = UnifiedSearchLogic.merge(listOf(first, dup))
        assertEquals(1, merged.size)
        assertEquals(listOf("chat", "project"), merged[0].sources)
        // Best rank jeetta hai (chat, 100) — sources merge hote hain.
        assertEquals("chat", merged[0].source)
    }

    @Test
    fun planValidatorForbiddenHelpers() {
        assertTrue(PlanValidator.isForbiddenKey("db_password"))
        assertTrue(PlanValidator.isForbiddenKey("token"))
        assertFalse(PlanValidator.isForbiddenKey("projectName"))
        assertTrue(PlanValidator.containsForbiddenAssignment("token: kuch"))
        assertFalse(PlanValidator.containsForbiddenAssignment("kal kaam poora karna hai"))
    }
}
