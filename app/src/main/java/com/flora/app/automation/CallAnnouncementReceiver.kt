package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.data.AutoLogStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.callAnnounceStore by preferencesDataStore(name = "flora_prefs")

/**
 * Call announcement (v1.2 — name announce, user order 2026-10-30).
 * Incoming call aate hi caller ka NAAM bol kar batata hai:
 * "Boss, {name} calling you"; naam na mile to "Boss, unknown number
 * calling you" — digits kabhi nahi bole jayenge (paas khade log
 * number sun lete hain). Sirf tab jab user ne Settings me toggle ON
 * kiya ho (default OFF) aur Phone State + Call Log dono permissions
 * di hon (Android 9+ par number ke liye READ_CALL_LOG zaroori hai —
 * iske bina number null milta hai aur announce chupchaap nahi hota).
 * Ek hi ringing event dobara broadcast ho to 30 second me dobara
 * nahi bolega. Auto Log me sirf masked entry (naam/unknown +
 * aakhri 4 digits) — poora number kabhi log nahi hota, koi call
 * data server par nahi jaata. Bol kar receive/reject isme nahi hai
 * (dialer role alag raasta hai); auto-answer kabhi wapas nahi aayega.
 * Honest limit: app force-stop ho to announce nahi hoga.
 */
class CallAnnouncementReceiver : BroadcastReceiver() {

    companion object {
        @Volatile private var lastNumber: String? = null
        @Volatile private var lastTimeMs: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) !=
            TelephonyManager.EXTRA_STATE_RINGING
        ) return
        val enabled = try {
            runBlocking {
                context.callAnnounceStore.data.map {
                    it[booleanPreferencesKey("call_announce")] ?: false
                }.first()
            }
        } catch (e: Exception) {
            false
        }
        if (!enabled) return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?: return
        val now = System.currentTimeMillis()
        if (number == lastNumber && now - lastTimeMs < 30_000L) return
        lastNumber = number
        lastTimeMs = now
        val appContext = context.applicationContext
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
        try {
            runBlocking {
                AutoLogStore(appContext).add(
                    "Call announce — ${name ?: "unknown"} (…${number.takeLast(4)})"
                )
            }
        } catch (e: Exception) {
        }
    }
}
