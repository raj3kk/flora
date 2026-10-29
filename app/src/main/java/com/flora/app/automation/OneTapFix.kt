package com.flora.app.automation

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * One-Tap Fix (Lane A — A1, R-A1-6/R-A1-10).
 *
 * DOCTOR CARVE-OUT — YE WAHI EK AUTHORIZED SECURE-SETTINGS PATH HAI:
 * Doctor/planner rule kehta hai secure settings sirf One-Tap Fix path
 * chhedega. Is file ke alawa koi code enabled_accessibility_services
 * ya Settings.Secure.putString ko nahi chhuta (re-audit grep proof).
 * Ye path SIRF Flora ki apni accessibility service restore karta hai —
 * battery-optimisation/auto-start restore isme NAHI aata (R-A1-10).
 *
 * Component string manifest service declaration se verified hai:
 * com.flora.app/com.flora.app.automation.FloraAutomationService.
 *
 * Priority (R-A1-6): (1) app ke paas WRITE_SECURE_SETTINGS ho
 * (pehle adb se grant mila ho) to seedha append-write; (2) Shizuku
 * se grant lekar phir write (Shizuku-write PEHLE, adb-guide FALLBACK);
 * (3) dono na hon to imaandaar fail — success ka jhootha message
 * kabhi nahi. Har result ko re-read se verify kiya jata hai.
 */
object OneTapFix {

    const val COMPONENT =
        "com.flora.app/com.flora.app.automation.FloraAutomationService"
    const val SECURE_KEY = "enabled_accessibility_services"
    const val ADB_COMMAND =
        "adb shell pm grant com.flora.app android.permission.WRITE_SECURE_SETTINGS"

    sealed class Result {
        /**
         * Service pehle se ON hai — kuch badla nahi. A1 follow-up
         * F1: ye SIRF tab jab component list me ho AUR
         * accessibility_enabled == "1" ho. List me entry par
         * flag "0" ho to ye kabhi nahi milta (warna agla Fix
         * tap wahi galat AlreadyOn dekar stuck loop banata).
         */
        object AlreadyOn : Result()

        /**
         * Restore hua. enabledFlagChanged = true matlab
         * accessibility_enabled "0" se "1" bhi karna pada (sirf tab
         * jab wo "1" nahi tha — koi doosri setting overwrite nahi).
         * Restored(false) sirf tab jab flag pehle se "1" tha.
         */
        data class Restored(val enabledFlagChanged: Boolean) : Result()

        /**
         * AADHA restore (F1): list-write ho gaya par flag-write
         * fail ya flag-verify fail — kabhi Restored nahi. UI isko
         * "pehle se ON hai — kuch badla nahi" jaisa kabhi nahi
         * dikhata; automation abhi poori tarah ON nahi hai.
         */
        data class PartiallyRestored(val reason: String) : Result()

        /** Koi grant nahi mila — adb guide / Shizuku-start dikhao. */
        data class GrantMissing(val shizukuReady: Boolean) : Result()

        /** Grant mila par write/verify fail — jhootha success nahi. */
        data class Failed(val reason: String) : Result()
    }

    /**
     * Append-only core (PURE — overwrite-safety ka test point):
     * maujooda colon-separated list parse karo, Flora entry pehle
     * se ho to null (duplicate append nahi), khaali list par sirf
     * Flora entry, warna maujooda entries + Flora entry. Doosri
     * services ki entries hamesha barkaraar rehti hain.
     */
    fun appendService(existing: String?): String? {
        val parts = (existing ?: "")
            .split(":")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (COMPONENT in parts) return null
        return (parts + COMPONENT).joinToString(":")
    }

