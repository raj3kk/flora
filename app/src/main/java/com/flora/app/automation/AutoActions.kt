package com.flora.app.automation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Confirmed automation (user order 2026-10-28): user says it once,
 * the app asks ONE confirmation, then executes fully — no dialer
 * draft, no manual Send step. Nothing here runs silently: every
 * call/SMS goes through an in-app confirmation dialog first, and
 * Android's own CALL_PHONE / SEND_SMS permissions must be granted
 * by the user once in the system dialog (that grant cannot be
 * bypassed by any app). If permission is missing, callers must
 * fall back to the manual dialer/SMS draft and say so honestly.
 */
object AutoActions {

    fun hasCallPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** Places a real call directly. Returns false if permission is missing. */
    fun placeCall(context: Context, number: String): Boolean {
        if (!hasCallPermission(context)) return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_CALL, Uri.parse("tel:${number.trim()}")).apply {
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
