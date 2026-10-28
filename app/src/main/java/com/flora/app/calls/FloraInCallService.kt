package com.flora.app.calls

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dialer call-talk (v1.5, local — phone test ke baad hi "done").
 * Flora tabhi call utha sakti hai jab user khud Flora ko default
 * dialer banaye (system RoleManager dialog se, Connect screen se).
 * Role mile bina ye service kabhi call nahi uthayegi.
 *
 * HONEST LIMIT (pehle se bataya hua): Android public API se TTS ko
 * call ke uplink me daalna guarantee nahi hai — Flora ka greeting
 * speaker se bajega, par doosri taraf saaf sunai dega ya nahi, ye
 * sirf dialer-role phone test ke baad kaha ja sakta hai. Isliye
 * call-talk ka daava phone test PASS hone tak nahi kiya jaayega.
 */
class FloraInCallService : InCallService() {
    private var voice: VoiceEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = try {
            call.details?.handle?.schemeSpecificPart ?: "unknown number"
        } catch (e: Exception) {
            "unknown number"
        }
        if (voice == null) voice = VoiceEngine(applicationContext)
        scope.launch {
            try {
                val prefs = FloraPreferences(applicationContext)
                if (prefs.callTalk()) {
                    try {
                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                    } catch (e: Exception) {
                        // Answer fail: sirf announce karo, jhootha "utha liya" mat bolo.
                        voice?.speak("Call aa rahi hai $number se, utha nahi payi.")
                        return@launch
                    }
                    try {
                        setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                    } catch (e: Exception) {
                    }
                    voice?.speak("Namaste, Flora bol rahi hun. Aap kaun bol rahe hain?")
                } else {
                    voice?.speak("Call aa rahi hai $number se.")
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        try {
            voice?.stop()
        } catch (e: Exception) {
        }
    }
}
