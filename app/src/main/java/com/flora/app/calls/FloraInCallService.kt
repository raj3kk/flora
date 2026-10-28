package com.flora.app.calls

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.flora.app.assistant.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dialer call-talk (v2.22, UI-first — local, phone test ke baad hi
 * "done"). Flora tabhi call dekh sakti hai jab user khud Flora ko
 * default dialer banaye (system RoleManager dialog se, Connect
 * screen se). Role mile bina ye service kabhi call nahi dekhegi.
 *
 * v2.22 BADLAV — pehle service callTalk on hone par KHUD answer kar
 * leti thi, user ko koi option nahi milta tha (aur callTalk off
 * hone par to koi UI hi nahi tha). Ab service kabhi auto-answer
 * nahi karti: CallActivity kholti hai, user 3 me se ek chunta hai
 * (Flora se baat karao / Khud baat karo / Kaato).
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
        CallHolder.number = number
        CallHolder.onAnswerFlora = {
            var answered = false
            try {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
                answered = true
            } catch (e: Exception) {
            }
            if (answered) {
                try {
                    setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                } catch (e: Exception) {
                }
                voice?.speak("Namaste, Flora bol rahi hun. Aap kaun bol rahe hain?")
            } else {
                voice?.speak("Call utha nahi payi.")
            }
        }
        CallHolder.onAnswerSelf = {
            try {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Exception) {
            }
            try {
                setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            } catch (e: Exception) {
            }
        }
        CallHolder.onDisconnect = {
            try {
                call.disconnect()
            } catch (e: Exception) {
            }
        }
        scope.launch {
            voice?.speak("Call aa rahi hai $number se.")
            try {
                val launch = Intent(this@FloraInCallService, CallActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                startActivity(launch)
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
        try {
            CallHolder.onRemoved?.invoke()
        } catch (e: Exception) {
        }
        CallHolder.clear()
    }
}
