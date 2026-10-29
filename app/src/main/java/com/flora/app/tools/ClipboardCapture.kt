package com.flora.app.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.flora.app.data.BuilderAuthKind
import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences

/**
 * B3-02 clipboard capture (E1, auth option D).
 *
 * Ordering — code me enforce hota hai, sirf UI bharosa nahi:
 * 1. Prompt PEHLE: "clipboard se key loon?" — promptConfirmed
 *    false ho to read hota hi nahi (PromptRequired).
 * 2. Foreground read: caller foreground me hota hai, isliye koi
 *    naya manifest permission nahi, koi background clipboard
 *    access nahi — modern Android khud clipboard-read toast
 *    dikhata hai (kuch chhupa hua nahi).
 * 3. Validation save se PEHLE: shape galat ya cookie/session/
 *    password shape ho to kabhi save nahi (Invalid + re-copy ask,
 *    never guess-save).
 * 4. Save: KeyStore-encrypted, FloraPreferences builder slot me
 *    (PAT kind — clipboard se aayi cheez hamesha PAT mani jati
 *    hai, OAuth nahi).
 * 5. Auto-clear save ke BAAD — sirf jab opt-out flag OFF na ho
 *    (default ON, R-B3-9 DataStore setting).
 * Full key kabhi log/chat/screen par nahi — result me sirf
 * status hota hai, value kabhi nahi.
 * LicenceScreen ka copy path (WRITE) isse alag hai — capture
 * ke liye reuse nahi hua (B3-02).
 */
object ClipboardCapture {

    sealed class Decision {
        object PromptRequired : Decision()
        object Empty : Decision()
        data class Invalid(val message: String) : Decision()
        data class Ready(val value: String) : Decision()
    }

    sealed class Result {
        object Saved : Result()
        object PromptRequired : Result()
        object Empty : Result()
        data class Invalid(val message: String) : Result()
        object SaveFailed : Result()
    }

    /** Pure ordering decision — unit test me bina Android ke check hota hai. */
    fun decide(
        promptConfirmed: Boolean,
        clipText: String?,
        service: BuilderService
    ): Decision {
        if (!promptConfirmed) return Decision.PromptRequired
        val text = clipText?.trim() ?: ""
        if (text.isEmpty()) return Decision.Empty
        val error = KeyInputGuard.validateForService(service, text)
        if (error != null) return Decision.Invalid(error)
        return Decision.Ready(text)
    }

    suspend fun captureFromClipboard(
        context: Context,
        prefs: FloraPreferences,
        service: BuilderService,
        promptConfirmed: Boolean
    ): Result {
        if (!promptConfirmed) return Result.PromptRequired
        val clipText = try {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = manager.primaryClip
            if (clip == null || clip.itemCount == 0) null
            else clip.getItemAt(0)?.text?.toString()
        } catch (e: Exception) {
            null
        }
        return when (val decision = decide(promptConfirmed, clipText, service)) {
            is Decision.PromptRequired -> Result.PromptRequired
            is Decision.Empty -> Result.Empty
            is Decision.Invalid -> Result.Invalid(decision.message)
            is Decision.Ready -> {
                val saved = try {
                    prefs.setBuilderToken(service, decision.value, BuilderAuthKind.PAT)
                } catch (e: Exception) {
                    false
                }
                if (!saved) return Result.SaveFailed
                val autoClear = try {
                    prefs.builderClipboardAutoClear()
                } catch (e: Exception) {
                    true
                }
                if (autoClear) clearClipboard(context)
                Result.Saved
            }
        }
    }

    fun clearClipboard(context: Context) {
        try {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (e: Exception) {
        }
    }
}
