package com.flora.app.assistant

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.flora.app.data.FloraPreferences

/**
 * Battery-aware reply ka ekmatra reader (Lane A — A1).
 *
 * - Sirf isi file me BatteryManager / sticky ACTION_BATTERY_CHANGED
 *   read hota hai (re-audit grep proof ke liye — doosri file me
 *   battery read mat jodo).
 * - Sticky read toggle se azaad hai: EventAnnounceReceiver ka toggle
 *   OFF ho to bhi reply-time battery state milta hai (A1-P1-8).
 * - Ye reader koi state save nahi karta — no-repeat key
 *   FloraPreferences (batteryLastWarnedKey) me rehti hai.
 * - Thresholds coordinator ruling R-A1-1 se: LOW = discharging +
 *   battery <= 20%; CRITICAL = discharging + battery <= 10%;
 *   FULL = 100% (sticky read se). Charging ON par koi
 *   low/critical nag nahi.
 */
data class BatteryState(val percent: Int, val charging: Boolean)

/**
 * Shared NON-COMPOSABLE battery-note source (Lane A — A1 follow-up F3).
 *
 * Pehle ye logic sirf FloraApp ke composable-local helper me tha, isliye
 * BackgroundMicService (service context, koi composable nahi) use nahi
 * kar sakta tha. Ab ek hi jagah hai: FloraApp ke 4 chat call sites
 * isi logic ko wrapper se use karte hain, aur background-mic path
 * seedha isi ko call karta hai — duplicate logic nahi bana.
 * No-repeat key wahi ek hai (FloraPreferences battery_last_warned):
 * ek state par chat ne warn kar diya to background-mic chup rahega,
 * aur ulta bhi. Koi exception aaye to khali note — jawab kabhi
 * battery read par nahi rukega. Note sirf transient hai: ye string
 * kabhi memCtx/history/stored turn me nahi jati (BrainClient.ask ka
 * transientNote param dekho).
 */
object BatteryTransientNote {
    suspend fun note(context: Context, prefs: FloraPreferences): String {
        return try {
            val state = BatteryStateReader.read(context)
            val key = BatteryStateReader.keyFor(state)
            val last = try { prefs.batteryLastWarnedKey() } catch (e: Exception) { "" }
            val isNew = key != last
            if (isNew) {
                try { prefs.setBatteryLastWarnedKey(key) } catch (e: Exception) { }
            }
            BatteryStateReader.instructionFor(key, state.percent, isNew) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

object BatteryStateReader {

    /** Sticky broadcast se % + charging — koi receiver register nahi hota. */
    fun read(context: Context): BatteryState {
        return try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ) ?: return BatteryState(-1, false)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            BatteryState(percent, charging)
        } catch (e: Exception) {
            // Battery na padhi ja sake to fail-open: normal jawab, koi nag nahi.
            BatteryState(-1, false)
        }
    }

    /**
     * No-repeat state key (R-A1-4): aakhri warned level/state.
     * "normal"/"full" par koi warning nahi — key sirf isliye badalti
     * hai taaki wapas low par aane par dobara ek baar warn ho sake.
     */
    fun keyFor(state: BatteryState): String {
        if (state.charging) return "charging"
        if (state.percent >= 100) return "full"
        if (state.percent in 0..10) return "critical"
        if (state.percent in 0..20) return "low"
        return "normal"
    }

    /**
     * Transient instruction (R-A1-3/R-A1-5): sirf tab jab state key
     * BADLI ho (isNew) aur key low/critical/charging ho. Wapas
     * aane wali (same-key) calls par null — koi repeat nag nahi.
     * Ye string sirf us ek brain.ask() call ke prompt me jati hai;
     * memCtx/history/stored turn me kabhi nahi (BrainClient.ask ka
     * transientNote param dekho).
     */
    fun instructionFor(key: String, percent: Int, isNew: Boolean): String? {
        if (!isNew) return null
        return when (key) {
            "critical" -> "Battery bahut kam hai ($percent%) — jawab BAHUT " +
                "chhota rakho, sirf zaroori baat, aur ek baar charger " +
                "lagane ko kaho."
            "low" -> "Battery low hai ($percent%) — is jawab ko CHHOTA " +
                "rakho, kaam ki baat pehle bolo, aur ek baar ye sujhav " +
                "bhi do ki battery bachane ke liye screen brightness kam " +
                "kare ya background apps band kare."
            "charging" -> "Charger lag gaya hai — is jawab me ek baar " +
                "khushi se ye bata do, phir normal jawab do."
            else -> null
        }
    }
}
