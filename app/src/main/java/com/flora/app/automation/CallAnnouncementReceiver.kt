package com.flora.app.automation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.calls.IncomingCallActivity
import com.flora.app.calls.IncomingCallHolder
import com.flora.app.data.AutoLogStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.callAnnounceStore by preferencesDataStore(name = "flora_prefs")

/**
 * Call announcement (v1.2 name announce + Batch 4 Maya-parity
 * self-activation + missed-call summary).
 *
 * RINGING: caller ka NAAM bol kar batata hai — "Boss, {name}
 * calling you"; naam na mile to "Boss, unknown number calling
 * you" — digits kabhi nahi bole jayenge (paas khade log number
 * sun lete hain) aur UI par bhi digits kabhi display nahi hote.
 * Sirf tab jab user ne Settings me toggle ON kiya ho (default
 * OFF) aur Phone State + Call Log dono permissions di hon
 * (Android 9+ par number ke liye READ_CALL_LOG zaroori hai —
 * iske bina number null milta hai aur announce chupchaap nahi
 * hota). Ek hi ringing event dobara broadcast ho to 30 second
 * me dobara nahi bolega.
 *
 * SELF-ACTIVATION (Maya-parity): ringing par Flora khud saamne
 * aati hai, user ko app kholna ya tap karna nahi padta —
 * HIGH-importance CATEGORY_CALL notification: FSI grant ho to
 * full-screen intent se IncomingCallActivity lock screen par
 * khulti hai; FSI na ho to heads-up notification + overlay
 * koshish (AnnounceService overlay view). POST_NOTIFICATIONS
 * na ho to notification nahi — sirf awaaz + overlay koshish.
 * Overlay best-effort hai: Android 12+ background se FGS start
 * block kar sakta hai, tab heads-up + awaaz hi rahegi.
 * FSI grant: Android 14+ par user Settings se deta hai
 * (Settings me "Settings kholo" button); 14 se neeche grant ki
 * zaroorat nahi.
 *
 * MISSED: RINGING → IDLE bina OFFHOOK ke = missed call. Sirf
 * tab bolega jab MISSED_ANNOUNCE toggle ON ho (default OFF):
 * "Boss, {name} ki missed call hai" / unknown variant.
 * Uthayi hui call (OFFHOOK hua) par missed kabhi nahi bolega.
 * State sirf isi process ke companion vars me hai — process
 * mar gaya to missed chhoot sakta hai (best-effort, chhupaya
 * nahi gaya).
 *
 * Auto Log me sirf masked entry (naam/unknown + aakhri 4
 * digits) — poora number kabhi log nahi hota, koi call data
 * server par nahi jaata. Bol kar receive/reject isme nahi hai
 * (dialer role alag raasta hai); auto-answer kabhi wapas nahi
 * aayega. Honest limit: app force-stop ho to announce nahi hoga;
 * Vivo kill par receiver fire hi nahi hoga — Batch 3 Vivo
 * guidance (auto-start + battery unrestricted) hi bachaav hai.
 */
class CallAnnouncementReceiver : BroadcastReceiver() {

