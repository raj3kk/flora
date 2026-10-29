package com.flora.app.voice

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * DeepgramStreamClient (v2.22, chunk 7) — Deepgram live
 * listen WebSocket. Audio sirf stream hota hai, kabhi save
 * nahi hota. Key khali ho to start false deta hai.
 * Interim results sirf UI ke liye hote hain; barge-in
 * sirf FINAL transcript par trigger hota hai.
 */
class DeepgramStreamClient {

    interface Listener {
        fun onFinal(text: String)
        fun onError()

        /**
         * N4/NOTCH-04: interim (non-final) transcript. Default
         * khali hai taaki purane implementer (jaise
         * FloraRecognitionService) bina badle compile hon.
         * Caller is par sirf TTS rokta hai (ARM) — naya turn
         * tabhi banta hai jab non-echo FINAL aaye (commit
         * gate wahi hai, speaker bleed se false turn nahi).
         */
        fun onPartial(text: String) {}
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()
    private var ws: WebSocket? = null

    fun start(key: String, listener: Listener): Boolean {
        if (key.isEmpty() || ws != null) return false
        val request = Request.Builder()
            .url(
                "wss://api.deepgram.com/v1/listen" +
                    "?model=nova-2&language=hi&smart_format=true&interim_results=true"
            )
            .addHeader("Authorization", "Token $key")
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val isFinal = root.optBoolean("is_final", false)
                    val transcript = root.optJSONObject("channel")
                        ?.optJSONArray("alternatives")?.optJSONObject(0)
                        ?.optString("transcript", "") ?: ""
                    if (isFinal && transcript.isNotBlank()) {
                        listener.onFinal(transcript)
                    } else if (!isFinal && transcript.isNotBlank()) {
                        listener.onPartial(transcript)
                    }
                } catch (e: Exception) {
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError()
            }
        })
        return true
    }

    fun sendPcm(pcm: ShortArray) {
        val ws = this.ws ?: return
        try {
            val bytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bytes.asShortBuffer().put(pcm)
            ws.send(bytes.array().toByteString())
        } catch (e: Exception) {
        }
    }

    fun stop() {
        try {
            ws?.close(1000, "done")
        } catch (e: Exception) {
        }
        ws = null
    }
}
