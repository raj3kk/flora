package com.flora.app.core

import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Feature flags client (Phase 8, panel phase 2 ke server se juda).
 *
 * Contract (FINAL, panel phase 2 builder ke saath tay):
 *  POST {SUPABASE_URL}/functions/v1/feature-flags
 *  Headers: apikey <anon>, Authorization Bearer <anon>
 *  Body: {} ya {"user_id": "<linked uid>"} (linked ho to hi)
 *  Response: {"flags": {"talk": bool, "calls": bool, "sms": bool,
 *    "whatsapp": bool, "browser": bool, "macros": bool,
 *    "image_gen": bool, "screen_vision": bool, "camera": bool,
 *    "overlay_mic": bool, "wakeword": bool, "home": bool,
 *    "routines": bool, "form_fill": bool}}
 *  Server: global rows base + per-user row per-key override.
 *
 * FAIL-OPEN (sakht niyam): flags na mile, key response me na ho,
 * ya network fail ho — har case me us feature ko ON mana jayega.
 * App panel ke bina, ya server function deploy hone se pehle,
 * kabhi nahi tootegi. Aakhri mili hui flags phone par cache
 * hoti hain (offline me wahi chalti hain).
 *
 * Settings screen kabhi flag se band nahi hoti — recovery path
 * hamesha khula hai (admin flag galat bhi kar de to user
 * Settings se sab wapas chalu kar sakta hai — panel se nahi,
 * app ke andar se; panel toggles sirf features ko gate karte
 * hain, Settings UI ko nahi).
 */
object FeatureFlags {

    val ALL_KEYS = listOf(
        "talk", "calls", "sms", "whatsapp", "browser", "macros",
        "image_gen", "screen_vision", "camera", "overlay_mic",
        "wakeword", "home", "routines", "form_fill"
    )

    @Volatile
    private var map: Map<String, Boolean>? = null

    /** Koi flag na pata ho to true (fail-open). */
    fun isEnabled(key: String): Boolean = map?.get(key) ?: true

    suspend fun refresh(prefs: FloraPreferences) {
        withContext(Dispatchers.IO) {
            try {
                if (map == null) {
                    val cached = try { prefs.featureFlagsJson() } catch (e: Exception) { "" }
                    if (!cached.isNullOrEmpty()) {
                        try {
                            val obj = JSONObject(cached)
                            val m = mutableMapOf<String, Boolean>()
                            for (k in ALL_KEYS) {
                                if (obj.has(k)) m[k] = obj.optBoolean(k, true)
                            }
                            if (m.isNotEmpty()) map = m
                        } catch (e: Exception) {
                        }
                    }
                }
                val anon = try { prefs.supabaseAnonKey() } catch (e: Exception) { "" }
                if (anon.isEmpty()) return@withContext
                val uid = try { prefs.linkedUserId() } catch (e: Exception) { "" }
                val body = JSONObject()
                if (uid.isNotEmpty()) body.put("user_id", uid)
                val url = URL(FloraConfig.SUPABASE_URL + FloraConfig.FEATURE_FLAGS_PATH)
                val conn = PinnedTrust.openConnection(url) as HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 15_000
                    conn.readTimeout = 15_000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("apikey", anon)
                    conn.setRequestProperty("Authorization", "Bearer $anon")
                    conn.outputStream.use {
                        it.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                    }
                    if (conn.responseCode !in 200..299) return@withContext
                    val text = BufferedReader(
                        InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)
                    ).readText()
                    val flags = JSONObject(text).optJSONObject("flags")
                        ?: return@withContext
                    val m = mutableMapOf<String, Boolean>()
                    for (k in ALL_KEYS) {
                        if (flags.has(k)) m[k] = flags.optBoolean(k, true)
                    }
                    if (m.isNotEmpty()) {
                        map = m
                        try { prefs.setFeatureFlagsJson(flags.toString()) } catch (e: Exception) { }
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                // Fail-open: map wahi rahega jo tha (cache ya null =
                // sab ON). App-start kabhi nahi rukega.
            }
        }
    }
}
