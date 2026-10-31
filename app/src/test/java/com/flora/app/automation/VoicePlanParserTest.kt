package com.flora.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Planner-first routing, Batch 1 — VoicePlanParser tests (pure JVM).
 * Resolver fake hai; koi Android class nahi chhooti.
 */
class VoicePlanParserTest {

    private val resolver: (String) -> List<Pair<String, String>> = { name ->
        when (name) {
            "youtube" -> listOf("YouTube" to "com.google.android.youtube")
            "spotify" -> listOf("Spotify" to "com.spotify.music")
            "music" -> listOf("MusicA" to "com.example.a", "MusicB" to "com.example.b")
            else -> emptyList()
        }
    }

    private val foreground = "com.google.android.youtube" to "YouTube"

    @Test
    fun compoundAurMedia() {
        val plan = VoicePlanParser.parse("youtube kholo aur arijit ka gaana bajao", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("YouTube", plan.steps[0].input["label"])
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[1].executor)
        assertEquals("arijit ka", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
        assertEquals("com.google.android.youtube", plan.steps[2].input["package"])
    }

    @Test
    fun compoundKholKMedia() {
        val plan = VoicePlanParser.parse("youtube khol k gana bajao arijit", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals("arijit", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
    }

    @Test
    fun compoundKholoToMedia() {
        val plan = VoicePlanParser.parse("youtube kholo to arijit ka gana bajao", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals("arijit ka", plan.steps[1].input["query"])
    }

    @Test
    fun standaloneMedia() {
        val plan = VoicePlanParser.parse("arijit ka gana bajao", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("arijit ka", plan.steps[1].input["query"])
        assertEquals(
            "SEARCH_URL:https://www.youtube.com/results?search_query=arijit+ka",
            plan.steps[1].fallback,
        )
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
    }

    @Test
    fun mediaWithoutYoutubeAppHasNoTapStep() {
        val noApp: (String) -> List<Pair<String, String>> = { emptyList() }
        val plan = VoicePlanParser.parse("arijit ka gana bajao", noApp)
        assertNotNull(plan)
        assertEquals(1, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[0].executor)
    }

    @Test
    fun compoundRestMediaNonYoutubeIsNull() {
        // Batch 1 rule: rest-media chain sirf YouTube par.
        assertNull(VoicePlanParser.parse("spotify kholo aur gaana bajao arijit", resolver))
    }

    @Test
    fun mediaWithoutSongIsNull() {
        assertNull(VoicePlanParser.parse("gaana bajao", resolver))
    }

    @Test
    fun unknownIsNull() {
        assertNull(VoicePlanParser.parse("aaj mausam kaisa hai", resolver))
    }

    @Test
    fun genericToIsNotASplitter() {
        assertNull(VoicePlanParser.parse("kal to chhutti hai", resolver))
    }

    @Test
    fun ambiguousAppIsNull() {
        assertNull(VoicePlanParser.parse("music kholo aur gaana bajao arijit", resolver))
    }

    @Test
    fun unknownAppIsNull() {
        assertNull(VoicePlanParser.parse("xyzapp kholo aur gaana bajao arijit", resolver))
    }

    @Test
    fun followUpSearchNeedsForeground() {
        val withFg = VoicePlanParser.parse("arijit search karo", resolver, foreground)
        assertNotNull(withFg)
        assertEquals(1, withFg!!.steps.size)
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, withFg.steps[0].executor)
        assertEquals("arijit", withFg.steps[0].input["query"])
        assertNull(VoicePlanParser.parse("arijit search karo", resolver, null))
    }

    @Test
    fun compoundSearch() {
        val plan = VoicePlanParser.parse("youtube kholo aur arijit search karo", resolver)
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("arijit", plan.steps[1].input["query"])
    }

    @Test
    fun songNameContainingAurFallsThroughToMedia() {
        val plan = VoicePlanParser.parse("tum aur hum gaana bajao", resolver)
        assertNotNull(plan)
        assertEquals("tum aur hum", plan!!.steps[1].input["query"])
    }

    @Test
    fun compoundRestUnrecognizedIsNull() {
        // Rest samajh na aaye to maujooda compound branch sambhalegi.
        assertNull(VoicePlanParser.parse("youtube kholo aur volume badhao", resolver))
    }

    // Voice-fix tests: video verbs gaana chain reuse karte hain.
    @Test
    fun standaloneVideoWithTitle() {
        val plan = VoicePlanParser.parse("arijit ka video chalao", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("arijit ka", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
        assertEquals("com.google.android.youtube", plan.steps[2].input["package"])
    }

    @Test
    fun standaloneVideoPlayKaroWithTitle() {
        val plan = VoicePlanParser.parse("video play karo arijit", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals("arijit", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
    }

    @Test
    fun compoundKholoSpaceVideoWithTitle() {
        val plan = VoicePlanParser.parse("youtube kholo video chalao arijit", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("YouTube", plan.steps[0].input["label"])
        assertEquals("arijit", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
    }

    @Test
    fun compoundKholoSpaceBareVideoOpensYoutubeOnly() {
        // Koi title nahi → search query nahi ban sakti, sirf open.
        val plan = VoicePlanParser.parse("youtube kholo video chalao", resolver)
        assertNotNull(plan)
        assertEquals(1, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("YouTube", plan.steps[0].input["label"])
    }

    @Test
    fun compoundKholoSpaceNonYoutubeIsOpenPlusSearch() {
        val plan = VoicePlanParser.parse("spotify kholo video chalao arijit", resolver)
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("Spotify", plan.steps[0].input["label"])
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[1].executor)
        assertEquals("arijit", plan.steps[1].input["query"])
    }

    @Test
    fun compoundKholoSpaceBareVideoNonYoutubeIsNull() {
        assertNull(VoicePlanParser.parse("spotify kholo video chalao", resolver))
    }

    @Test
    fun compoundAurVideoWithTitle() {
        val plan = VoicePlanParser.parse("youtube kholo aur video chalao arijit", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals("arijit", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
    }

    // Fixer regression: §1b (kholo-space) me naam wala app resolve
    // na ho / ambiguous ho / own-screen ho to null — §3 par
    // fall-through se YouTube plan nahi banega.
    @Test
    fun kholoSpaceUnknownAppIsNull() {
        assertNull(VoicePlanParser.parse("xyzapp kholo video chalao arijit", resolver))
    }

    @Test
    fun kholoSpaceAmbiguousAppIsNull() {
        assertNull(VoicePlanParser.parse("music kholo video chalao arijit", resolver))
    }

    @Test
    fun kholoSpaceOwnScreenIsNull() {
        assertNull(VoicePlanParser.parse("camera kholo video chalao arijit", resolver))
    }

    // Cycle 1 — generic in-app automation (sirf YouTube nahi).
    @Test
    fun genericTitledSearchSpotify() {
        val plan = VoicePlanParser.parse("spotify kholo arijit search karo", resolver)
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("Spotify", plan.steps[0].input["label"])
        assertEquals("com.spotify.music", plan.steps[0].input["package"])
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[1].executor)
        assertEquals("arijit", plan.steps[1].input["query"])
    }

    @Test
    fun bareOpenSpotify() {
        val plan = VoicePlanParser.parse("spotify kholo", resolver)
        assertNotNull(plan)
        assertEquals(1, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("Spotify", plan.steps[0].input["label"])
    }

    @Test
    fun bareOpenUnknownIsNull() {
        assertNull(VoicePlanParser.parse("xyzapp kholo", resolver))
    }

    @Test
    fun bareOpenAmbiguousIsNull() {
        assertNull(VoicePlanParser.parse("music kholo", resolver))
    }

    @Test
    fun bareOpenOwnScreenIsNull() {
        assertNull(VoicePlanParser.parse("camera kholo", resolver))
    }

    @Test
    fun meFormSearchSpotify() {
        val plan = VoicePlanParser.parse("spotify me arijit search karo", resolver)
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals("Spotify", plan.steps[0].input["label"])
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[1].executor)
        assertEquals("arijit", plan.steps[1].input["query"])
    }

    @Test
    fun meFormKholoSpotifyTapsFirstResult() {
        val plan = VoicePlanParser.parse("spotify me arijit kholo", resolver)
        assertNotNull(plan)
        assertEquals(3, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals(VoicePlanExecutor.IN_APP_SEARCH, plan.steps[1].executor)
        assertEquals("arijit", plan.steps[1].input["query"])
        assertEquals(VoicePlanExecutor.TAP_FIRST_RESULT, plan.steps[2].executor)
        assertEquals("com.spotify.music", plan.steps[2].input["package"])
    }

    @Test
    fun meFormUnknownIsNull() {
        assertNull(VoicePlanParser.parse("xyzapp me arijit search karo", resolver))
    }

    @Test
    fun meFormAmbiguousIsNull() {
        assertNull(VoicePlanParser.parse("music me arijit search karo", resolver))
    }

    @Test
    fun meFormScrollSpotify() {
        val plan = VoicePlanParser.parse("spotify me neeche scroll karo", resolver)
        assertNotNull(plan)
        assertEquals(2, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.APP_OPEN, plan.steps[0].executor)
        assertEquals(VoicePlanExecutor.SCROLL, plan.steps[1].executor)
        assertEquals("true", plan.steps[1].input["forward"])
    }

    @Test
    fun foregroundScrollOnly() {
        val fg = "com.spotify.music" to "Spotify"
        val plan = VoicePlanParser.parse("upar scroll karo", resolver, fg)
        assertNotNull(plan)
        assertEquals(1, plan!!.steps.size)
        assertEquals(VoicePlanExecutor.SCROLL, plan.steps[0].executor)
        assertEquals("false", plan.steps[0].input["forward"])
        assertNull(VoicePlanParser.parse("upar scroll karo", resolver, null))
    }
}
