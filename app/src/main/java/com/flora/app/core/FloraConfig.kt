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

    // Supabase project for Flora.
    // Publishable (anon) key: ye SECRET NAHI hai — Supabase ka design
    // hi ye hai ki ye client app me hoti hai; data ki suraksha RLS
    // rules se hoti hai. Isliye user order (2026-10-28) par isko
    // default banaya gaya hai taaki user ko Settings me key daalni
    // na pade. User ki apni Settings key ho to wahi pehle chalegi.
    // Service_role key kabhi yahan nahi aayegi — wo sirf server par
    // (Supabase Edge Function secrets me) rehti hai.
    const val SUPABASE_URL = "https://keomyweghrmhqglaxmyv.supabase.co"
    const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_qMjX-cw11dB_Y9nT8IlVzw_nPup-Yxe"
    const val LICENCE_FUNCTION_PATH = "/functions/v1/licence-verify"
    const val USAGE_FUNCTION_PATH = "/functions/v1/usage-count"

    // Wake phrase DECIDED hai: "Hey Flora" (user decision 2026-10-28).
    // Model/training abhi baaki hai (recordings 6/50) — isliye app
    // me abhi koi wake-word model nahi hai. Nothing is recorded silently.
    const val WAKE_WORD_DECISION_PENDING = false
}
