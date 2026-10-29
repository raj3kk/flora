package com.flora.app.models

import android.content.Context
import android.util.Log
import com.flora.app.assistant.HindiG2P
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.core.FloraConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Neural engine attach — EK hi jagah (Lane C).
 *
 * Idempotent + throw-proof + IO-scope:
 *  - Har app-start par KokoroOrtEngine.create() IO thread par
 *    chalta hai; onCreate kabhi block nahi hota.
 *  - Guard: KokoroModelManager.isReady() (.ok + dono file)
 *    AUR dono file ki EXACT size (FloraConfig sizes se milan
 *    — sirf "> 0" nahi). Size mismatch = corrupt/partial file
 *    ka sanket → attach nahi, engine null hi rahega.
 *  - Har app-start par 354 MB re-hash NAHI hota: yahan koi
 *    SHA nahi padha jata (SHA sirf download ke waqt verify
 *    hota hai, KokoroModelManager.download me; isReady bhi
 *    sirf .ok + existence dekhta hai).
 *  - Koi bhi fail (guard fail, session fail, I/O role
 *    mismatch, G2P fail) → engine null hi rehta hai, system
 *    TTS (Lily) barkaraar — awaaz kabhi nahi rukegi.
 *  - Success par HI NeuralSpeaker.engine set hota hai; uske
 *    baad VoiceEngine + call/event announce raaste apne aap
 *    neural ho jate hain (koi caller chhedna nahi padta).
 *
 * G2P seam (coordinator note): HindiG2P Worker 1 bana raha
 * hai. Assumed API: object HindiG2P, fun tokenize(text:
 * String): List<Int> (unwrapped vocab indices). Agar uska
 * asli API alag ho to SIRF neeche wali ek line badalni hai
 * (HindiG2P::tokenize) — engine file ko chhoona nahi padega.
 *
 * Retry: attach fail hone par `attached` false hi rehta hai,
 * isliye Settings download-success branch se dobara attach
 * ho sakta hai. Ek baar attached=true ho to dobara attach
 * nahi hota (wahi model dobara session banana bekaar hai).
 */
object NeuralVoiceAttach {

    private const val TAG = "KokoroAttach"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var attached = false

    @Volatile
    private var attaching = false

    fun isAttached(): Boolean = attached

    /**
     * Async, non-blocking, throw-proof. Kisi bhi thread se
     * bula sakte ho (Application.onCreate se bhi).
     */
    fun attach(context: Context) {
        if (attached) return
        synchronized(this) {
            if (attached || attaching) return
            attaching = true
        }
        val appContext = context.applicationContext
        scope.launch {
            try {
                if (!KokoroModelManager.isReady(appContext)) {
                    Log.i(TAG, "attach skipped: model not ready")
                    return@launch
                }
                val model = KokoroModelManager.modelFile(appContext)
                val voices = KokoroModelManager.voicesFile(appContext)
                // EXACT size match — sirf > 0 nahi. (length()
                // file padhta nahi, isliye ye re-hash nahi hai.)
                if (model.length() != FloraConfig.KOKORO_MODEL_SIZE) {
                    Log.w(
                        TAG,
                        "attach skipped: model size ${model.length()} " +
                            "!= ${FloraConfig.KOKORO_MODEL_SIZE}"
                    )
                    return@launch
                }
                if (voices.length() != FloraConfig.KOKORO_VOICES_SIZE) {
                    Log.w(
                        TAG,
                        "attach skipped: voices size ${voices.length()} " +
                            "!= ${FloraConfig.KOKORO_VOICES_SIZE}"
                    )
                    return@launch
                }
                // create() IO thread par hi hai (ye scope
                // Dispatchers.IO hai) + andar main-thread guard
                // bhi hai; fail hone par throw → neeche catch.
                val engine = KokoroOrtEngine.create(
                    model.absolutePath,
                    voices.absolutePath,
                    HindiG2P::tokenize
                )
                // Success par HI set — is line se pehle koi
                // fail hua to engine null hi rehta hai.
                NeuralSpeaker.engine = engine
                attached = true
                Log.i(TAG, "Kokoro ORT engine attached (hf_alpha)")
            } catch (e: Exception) {
                // Fail-closed: engine null, system TTS chalta
                // rahega. Caller ko kabhi throw nahi.
                Log.w(TAG, "attach failed, system TTS fallback", e)
            } finally {
                attaching = false
            }
        }
    }
}
