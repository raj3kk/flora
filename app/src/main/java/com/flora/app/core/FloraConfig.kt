package com.flora.app.core

/**
 * Flora — clean-room rebuild, behaviour-based only.
 * No creator code, UI text, assets, character or trademark is used here.
 * All values below are Flora's own decisions (see FLORA_DECISIONS.md).
 */
object FloraConfig {
    const val APP_NAME = "Flora"
    const val SUPPORT_EMAIL = "flipify.com@gmail.com"

    // Free plan: 10 minutes talk per day, all tools currently
    // built in the app. NOTE: koi "15 tools" ki list kabhi
    // define nahi hui thi — isliye is count par koi gating
    // nahi hai (gating lagana free users se chalte hue tools
    // chheenna hota, bina user order ke nahi kiya gaya).
    const val FREE_TALK_SECONDS_PER_DAY = 600

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
    const val PUSH_TOKEN_FUNCTION_PATH = "/functions/v1/push-token-sync"
    const val HOME_SYNC_FUNCTION_PATH = "/functions/v1/home-sync"

    // Feature flags + heartbeat (Phase 8, panel phase 2 server):
    // flags = POST, anon key, body {user_id?}; heartbeat = POST,
    // user JWT, body {user_id} (JWT user se match hona chahiye).
    // Dono fail-safe hain: flags fail-open (sab ON), heartbeat
    // silent-skip — server na bhi ho to app kabhi nahi tootegi.
    const val FEATURE_FLAGS_PATH = "/functions/v1/feature-flags"
    const val HEARTBEAT_PATH = "/functions/v1/heartbeat"

    // Public settings (Wave B, D1): website pay page + app Pro
    // screen ka EK UPI source — admin panel se set hota hai
    // (admin-settings function), no-auth read.
    const val PUBLIC_SETTINGS_PATH = "/functions/v1/public-settings"

    // Flora website (stable domain, Supabase Auth Site URL bhi
    // yahi hai). In-app browser isi se pay page kholta hai.
    const val WEBSITE_URL = "https://flora-henna-alpha.vercel.app"

    // Kokoro neural voice model (Phase 8, user-final: hf_alpha).
    // PUBLIC HuggingFace URLs (hexgrad/Kokoro-82M v1.0) — APK me
    // bundle nahi, runtime download (KokoroModelManager). SHA
    // 2026-10-29 ke download se verify kiye gaye hain; upstream
    // file badle to SHA mismatch par download reject hoga
    // (fail-closed), kabhi galat model ready nahi mana jayega.
    const val KOKORO_MODEL_URL =
        "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/kokoro-v1.0.onnx"
    const val KOKORO_VOICES_URL =
        "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/voices-v1.0.bin"
    const val KOKORO_MODEL_SHA256 =
        "7d5df8ecf7d4b1878015a32686053fed0eebe2bc377234608764cc0ef3636a6c5"
    const val KOKORO_VOICES_SHA256 =
        "bca610b8308e8d99f32e6fe4197e7ec01679264efed0cac9140fe9c29f1fbf7d"
    const val KOKORO_MODEL_SIZE = 325_532_387L
    const val KOKORO_VOICES_SIZE = 28_214_398L
    const val KOKORO_VOICE_ID = "hf_alpha"

    // Models manifest (v2.21, CEO plan M1): Flora ke apne
    // models ka server — Supabase Storage public bucket
    // flora-models (bucket banne tak ye URL 404 dega, tab
    // app bundled list hi dikhayegi — koi error nahi).
    // Bucket + manifest ka source of truth repo me
    // web/models/manifest.json hai.
    const val MODELS_MANIFEST_URL =
        "https://keomyweghrmhqglaxmyv.supabase.co/storage/v1/object/public/flora-models/manifest.json"

    // In-app update (Phase 2 Item 3): version check file repo ke
    // web/updates/version.json se aati hai — Vercel use static
    // serve karta hai (models/manifest.json wali pattern). APK
    // khud Supabase public bucket se aayega jab channel lagega;
    // tab tak version.json me apkUrl khali hai aur app website
    // ke download page par bhejega. GitHub releases private repo
    // ki wajah se no-auth channel nahi ban sakte; Vercel free
    // 50 MB limit ki wajah se APK host nahi kar sakta.
    const val UPDATE_VERSION_URL =
        "https://flora-henna-alpha.vercel.app/updates/version.json"
    const val UPDATE_PAGE_URL = WEBSITE_URL + "/download.html"

    // Google link sign-in (v2.22, Phase 5, G14): Web OAuth
    // client ID dashboard (Firebase/Supabase) par banana
    // baaki hai — tab tak khali hai aur Settings button
    // setup guidance dikhata hai, sign-in shuru nahi karta.
    const val GOOGLE_WEB_CLIENT_ID = ""
    // Wake phrase DECIDED hai: "Hey Flora" (user decision 2026-10-28).
    // v2.4 se v0 model hai: MFCC mean template (user ke 6 clips se
    // prototype, phone par 3-sample enrolment). Neural model abhi
    // baaki hai. Default OFF hai; chhupke recording kabhi nahi.
    const val WAKE_WORD_DECISION_PENDING = false
}
