package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flora.app.assistant.NeuralSpeaker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.callAnnounceStore by preferencesDataStore(name = "flora_prefs")

/**
 * Call announcement (video #65 ka Flora version, v1.1).
 * Incoming call aate hi number bol kar batata hai — sirf tab jab
 * user ne Settings me toggle ON kiya ho (default OFF) aur phone
 * permission di ho. Naam batana (contacts lookup) v1.2 ka kaam hai;
 * bol kar receive/reject karna CallScreening role ke saath v1.2 me
 * aayega — is receiver me uska daava nahi hai.
 * Honest limit: naye Android par background broadcast rok diya ja
 * sakta hai; tab announcement ke liye app ka background me chalna
 * zaroori hai. Koi call data server par nahi jaata.
 */
class CallAnnouncementReceiver : BroadcastReceiver() {
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
        val appContext = context.applicationContext
        // Phase 8: shared speak path — neural engine ho to wahi,
        // warna system TTS (pehle wala behaviour, shutdown
        // NeuralSpeaker ke andar hota hai). Free-talk counting
        // yahan kabhi nahi hoti.
        NeuralSpeaker.speakFixed(appContext, "Call aa raha hai, number $number se.")
    }
}
