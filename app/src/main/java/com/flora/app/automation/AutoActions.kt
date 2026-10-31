package com.flora.app.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Confirmed automation (user order 2026-10-28), D-11 ke baad ka
 * roop (CEO 2026-10-31): Flora default dialer NAHI banegi.
 * Call ke liye SIRF system dialer intent khulta hai — user khud
 * system dialer me Call dabata hai; Flora kabhi seedha call nahi
 * lagati (ACTION_CALL hata diya gaya hai). SMS path is change se
 * bahar hai (D-11 dialer/calling ka faisla hai; SMS alag surface
 * hai — coordinator ko flag kiya gaya hai).
 * Nothing here runs silently: every call/SMS goes through an
 * in-app confirmation dialog first. If permission is missing,
 * callers must fall back to the manual dialer/SMS draft and say
 * so honestly.
 */
object AutoActions {

    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** D-11: sirf system dialer kholta hai (number bhara hua) — seedha call kabhi nahi. */
    fun placeCall(context: Context, number: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Sends a real SMS directly. Returns false if permission is missing or send fails. */
    fun sendSms(context: Context, number: String, text: String): Boolean {
        if (!hasSmsPermission(context)) return false
        return try {
            SmsManager.getDefault().sendTextMessage(number.trim(), null, text, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
