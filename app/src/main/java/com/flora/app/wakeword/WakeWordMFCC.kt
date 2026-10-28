package com.flora.app.wakeword

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hey Flora wake-word MFCC draft — UNTESTED, prototype only.
 *
 * Ye file app code me NAHI hai (wakeword/ folder me draft hai).
 * Python prototype (prototype_mfcc.py) ka port hai: 16kHz mono
 * audio -> pre-emphasis -> 25ms frame / 10ms hop -> Hamming ->
 * 512 FFT -> 26 mel filters -> log -> DCT (13 coeffs, 0th chhod
 * kar 12-d mean vector) -> enrolled template se cosine.
 *
 * Prototype numbers (Python, 6 clips): leave-one-out positives
 * 0.732-0.896, silence/noise 0.232-0.314. Threshold yahan 0.60
 * sirf shuruaati guess hai — asli speech negatives se tune
 * hue bina isko ship NAHI karna.
 *
 * Audit note: FFT yahan radix-2 iterative hai (512 points).
 * Kotlin me ye code compile/test hue bina app me daalna mana
 * hai — pehle unit test (Python vector se match) chahiye.
 *
 * LOGIC AUDIT 2026-10-28: pre-emphasis, Hamming window, mel
 * filterbank bins, DCT (0th chhod kar 12), frame/hop, log —
 * sab Python prototype se step-by-step match kiye gaye hain.
 * Phir bhi ye sirf paper audit hai — asli unit test (ek hi
 * wav par Python vs Kotlin vector, cosine > 0.99) baaki hai.
 */
object WakeWordMFCC {
    private const val SR = 16000
    private const val FRAME_LEN = 400 // 25ms
    private const val HOP = 160 // 10ms
    private const val N_FFT = 512
    private const val N_MELS = 26

    private fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double) = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    private fun melFilterbank(): Array<DoubleArray> {
        val low = 0.0
        val high = SR / 2.0
        val melPoints = DoubleArray(N_MELS + 2) { i ->
            hzToMel(low) + (hzToMel(high) - hzToMel(low)) * i / (N_MELS + 1)
        }
        val hzPoints = melPoints.map { melToHz(it) }
        val bins = hzPoints.map { ((N_FFT + 1) * it / SR).toInt() }
        val fb = Array(N_MELS) { DoubleArray(N_FFT / 2 + 1) }
        for (m in 1..N_MELS) {
            for (k in bins[m - 1] until bins[m]) {
                fb[m - 1][k] = (k - bins[m - 1]).toDouble() / (bins[m] - bins[m - 1])
            }
            for (k in bins[m] until bins[m + 1]) {
                fb[m - 1][k] = (bins[m + 1] - k).toDouble() / (bins[m + 1] - bins[m])
            }
        }
        return fb
    }

    // Iterative radix-2 FFT, returns power spectrum (first half).
    private fun powerSpectrum(frame: DoubleArray): DoubleArray {
        val re = DoubleArray(N_FFT)
        val im = DoubleArray(N_FFT)
        for (i in frame.indices) re[i] = frame[i]
        var j = 0
        for (i in 1 until N_FFT) {
            var bit = N_FFT shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= N_FFT) {
            val ang = -2.0 * PI / len
            val wr = cos(ang)
            val wi = sin(ang)
            var i = 0
            while (i < N_FFT) {
                var wpr = 1.0
                var wpi = 0.0
                for (k in 0 until len / 2) {
                    val ur = re[i + k]
                    val ui = im[i + k]
                    val vr = re[i + k + len / 2] * wpr - im[i + k + len / 2] * wpi
                    val vi = re[i + k + len / 2] * wpi + im[i + k + len / 2] * wpr
                    re[i + k] = ur + vr
                    im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr
                    im[i + k + len / 2] = ui - vi
                    val nwr = wpr * wr - wpi * wi
                    wpi = wpr * wi + wpi * wr
                    wpr = nwr
                }
                i += len
            }
            len *= 2
        }
        return DoubleArray(N_FFT / 2 + 1) { re[it] * re[it] + im[it] * im[it] }
    }

    /** Audio (16kHz, -1..1 floats) se 12-d mean MFCC vector. */
    fun mfccMean(audio: FloatArray): DoubleArray? {
        if (audio.size < FRAME_LEN) return null
        val pre = DoubleArray(audio.size)
        pre[0] = audio[0].toDouble()
        for (i in 1 until audio.size) pre[i] = audio[i] - 0.97 * audio[i - 1]
        val window = DoubleArray(FRAME_LEN) { i ->
            0.54 - 0.46 * cos(2.0 * PI * i / (FRAME_LEN - 1))
        }
        val fb = melFilterbank()
        val frames = mutableListOf<DoubleArray>()
        var start = 0
        while (start + FRAME_LEN <= pre.size) {
            val frame = DoubleArray(FRAME_LEN) { pre[start + it] * window[it] }
            val spec = powerSpectrum(frame)
            val logMel = DoubleArray(N_MELS) { m ->
                var sum = 0.0
                for (k in spec.indices) sum += fb[m][k] * spec[k]
                ln(sum + 1e-10)
            }
            val dct = DoubleArray(12) { idx ->
                val k = idx + 1
                var sum = 0.0
                for (n in 0 until N_MELS) {
                    sum += logMel[n] * cos(PI * k * (n + 0.5) / N_MELS)
                }
                sum
            }
            frames.add(dct)
            start += HOP
        }
        if (frames.isEmpty()) return null
        val mean = DoubleArray(12)
        for (f in frames) for (i in 0 until 12) mean[i] += f[i] / frames.size
        return mean
    }

    fun cosine(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size) return -1.0
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return -1.0
        return dot / (sqrt(na) * sqrt(nb))
    }
}
