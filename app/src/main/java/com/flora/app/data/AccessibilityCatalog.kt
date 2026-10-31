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
 */
object AccessibilityCatalog {

    data class LabelRequirement(val screen: String, val element: String, val label: String)

    /** Audit checklist — har entry par screen reader me ye label bolna chahiye. */
    val REQUIREMENTS: List<LabelRequirement> = listOf(
        LabelRequirement("home", "orb-button", "Flora se baat karein"),
        LabelRequirement("home", "mic-button", "Mic se bolein"),
        LabelRequirement("chat", "send-button", "Message bhejein"),
        LabelRequirement("chat", "message-input", "Message likhein"),
        LabelRequirement("settings", "update-button", "App update check karein"),
        LabelRequirement("settings", "permissions-button", "Permissions dekhein"),
        LabelRequirement("catcher", "copy-button", "Report copy karein"),
        LabelRequirement("catcher", "share-button", "Report share karein"),
        LabelRequirement("catcher", "note-input", "Apni note likhein"),
        LabelRequirement("builder", "preview-button", "Preview dekhein"),
        LabelRequirement("reminders", "add-button", "Naya reminder banayein"),
        LabelRequirement("docs", "new-doc-button", "Naya document banayein")
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
