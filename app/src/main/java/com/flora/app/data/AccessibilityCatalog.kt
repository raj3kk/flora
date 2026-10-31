package com.flora.app.data

/**
 * P3-10 — Accessibility + language (screen-reader label audit catalog).
 *
 * Theme HAVE hai; screen-reader labels + language completeness ka
 * audit pending tha. Ye file audit ka checklist deti hai — har
 * screen ke har interactive element par content-description hona
 * chahiye; audit is catalog ke khilaaf chalega (tester ke paas):
 *
 * - Har button/icon ka bolne-layak label (sirf icon nahi).
 * - Har input field ka label + hint.
 * - Har status/info line ka padhne-layak text (khaali decorative
 *   element par label nahi — wo screen reader se chhupa rehta hai).
 * - Theme tests: dark/light + bada font — koi text katna nahi chahiye.
 *
 * Language completeness (G2P) Part 1 team ka scope hai; ye file
 * sirf accessibility audit hai, usse overlap nahi karti.
 *
 * STATIC AUDIT (fixer M5, 2026-10-31): catalog ke 12 labels ko
 * actual UI se milaya gaya (button texts + contentDescription
 * scan). Natija: poori UI me sirf 1 contentDescription hai
 * (PicturesScreen photos); baaki text buttons apna visible text
 * hi announce karte hain. Isliye catalog labels ko actual visible
 * texts se theek kiya gaya hai (neeche); orb koi button nahi hai
 * (visual only) aur builder me koi preview button nahi hai
 * (sirf preview section text) — dono entries isi sach ke saath
 * darj hain. Phone par screen-reader audit Part 4 flow me hoga.
 */
object AccessibilityCatalog {

    data class LabelRequirement(val screen: String, val element: String, val label: String)

    /** Audit checklist — har entry par screen reader me ye label bolna chahiye. */
    val REQUIREMENTS: List<LabelRequirement> = listOf(
        LabelRequirement("home", "orb-display", "Flora orb (visual only, koi button nahi)"),
        LabelRequirement("home", "mic-button", "Mic"),
        LabelRequirement("chat", "send-button", "Bhejo"),
        LabelRequirement("chat", "message-input", "Message likhiye ya mic dabaiye"),
        LabelRequirement("settings", "update-button", "Update check karo"),
        LabelRequirement("settings", "permissions-button", "Permissions dobara dekho"),
        LabelRequirement("catcher", "copy-button", "Report copy karo"),
        LabelRequirement("catcher", "share-button", "Report share karo"),
        LabelRequirement("catcher", "note-input", "Ye kar raha tha tab hua…"),
        LabelRequirement("builder", "preview-section", "Preview (sirf is file ka):"),
        LabelRequirement("reminders", "add-button", "Reminder lagao"),
        LabelRequirement("docs", "new-doc-button", "Document save karo")
    )

    /** Theme tests — har release checklist me chalte hain. */
    val THEME_TESTS: List<String> = listOf(
        "dark theme: saara text padhne layak ho, koi kaala-par-kaala nahi",
        "light theme: saara text padhne layak ho, koi safed-par-safed nahi",
        "bada font (largest): koi button/label katna nahi chahiye",
        "screen reader ON: har screen ka main kaam bina dekhe ho sake"
    )

    fun requirementsFor(screen: String): List<LabelRequirement> {
        return REQUIREMENTS.filter { it.screen == screen }
    }
}
