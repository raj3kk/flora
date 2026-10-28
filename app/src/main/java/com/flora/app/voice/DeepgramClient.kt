package com.flora.app.voice

import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * DeepgramClient (v2.22, mega build chunk 3, step 1: REST).
 * User ka faisla: Deepgram hi rahega. Key Settings se aati
 * hai; khali ho to transcribe null deta hai (koi fake nahi).
 * Audio WAV bana kar POST hota hai, transcript wapas aata
 * hai. Audio kabhi save nahi hota. Streaming WebSocket
 * (sachhi barge-in) agle chunk me aayega — ye REST step
 * uski buniyaad hai, barge-in ka daava is par nahi hoga.
 */
object DeepgramClient {

    private fun wavBytes(pcm: ShortArray): ByteArray {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // mono
        header.putInt(16000)
        header.putInt(16000 * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize)
        val out = ByteArrayOutputStream()
        out.write(header.array())
        val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        pcmBytes.asShortBuffer().put(pcm)
        out.write(pcmBytes.array())
        return out.toByteArray()
    }

    suspend fun transcribe(pcm: ShortArray, prefs: FloraPreferences): String? =
        withContext(Dispatchers.IO) {
            try {
                val key = prefs.deepgramApiKey()
                if (key.isEmpty() || pcm.isEmpty()) return@withContext null
                val url = URL(
                    "https://api.deepgram.com/v1/listen" +
                        "?model=nova-2&language=hi&smart_format=true"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Token $key")
                conn.setRequestProperty("Content-Type", "audio/wav")
                conn.outputStream.use { it.write(wavBytes(pcm)) }
                val stream = if (conn.responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
                val text = stream.bufferedReader().readText()
                conn.disconnect()
                val root = JSONObject(text)
                val transcript = root.optJSONObject("results")
                    ?.optJSONArray("channels")?.optJSONObject(0)
                    ?.optJSONArray("alternatives")?.optJSONObject(0)
                    ?.optString("transcript", "") ?: ""
                if (transcript.isBlank()) null else transcript
            } catch (e: Exception) {
                null
            }
        }
}