    companion object {
        @Volatile private var lastNumber: String? = null
        @Volatile private var lastTimeMs: Long = 0L
        @Volatile private var ringNumber: String? = null
        @Volatile private var ringAnswered: Boolean = false
        private const val CALL_NOTIF_ID = 2009
        private const val CALL_CHANNEL_ID = "flora_incoming_call"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val appContext = context.applicationContext
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> onRinging(appContext, intent)
            TelephonyManager.EXTRA_STATE_OFFHOOK -> onAnswered(appContext)
            TelephonyManager.EXTRA_STATE_IDLE -> onIdle(appContext)
        }
    }

    private fun prefsBool(context: Context, key: String): Boolean {
        return try {
            runBlocking {
                context.callAnnounceStore.data.map {
                    it[booleanPreferencesKey(key)] ?: false
                }.first()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun onRinging(appContext: Context, intent: Intent) {
        if (!prefsBool(appContext, "call_announce")) return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?: return
        val now = System.currentTimeMillis()
        if (number == lastNumber && now - lastTimeMs < 30_000L) return
        lastNumber = number
        lastTimeMs = now
        ringNumber = number
        ringAnswered = false
        val name = try {
            runBlocking { ContactLookup.lookupNameByNumber(appContext, number) }
        } catch (e: Exception) {
            null
        }
        val message = if (!name.isNullOrBlank()) {
            "Boss, $name calling you"
        } else {
            "Boss, unknown number calling you"
        }
        // Phase 8: shared speak path — neural engine ho to wahi,
        // warna system TTS. Free-talk counting yahan kabhi nahi hoti.
        NeuralSpeaker.speakFixed(appContext, message)
        postCallUi(appContext, name, number, message)
        try {
            runBlocking {
                AutoLogStore(appContext).add(
                    "Call announce — ${name ?: "unknown"} (…${number.takeLast(4)})"
                )
            }
        } catch (e: Exception) {
        }
    }

    private fun onAnswered(appContext: Context) {
        ringAnswered = true
        clearCallUi(appContext)
    }

    private fun onIdle(appContext: Context) {
        val number = ringNumber
        val answered = ringAnswered
        ringNumber = null
        ringAnswered = false
        clearCallUi(appContext)
        if (number == null || answered) return
        if (!prefsBool(appContext, "missed_announce")) return
        val name = try {
            runBlocking { ContactLookup.lookupNameByNumber(appContext, number) }
        } catch (e: Exception) {
            null
        }
        val message = if (!name.isNullOrBlank()) {
            "Boss, $name ki missed call hai"
        } else {
            "Boss, unknown number ki missed call hai"
        }
        NeuralSpeaker.speakFixed(appContext, message)
        try {
            runBlocking {
                AutoLogStore(appContext).add(
                    "Missed call announce — ${name ?: "unknown"} (…${number.takeLast(4)})"
                )
            }
        } catch (e: Exception) {
        }
    }

    private fun clearCallUi(appContext: Context) {
        try {
            appContext.getSystemService(NotificationManager::class.java)
                ?.cancel(CALL_NOTIF_ID)
        } catch (e: Exception) {
        }
        try {
            AnnounceService.hideCallOverlay(appContext)
        } catch (e: Exception) {
        }
        try {
            IncomingCallHolder.onCallEnded?.invoke()
            IncomingCallHolder.clear()
        } catch (e: Exception) {
        }
    }

    private fun canUseFsi(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < 34) true
            else context.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun postCallUi(
        appContext: Context,
        name: String?,
        number: String,
        message: String
    ) {
        val fsiOk = canUseFsi(appContext)
        var posted = false
        val notifGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (notifGranted) {
            try {
                val nm = appContext.getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(
                    NotificationChannel(
                        CALL_CHANNEL_ID, "Flora incoming call",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
                val fullIntent = Intent(appContext, IncomingCallActivity::class.java).apply {
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, name)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, number)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                val fullPending = PendingIntent.getActivity(
                    appContext, CALL_NOTIF_ID, fullIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val builder = NotificationCompat.Builder(appContext, CALL_CHANNEL_ID)
                    .setContentTitle(message)
                    .setContentText("Flora announce — UI kholne ke liye dabao.")
                    .setContentIntent(fullPending)
                    .setSmallIcon(android.R.drawable.btn_star_big_on)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setAutoCancel(false)
                if (fsiOk) {
                    builder.setFullScreenIntent(fullPending, true)
                }
                nm?.notify(CALL_NOTIF_ID, builder.build())
                posted = true
            } catch (e: Exception) {
            }
        }
        // Overlay fallback: FSI na mile ya notification post na ho to
        // overlay koshish (best-effort — background FGS start block
        // ho sakta hai, tab heads-up + awaaz hi rahegi).
        if (!posted || !fsiOk) {
            try {
                if (android.provider.Settings.canDrawOverlays(appContext)) {
                    AnnounceService.showCallOverlay(appContext, name, number)
                }
            } catch (e: Exception) {
            }
        }
    }
}
