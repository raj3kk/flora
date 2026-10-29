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
        /** Service pehle se ON hai — kuch badla nahi. */
        object AlreadyOn : Result()

        /**
         * Restore hua. enabledFlagChanged = true matlab
         * accessibility_enabled "0" se "1" bhi karna pada (sirf tab
         * jab wo "1" nahi tha — koi doosri setting overwrite nahi).
         */
        data class Restored(val enabledFlagChanged: Boolean) : Result()

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

    fun restore(context: Context): Result {
        return try {
            val before = readEnabledList(context)
            if (appendService(before) == null) return Result.AlreadyOn
            if (!hasWritePermission(context)) {
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
                } else {
                    return Result.GrantMissing(
                        shizukuReady = ShizukuBridge.isReady(),
                    )
                }
            }
            val newList = appendService(readEnabledList(context))
                ?: return Result.AlreadyOn
            try {
                Settings.Secure.putString(context.contentResolver, SECURE_KEY, newList)
            } catch (e: Exception) {
                return Result.Failed("secure settings likh nahi payi")
            }
            // Verify: dobara padh kar hi success mano.
            val after = readEnabledList(context)
            if (after == null || COMPONENT !in after.split(":").map { it.trim() }) {
                return Result.Failed("likhne ke baad verify nahi hua")
            }
            var flagChanged = false
            try {
                val enabled = Settings.Secure.getString(
                    context.contentResolver,
                    "accessibility_enabled",
                )
                if (enabled != "1") {
                    Settings.Secure.putString(
                        context.contentResolver,
                        "accessibility_enabled",
                        "1",
                    )
                    flagChanged = true
                }
            } catch (e: Exception) {
                // List jud chuki hai — flag fail par bhi restore mana jayega,
                // par Auto Log me ye baat darj hogi (caller log karta hai).
            }
            Result.Restored(flagChanged)
        } catch (e: Exception) {
            Result.Failed("fix chalate waqt dikkat aayi")
        }
    }
}
