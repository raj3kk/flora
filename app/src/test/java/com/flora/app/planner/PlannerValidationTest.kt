package com.flora.app.planner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B4 planner pure-logic tests (junit only, production APK me nahi).
 * VM me JDK nahi hai, isliye ye tests CI par ./gradlew
 * :app:testDebugUnitTest se chalenge — "tests pass" ka daava
 * tabhi jab wo run green ho (test-execution PENDING note).
 */
class PlannerValidationTest {

    private fun step(
        title: String = "Step",
        tool: String = "NOTE",
        dependsOn: List<Int> = emptyList(),
        input: Map<String, String> = emptyMap(),
        preview: String = "",
    ): PlanStepDraft {
        return PlanStepDraft(
            title = title,
            tool = tool,
            input = input,
            successCheck = "check",
            fallback = "fallback",
            dependsOn = dependsOn,
            sideEffectPreview = preview,
        )
    }

    @Test
    fun validPlanPasses() {
        val draft = PlanDraft(
            goal = "Todo app banao",
            steps = listOf(step(tool = "PROJECT_SAVE_FILE", preview = "file banegi")),
            stops = listOf("MONEY"),
            noDo = listOf("delete mat karo"),
            rollback = "file hata do",
        )
        assertTrue(PlanValidator.validate(draft).isEmpty())
    }

    @Test
    fun unknownToolRejected() {
        val draft = PlanDraft("g", listOf(step(tool = "FLY_TO_MOON")))
        assertTrue(PlanValidator.validate(draft).isNotEmpty())
    }

    @Test
    fun forwardDependsOnRejected() {
        val draft = PlanDraft(
            "g",
            listOf(
                step(dependsOn = listOf(1)),
                step(),
            ),
        )
        assertTrue(PlanValidator.validate(draft).any { it.contains("depends_on") })
    }

    @Test
    fun passwordKeyRejected() {
        val draft = PlanDraft(
            "g",
            listOf(step(tool = "SUPABASE_CREATE_PROJECT", input = mapOf("dbPassword" to "x"), preview = "p")),
        )
        assertTrue(PlanValidator.validate(draft).any { it.contains("mana hai") })
    }

    @Test
    fun sideEffectNeedsPreview() {
        val draft = PlanDraft("g", listOf(step(tool = "GITHUB_CREATE_REPO", preview = "")))
        assertTrue(PlanValidator.validate(draft).any { it.contains("preview") })
    }

    @Test
    fun levelsParallelIndependentSteps() {
        val levels = PlanValidator.levels(3, listOf(emptyList(), emptyList(), listOf(0, 1)))
        assertEquals(2, levels.size)
        assertEquals(listOf(0, 1), levels[0])
        assertEquals(listOf(2), levels[1])
    }

    @Test
    fun estimateCountsCalls() {
        val estimate = PlanEstimateCalc.forPlan(4, 1)
        assertEquals(5, estimate.aiCalls)
        assertTrue(estimate.approxMinutes > 0)
    }

    @Test
    fun toolParsingCaseInsensitive() {
        assertEquals(PlanTool.GITHUB_PUSH_FILE, PlanTool.fromString("github_push_file"))
        assertEquals(null, PlanTool.fromString("nope"))
    }
}
