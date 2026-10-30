package com.flora.app.planner

/**
 * B5-07 tools consolidation — POORA registry, PlannerTools ko
 * EXTEND karke (REPLACE NAHI).
 *
 * Single construction site barkaraar hai: PlannerTools sirf
 * PlannerEngine:82 par banta hai (PlannerEngineFactory shared
 * instance). Ye registry doosri construction site NAHI hai — ye
 * sirf descriptor list hai (permission + licence + log audit ek
 * jagah se). Har tool ka asli execution wahi purana raasta hai
 * (B3 clients, ProjectStore, ya FloraApp ka maujooda flow) —
 * koi naya transport, naya controller, naya HttpURLConnection
 * nahi bana.
 *
 * BrowserController: wahi singleton object hai (tools/
 * BrowserController.kt:40) — naya controller NAHI bana, aur
 * planner me browser execution ka doosra raasta bhi NAHI bana
 * (beech ka raasta duplicate hota). Registry me browser
 * descriptor-only hai: executesVia = FloraApp ka maujooda
 * browser flow. Planner step se browser chalana ho to wahi
 * MACRO_UI_STEP adapter raasta hai (B4), seedha controller
 * call planner se nahi hota.
 *
 * Licence key null = is tool par koi plan licence key nahi
 * lagti (fail-open note ke saath) — key invent nahi ki gayi.
 * "builder" key wale tools par PlannerTools.execute entry par
 * PlanFeatures.isAllowed("builder") gate lagta hai (B5-08).
 *
 * Log audit: PlannerTools.execute ka har step + har tool audit
 * AutoLogStore me jata hai (engine ke auditSink se) — format
 * "[tool:<name>] ..." (plan log "[plan:<id> step:<n>]" se
 * alag prefix, wahi store).
 */
data class ToolDescriptor(
    val name: String,
    val kind: String,
    val permission: String,
    val licenceKey: String?,
    val executesVia: String,
)

object ToolRegistry {

    fun all(): List<ToolDescriptor> = listOf(
        ToolDescriptor(
            name = "github",
            kind = "builder-remote",
            permission = "User grant-once connection (GitHub, ConnectServices se)",
            licenceKey = "builder",
            executesVia = "PlannerTools.execute -> GitHubClient (B3)",
        ),
        ToolDescriptor(
            name = "vercel",
            kind = "builder-remote",
            permission = "User grant-once connection (Vercel, ConnectServices se)",
            licenceKey = "builder",
            executesVia = "PlannerTools.execute -> VercelClient (B3), pre-deploy gate ke saath",
        ),
        ToolDescriptor(
            name = "supabase_projects",
            kind = "builder-remote",
            permission = "User grant-once connection (Supabase, ConnectServices se)",
            licenceKey = "builder",
            executesVia = "PlannerTools.execute -> SupabaseProjectClient (B3)",
        ),
        ToolDescriptor(
            name = "project_store",
            kind = "builder-local",
            permission = "App-private files hi (koi device folder permission nahi)",
            licenceKey = "builder",
            executesVia = "PlannerTools.execute -> ProjectStore (B2)",
        ),
        ToolDescriptor(
            name = "browser",
            kind = "flora-surface",
            permission = "Flora browser flow (B1) — user ke saamne khulta hai, chhupke nahi",
            licenceKey = "browser",
            executesVia = "DESCRIPTOR-ONLY: FloraApp browser flow + BrowserController singleton (wahi object); planner se seedha execution path nahi — duplicate raasta mana hai",
        ),
        ToolDescriptor(
            name = "search_tools",
            kind = "web-lanes",
            permission = "User keys se web search lanes (Tavily/DuckDuckGo) — koi plan licence key nahi",
            licenceKey = null,
            executesVia = "FloraApp research flow -> SearchTools.search (maujooda caller, planner se nahi)",
        ),
        ToolDescriptor(
            name = "image_tools",
            kind = "web-generation",
            permission = "Public image generation (koi key nahi) — user ke saamne result dikhta hai",
            licenceKey = "image_gen",
            executesVia = "FloraApp / voice flow -> ImageTools.generateImage (maujooda caller)",
        ),
        ToolDescriptor(
            name = "website_publisher",
            kind = "builder-publish",
            permission = "User ke Supabase anon key se flora-sites upload — destination preview/upload se PEHLE dikhta hai",
            licenceKey = "builder",
            executesVia = "Maujooda caller -> WebsitePublisher.publish (planner step nahi — upload destination preview rule E2 hardening se)",
        ),
        ToolDescriptor(
            name = "heartbeat",
            kind = "background-sync",
            permission = "Koi naya permission nahi — linked account par hi, silent skip on fail",
            licenceKey = null,
            executesVia = "Background flow -> HeartbeatClient.send (maujooda caller)",
        ),
        ToolDescriptor(
            name = "home_sync",
            kind = "background-sync",
            permission = "Home flag + plan feature allow hone par hi sync",
            licenceKey = "home",
            executesVia = "Home flow -> HomeSyncClient.sync (maujooda caller)",
        ),
        ToolDescriptor(
            name = "clipboard_capture",
            kind = "auth-capture",
            permission = "CLIPBOARD PROMPT (user haan bole tabhi) + save ke baad auto-clear — silent read kabhi nahi",
            licenceKey = null,
            executesVia = "ConnectServices flow -> ClipboardCapture.captureFromClipboard (prompt+auto-clear rule barkaraar)",
        ),
        ToolDescriptor(
            name = "builder_oauth",
            kind = "auth",
            permission = "User khud login karega (OAuth/PKCE) — Flora kabhi password nahi leti",
            licenceKey = "builder",
            executesVia = "ConnectServices flow -> BuilderOAuth (B3)",
        ),
        ToolDescriptor(
            name = "builder_tokens",
            kind = "auth-storage",
            permission = "Koi naya permission nahi — KeyStore encrypted storage hi",
            licenceKey = null,
            executesVia = "BuilderTokens / FloraPreferences builder slots (B3 storage, kabhi log nahi)",
        ),
        ToolDescriptor(
            name = "device_info",
            kind = "device-local",
            permission = "Koi permission nahi — system info read hi (time/battery/device/storage/RAM/network/Wi-Fi state)",
            licenceKey = null,
            executesVia = "FloraApp chat branch -> DeviceActions (NoAccess Batch A)",
        ),
        ToolDescriptor(
            name = "device_intents",
            kind = "device-local",
            permission = "Intent se system app khulta hai — user khud confirm karta hai (alarm/timer/dial/settings)",
            licenceKey = null,
            executesVia = "FloraApp chat branch -> DeviceActions (NoAccess Batch A)",
        ),
        ToolDescriptor(
            name = "device_torch",
            kind = "device-local",
            permission = "CAMERA runtime grant — wizard/PermissionsScreen se (na mile to fail-closed)",
            licenceKey = null,
            executesVia = "FloraApp chat branch -> DeviceActions (NoAccess Batch B)",
        ),
    )

    fun byName(name: String): ToolDescriptor? = all().firstOrNull { it.name == name }
}
