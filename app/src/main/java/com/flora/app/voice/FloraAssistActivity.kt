package com.flora.app.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.flora.app.MainActivity

/**
 * Assist activity (v2.23, Phase 5) — default-assistant trio ka
 * teesra hissa (VoiceInteractionService + Session + ye).
 * System assist trigger par ye activity khulti hai, Flora ki
 * MainActivity aage laati hai, aur khud khatam ho jaati hai.
 * Koi UI nahi, koi command khud execute nahi karta, koi
 * unlock/posting nahi — Flora ka apna mic/voice pipeline hi
 * aage chalta hai (session wali guarantee wahi hai).
 */
class FloraAssistActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(launch)
        } catch (e: Exception) {
        }
        finish()
    }
}
