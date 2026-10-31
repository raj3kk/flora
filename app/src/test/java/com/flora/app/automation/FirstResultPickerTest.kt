package com.flora.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fixer (tester D2) — FirstResultPicker tests (pure JVM).
 * Service ka DFS Android node tree par chalta hai, isliye pick
 * ka faisla yahan test hota hai: barabar depth par PEHLA
 * jeet-ta hai (purana >= bug aakhri ko jitaata tha),
 * query-token preference sirf sabse gehre tier me lagti hai,
 * aur clear match na mile to first-pick par fall back hota hai.
 */
class FirstResultPickerTest {

    @Test
    fun equalDepthFirstWins() {
        val candidates = listOf(
            ResultCandidate(2, "pehla result"),
            ResultCandidate(2, "doosra result"),
            ResultCandidate(2, "teesra result"),
        )
        assertEquals(0, FirstResultPicker.pick(candidates, null))
    }

    @Test
    fun deepestTierWinsOverShallower() {
        val candidates = listOf(
            ResultCandidate(1, "outer container"),
            ResultCandidate(3, "gehra result"),
            ResultCandidate(2, "beech result"),
        )
        assertEquals(1, FirstResultPicker.pick(candidates, null))
    }

    @Test
    fun queryTokenPrefersMatchingRowInDeepestTier() {
        val candidates = listOf(
            ResultCandidate(2, "kuch aur gaana"),
            ResultCandidate(2, "arijit singh tum hi ho"),
        )
        assertEquals(1, FirstResultPicker.pick(candidates, "arijit ka"))
    }

    @Test
    fun queryTokenKeepsFirstRowWhenItMatches() {
        val candidates = listOf(
            ResultCandidate(2, "arijit singh live"),
            ResultCandidate(2, "doosra kalakaar"),
        )
        assertEquals(0, FirstResultPicker.pick(candidates, "arijit"))
    }

    @Test
    fun noClearTokenMatchFallsBackToFirstPick() {
        val candidates = listOf(
            ResultCandidate(2, "pehla result"),
            ResultCandidate(2, "doosra result"),
        )
        assertEquals(0, FirstResultPicker.pick(candidates, "xyzabc"))
    }

    @Test
    fun emptyCandidatesIsNull() {
        assertNull(FirstResultPicker.pick(emptyList(), "arijit"))
    }

    @Test
    fun shortTokensIgnored() {
        assertEquals(emptyList<String>(), FirstResultPicker.queryTokens("ka"))
        assertEquals(listOf("arijit"), FirstResultPicker.queryTokens("arijit ka"))
    }
}
