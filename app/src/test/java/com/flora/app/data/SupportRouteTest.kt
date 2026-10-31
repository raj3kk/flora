package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3-11 SupportRoute tests (fixer M4 wiring).
 * Intent ka Android-side launch JVM unit test me nahi hota — yahan
 * sirf pure parts hain: destination, draft sanitization, sendable.
 */
class SupportRouteTest {

    @Test
    fun destinationIsWebsitePublishedSupport() {
        // Website ke privacy/terms/index pages par published mailbox.
        assertEquals("flipify.com@gmail.com", SupportRoute.SUPPORT_ADDRESS)
        assertTrue(SupportRoute.destinationLabel().contains("flipify.com@gmail.com"))
    }

    @Test
    fun draftIsSanitizedAndSendableOnlyWhenNonBlank() {
        assertFalse(SupportRoute.isSendable(SupportRoute.feedbackDraft("   ")))
        assertFalse(SupportRoute.isSendable(""))
        val draft = SupportRoute.feedbackDraft("  App atak raha hai\nkabhi kabhi  ")
        assertTrue(SupportRoute.isSendable(draft))
        assertFalse(draft.contains("\n"))
    }

    @Test
    fun consentCatalogHasFiveDefaultOffEntries() {
        // P3-05 wiring ka contract: 5 permissions, sab default OFF.
        assertEquals(5, ConsentCatalog.ENTRIES.size)
        assertTrue(ConsentCatalog.ENTRIES.all { !it.defaultOn })
        assertEquals(
            setOf("mic-24x7", "screen-view", "camera", "notification-read", "contacts"),
            ConsentCatalog.ENTRIES.map { it.key }.toSet()
        )
    }

    @Test
    fun accessibilityCatalogHasTwelveCorrectedLabels() {
        // P3-10 fixer M5 audit: 12 entries, actual UI texts se match.
        assertEquals(12, AccessibilityCatalog.REQUIREMENTS.size)
        val byElement = AccessibilityCatalog.REQUIREMENTS.associate { it.element to it.label }
        assertEquals("Report copy karo", byElement["copy-button"])
        assertEquals("Permissions dobara dekho", byElement["permissions-button"])
        assertEquals("Reminder lagao", byElement["add-button"])
    }
}
