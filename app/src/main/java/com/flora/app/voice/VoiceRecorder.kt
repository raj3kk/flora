package com.flora.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VoiceRecorder (v2.22, mega build chunk 3) — enrol aur
 * Guardian voice-check ke liye 16kHz mono PCM16 recording.
 * Audio kabhi save/upload nahi hota — sirf memory me rehta
 * hai, match/enrol ke baad bhool jaya jata hai.
 * Permission ya init fail ho to null (koi fake sample nahi).
 */
object VoiceRecorder {

    suspend fun recordSeconds(context: Context, seconds: Int): ShortArray? =
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext null
            }
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBuf <= 0) return@withContext null
            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBuf * 2
                )
            } catch (e: Exception) {
                return@withContext null
            }
            try {
                if (recorder.state != AudioRecord.STATE_INITIALIZED) return@withContext null
                val total = sampleRate * seconds
                val out = ShortArray(total)
                var filled = 0
                recorder.startRecording()
                val buf = ShortArray(1600)
                val deadline = System.currentTimeMillis() + seconds * 1000L + 2000L
                while (filled < total && System.currentTimeMillis() < deadline) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n > 0) {
                        val take = minOf(n, total - filled)
                        System.arraycopy(buf, 0, out, filled, take)
                        filled += take
                    } else if (n < 0) {
                        break
                    }
                }
                if (filled < sampleRate) null else out.copyOf(filled)
            } catch (e: Exception) {
                null
            } finally {
                try { recorder.stop() } catch (e: Exception) { }
                try { recorder.release() } catch (e: Exception) { }
            }
        }
}
