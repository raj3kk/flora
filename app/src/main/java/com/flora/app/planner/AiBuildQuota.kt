package com.flora.app.planner

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.MemoryCrypto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.aiBuildQuotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_ai_build_quota")

/**
 * B5-06 quota counting, DEVICE-SIDE (CEO faisla c, binding).
 *
 * - 1 AI build = 1 code-generation call. Code-generation
 *   execution par hi +1 hota hai (CODE_GEN_TOOLS — GITHUB_PUSH_FILE,
 *   VERCEL_DEPLOY, PROJECT_SAVE_FILE). Plan-generation calls
 *   (PlannerBrain.Generation.calls — generate + replan) builds me
 *   KABHI nahi ginegi: wo sirf planCalls counter + Auto Log me
 *   rehte hain (createPlan ki log line me saaf likha hai).
 * - Naya counter talkSecondsUsed (600s/day free-talk cap) ya
 *   promptCount (G2 metric) se derive NAHI hota — sirf code-gen
 *   execution par badhta hai. Isliye talk-cap aur build-quota ke
 *   messages alag-alag hain (dono ka text alag hai, confuse nahi).
 * - Day = device local day (device timezone, local midnight
 *   reset — stored day badla to count 0).
 * - Free: 3 projects + 5 builds/day. Gate = imaandaar message +
 *   upgrade hint (LicenceGate honest-message pattern par);
 *   silent block NAHI (message hamesha batata hai kyun ruka),
 *   aur hard enforcement bhi NAHI (server-side asli limiter
 *   brain-proxy 600s cap hi rahega, plan doc B5 CEO (c)).
 * - Pro: unlimited (server verify se plan=pro + valid).
 *   Verify na ho paye (offline / no-link) to UNKNOWN -> gate
 *   allow karta hai (purana vyavhaar, fail-open) — offline me
 *   paying user ko galat gate nahi lagega.
 */
object AiBuildQuotaLogic {
    val CODE_GEN_TOOLS: Set<PlanTool> = setOf(
        PlanTool.GITHUB_PUSH_FILE,
        PlanTool.VERCEL_DEPLOY,
        PlanTool.PROJECT_SAVE_FILE,
    )

    const val FREE_PROJECT_LIMIT = 3
    const val FREE_BUILDS_PER_DAY = 5

    fun isNewDay(storedDay: String, today: String): Boolean = storedDay != today

    fun buildsRemaining(buildsToday: Int, isPro: Boolean): Int {
        if (isPro) return Int.MAX_VALUE
        return (FREE_BUILDS_PER_DAY - buildsToday).coerceAtLeast(0)
    }

    fun buildGateMessage(buildsToday: Int): String? {
        if (buildsToday < FREE_BUILDS_PER_DAY) return null
        return "Aaj ke free AI builds (5/day) poore ho gaye hain. " +
            "Kal phir milega, ya Pro lijiye (unlimited builds). " +
            "Ye build-quota hai — free talk time (10 minute/day) alag cap hai."
    }

    fun projectGateMessage(currentCount: Int): String? {
        if (currentCount < FREE_PROJECT_LIMIT) return null
        return "Free me 3 projects tak bana sakte ho — ye limit poori ho gayi hai. " +
            "Pro lijiye (unlimited projects), ya purana project istemal karo."
    }
}

class AiBuildQuota(
    private val context: Context,
    private val prefs: FloraPreferences,
    private val licenceClient: LicenceClient,
    private val androidId: String,
) {

    enum class ProState { PRO, FREE, UNKNOWN }

    private val dayKey = stringPreferencesKey("quota_day")
    // B5 FIX 4: ye counters Int hain — stringPreferencesKey galat
    // tha (CI compile error: String? vs Int mismatch, 4 errors).
    private val buildsKey = intPreferencesKey("quota_builds")
    private val planCallsKey = intPreferencesKey("quota_plan_calls")

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    suspend fun buildsToday(): Int {
        return try {
            val data = context.aiBuildQuotaDataStore.data.first()
            val day = data[dayKey] ?: ""
            if (AiBuildQuotaLogic.isNewDay(day, today())) 0 else (data[buildsKey] ?: 0)
        } catch (e: Exception) {
            0
        }
    }

    /** Sirf code-gen execution par call karo — plan-gen par kabhi nahi. */
    suspend fun recordCodeGeneration() {
        try {
            val day = today()
            context.aiBuildQuotaDataStore.edit { store ->
                val current = if (store[dayKey] != day) 0 else (store[buildsKey] ?: 0)
                store[dayKey] = day
                store[buildsKey] = current + 1
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Plan-generation calls ka count — sirf hisaab ke liye.
     * Ye builds counter ko kabhi nahi chhuta (CEO faisla c).
     */
    suspend fun recordPlanGeneration(calls: Int) {
        if (calls <= 0) return
        try {
            val day = today()
            context.aiBuildQuotaDataStore.edit { store ->
                val current = if (store[dayKey] != day) 0 else (store[planCallsKey] ?: 0)
                store[dayKey] = day
                store[planCallsKey] = current + calls
            }
        } catch (e: Exception) {
        }
    }

    suspend fun planCallsToday(): Int {
        return try {
            val data = context.aiBuildQuotaDataStore.data.first()
            val day = data[dayKey] ?: ""
            if (AiBuildQuotaLogic.isNewDay(day, today())) 0 else (data[planCallsKey] ?: 0)
        } catch (e: Exception) {
            0
        }
    }

    suspend fun proState(): ProState {
        return try {
            val uid = prefs.effectiveUserId()
            val dev = prefs.deviceId(androidId)
            if (uid.isEmpty() || dev.isEmpty()) return ProState.UNKNOWN
            val st = licenceClient.verify(uid, dev)
            if (!st.serverChecked) return ProState.UNKNOWN
            if (st.plan == "pro" && st.valid) ProState.PRO else ProState.FREE
        } catch (e: Exception) {
            ProState.UNKNOWN
        }
    }

    /** Null = allowed; message = gate (imaandaar + upgrade hint). */
    suspend fun buildGateMessage(): String? {
        return when (proState()) {
            ProState.PRO -> null
            ProState.UNKNOWN -> null
            ProState.FREE -> AiBuildQuotaLogic.buildGateMessage(buildsToday())
        }
    }

    /** Null = allowed; message = gate (imaandaar + upgrade hint). */
    suspend fun projectGateMessage(currentCount: Int): String? {
        return when (proState()) {
            ProState.PRO -> null
            ProState.UNKNOWN -> null
            ProState.FREE -> AiBuildQuotaLogic.projectGateMessage(currentCount)
        }
    }

    suspend fun statusText(): String {
        val builds = buildsToday()
        return when (proState()) {
            ProState.PRO -> "AI builds: Pro (unlimited), aaj $builds hue."
            ProState.UNKNOWN -> "AI builds aaj: $builds/5 (licence status unknown — gate fail-open)."
            ProState.FREE -> "AI builds aaj: $builds/5 (Free)."
        }
    }
}
