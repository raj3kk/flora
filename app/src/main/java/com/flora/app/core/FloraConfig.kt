package com.flora.app.core

/**
 * Flora — clean-room rebuild, behaviour-based only.
 * No creator code, UI text, assets, character or trademark is used here.
 * All values below are Flora's own decisions (see FLORA_DECISIONS.md).
 */
object FloraConfig {
    const val APP_NAME = "Flora"
    const val SUPPORT_EMAIL = "flipify.com@gmail.com"

    // Free plan: 10 minutes talk per day, ~15 tools.
    const val FREE_TALK_SECONDS_PER_DAY = 600
    const val FREE_TOOLS_COUNT = 15

    // Pro plan: Rs 199 per month (19900 paise), unlimited talk, all tools.
    const val PRO_PRICE_PAISA_PER_MONTH = 19900
    const val PRO_PRICE_TEXT = "₹199/month"

    // Data retention for sync/memory: 30 days. Licence/payment records
    // are kept separately for legal accounting and are NOT auto-expired.
    const val DATA_RETENTION_DAYS = 30

    // Supabase project for Flora (public project URL only — never a key).
    // API keys are NEVER hardcoded: the user enters his own publishable
    // anon key once in Settings, stored in DataStore on-device only.
    const val SUPABASE_URL = "https://keomyweghrmhqglaxmyv.supabase.co"
    const val LICENCE_FUNCTION_PATH = "/functions/v1/licence-verify"
    const val USAGE_FUNCTION_PATH = "/functions/v1/usage-count"

    // Wake phrases are NOT decided yet (open user decision): the app
    // ships without a wake-word model until the user confirms phrases
    // and gives consent-only recordings. Nothing is recorded silently.
    const val WAKE_WORD_DECISION_PENDING = true
}
