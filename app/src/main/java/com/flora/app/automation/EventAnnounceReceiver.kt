package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale

private val Context.eventAnnounceStore by preferencesDataStore(name = "flora_prefs")

/**
 * Event triggers (video #68 ka Flora version, v1.3 draft — Settings
 * toggle wiring v1.3 push se pehle hogi).
 * Charger lagna/nikalna aur battery low par bol kar batata hai —
 * sirf tab jab user ne toggle ON kiya ho (default OFF).
 * Koi event data server par nahi jaata.
 */
class EventAnnounceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> "Charger lag gaya hai."
            Intent.ACTION_POWER_DISCONNECTED -> "Charger nikal gaya hai."
            Intent.ACTION_BATTERY_LOW -> "Battery low hai, charger lagana chahiye."
            else -> return
        }
        val enabled = try {
            runBlocking {
                context.eventAnnounceStore.data.map {
                    it[booleanPreferencesKey("event_announce")] ?: false
                }.first()
            }
        } catch (e: Exception) {
            false
        }
        if (!enabled) return
        val appContext = context.applicationContext
        var tts: TextToSpeech? = null
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setOnUtteranceProgressListener(
                    object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) {
                            tts?.shutdown()
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            tts?.shutdown()
                        }
                    }
                )
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "flora-event")
            }
        }
    }
}
