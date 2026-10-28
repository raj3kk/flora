package com.flora.app.wakeword

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Wake-word audio capture — v2.4, LOCAL ONLY.
 * 16kHz mono PCM, AudioRecord se. Koi audio sample save nahi
 * hota, koi server par nahi jata — sirf MFCC vector banta hai
 * (enrol) ya match hota hai (service). Permission ke bina
 * record() null deta hai, crash nahi.
 */
object WakeWordRecorder {
    const val SAMPLE_RATE = 16000

    @SuppressLint("MissingPermission")
    fun recordMillis(millis: Int): FloatArray? {
        return try {
            val channel = AudioFormat.CHANNEL_IN_MONO
            val format = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, channel, format)
            if (minBuf <= 0) return null
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, channel, format, minBuf * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return null
            }
            val totalSamples = SAMPLE_RATE * millis / 1000
            val out = FloatArray(totalSamples)
            var filled = 0
            val buf = ShortArray(1024)
            record.startRecording()
            try {
                while (filled < totalSamples) {
                    val read = record.read(buf, 0, minOf(buf.size, totalSamples - filled))
                    if (read <= 0) break
                    for (i in 0 until read) {
                        out[filled + i] = buf[i] / 32768f
                    }
                    filled += read
                }
            } finally {
                try { record.stop() } catch (e: Exception) { /* ignore */ }
                record.release()
            }
            if (filled < totalSamples / 2) null else out.copyOf(filled)
        } catch (e: Exception) {
            null
        }
    }
}