    fun hasWritePermission(context: Context): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_SECURE_SETTINGS,
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun readEnabledList(context: Context): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, SECURE_KEY)
        } catch (e: Exception) {
            null
        }
    }

    /** List me Flora entry hai ya nahi — sirf membership, ON ka saboot nahi. */
    private fun listContains(list: String?): Boolean {
        if (list == null) return false
        return COMPONENT in list.split(":").map { it.trim() }
    }

    /** accessibility_enabled flag — null = padha nahi ja saka, "1" nahi mana jayega. */
    private fun readEnabledFlag(context: Context): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, "accessibility_enabled")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Shizuku-first write-permission gate (R-A1-6, dono raaston se
     * ek hi jagah): permission ho to null (aage badho), warna
     * Shizuku se grant ki koshish, phir bhi na mile to GrantMissing.
     */
    private fun ensureWritePermission(context: Context): Result? {
        if (hasWritePermission(context)) return null
        // Shizuku-write PEHLE: Shizuku se grant lekar dobara check.
        if (ShizukuBridge.isReady() && ShizukuBridge.hasPermission()) {
            val granted = try {
                ShizukuBridge.grantSecureSettingsPermission(
                    context.packageName,
                    context,
                )
            } catch (e: Exception) {
                false
            }
            if (!granted || !hasWritePermission(context)) {
                return Result.GrantMissing(shizukuReady = true)
            }
            return null
        }
        return Result.GrantMissing(shizukuReady = ShizukuBridge.isReady())
    }

    /**
     * Flag-repair (F1): list me entry hai par flag "1" nahi.
     * Grant lekar flag "1" likho aur re-read se verify karo.
     * Sachha result hi: Restored(true) sirf verified "1" par,
     * warna PartiallyRestored — kabhi AlreadyOn nahi.
     */
    private fun repairFlag(context: Context): Result {
        val missing = ensureWritePermission(context)
        if (missing != null) return missing
        try {
            Settings.Secure.putString(
                context.contentResolver,
                "accessibility_enabled",
                "1",
            )
        } catch (e: Exception) {
            return Result.PartiallyRestored("accessibility flag likh nahi paya")
        }
        if (readEnabledFlag(context) != "1") {
            return Result.PartiallyRestored("flag likhne ke baad verify nahi hua")
        }
        return Result.Restored(enabledFlagChanged = true)
    }

    fun restore(context: Context): Result {
        return try {
            val before = readEnabledList(context)
            if (listContains(before)) {
                // F1 (a): sirf list-membership AlreadyOn nahi hai.
                // Flag "1" ho to hi AlreadyOn; warna flag repair
                // ki koshish aur uska sachha result.
                if (readEnabledFlag(context) == "1") return Result.AlreadyOn
                return repairFlag(context)
            }
            val missing = ensureWritePermission(context)
            if (missing != null) return missing
            val newList = appendService(readEnabledList(context))
                ?: run {
                    // Beech me kisi ne list me entry jod di (race):
                    // andha AlreadyOn nahi — wahi flag check.
                    if (readEnabledFlag(context) == "1") return Result.AlreadyOn
                    return repairFlag(context)
                }
            try {
                Settings.Secure.putString(context.contentResolver, SECURE_KEY, newList)
            } catch (e: Exception) {
                return Result.Failed("secure settings likh nahi payi")
            }
            // Verify: dobara padh kar hi success mano.
            val after = readEnabledList(context)
            if (!listContains(after)) {
                return Result.Failed("likhne ke baad verify nahi hua")
            }
            // F1 (b): flag-write fail ab nigla nahi jata.
            // Flag pehle se "1" tha to Restored(false); likhna
            // pada aur verified "1" hua to Restored(true);
            // likhna fail ya verify fail to PartiallyRestored,
            // kabhi Restored nahi.
            if (readEnabledFlag(context) == "1") {
                return Result.Restored(enabledFlagChanged = false)
            }
            try {
                Settings.Secure.putString(
                    context.contentResolver,
                    "accessibility_enabled",
                    "1",
                )
            } catch (e: Exception) {
                return Result.PartiallyRestored("accessibility flag likh nahi paya")
            }
            if (readEnabledFlag(context) != "1") {
                return Result.PartiallyRestored("flag likhne ke baad verify nahi hua")
            }
            Result.Restored(enabledFlagChanged = true)
        } catch (e: Exception) {
            Result.Failed("fix chalate waqt dikkat aayi")
        }
    }
}
