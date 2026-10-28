package com.flora.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.flora.app.MainActivity

/**
 * Default assistant (v2.22, Phase 1, G3 full) — user Flora ko
 * system ka default assistant chunta hai (Settings ka button
 * system settings kholta hai, tap user ka), to assistant invoke
 * hone par ye session Flora ki MainActivity aage laata hai.
 * Uske baad Flora ka apna mic/voice pipeline hi chalta hai —
 * koi alag awaaz ka raasta nahi, koi background action nahi.
 * Session sirf Flora kholta hai; koi command khud execute nahi
 * karta, koi unlock/posting nahi karta.
 */
class FloraVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}

class FloraVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return FloraVoiceInteractionSession(this)
    }
}

class FloraVoiceInteractionSession(
    private val appContext: Context
) : VoiceInteractionSession(appContext) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        try {
            val launch = Intent(appContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            appContext.startActivity(launch)
        } catch (e: Exception) {
        }
    }
}
