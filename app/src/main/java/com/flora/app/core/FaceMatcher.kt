package com.flora.app.core

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Face-match core — v2.3 prep, LOCAL ONLY (push nahi).
 *
 * Model: MobileFaceNet TFLite, assets/MobileFaceNet.tflite
 * (5,233,396 bytes, git blob sha 3249c511be86..., sha256
 * d8ba40c0127f... — 2026-10-28 download + sha verified).
 * Licence: upstream repo Apache 2.0 (2026-10-28 verified).
 * Kisi repo ka code copy nahi hai — ye inference code apna hai.
 *
 * Pipeline: 112x112 crop bitmap → pixels ko [-1, 1] range me
 * scale karo → TFLite inference → 192-d embedding → L2
 * normalize → enrolled template se cosine similarity.
 *
 * IMPORTANT (imaandaari note): enrolment aur live check DONO
 * isi pipeline se guzarte hain, isliye threshold ka final
 * number phone par tune hoga — yahan default 0.55 sirf
 * shuruaati value hai, koi accuracy claim nahi.
 * Face-match AKELA kabhi PIN unlock nahi karega (design rule).
 */
class FaceMatcher(context: Context) {
    private val interpreter: Interpreter? = try {
        val asset = context.assets.openFd("MobileFaceNet.tflite")
        val buffer = asset.createInputStream().readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(buffer.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(buffer)
        // AUDIT FIX 2026-10-28: put() ke baad position end par hoti
        // hai — rewind ke bina Interpreter khaali buffer padhta.
        byteBuffer.rewind()
        Interpreter(byteBuffer)
    } catch (e: Exception) {
        null
    }

    fun isReady(): Boolean = interpreter != null

    /** Bitmap (face crop) se 192-d normalized embedding, ya null. */
    fun embedding(crop: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        return try {
            val resized = Bitmap.createScaledBitmap(crop, 112, 112, true)
            val pixels = IntArray(112 * 112)
            resized.getPixels(pixels, 0, 112, 0, 0, 112, 112)
            val input = ByteBuffer.allocateDirect(112 * 112 * 3 * 4)
            input.order(ByteOrder.nativeOrder())
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                input.putFloat((r - 127.5f) / 128f)
                input.putFloat((g - 127.5f) / 128f)
                input.putFloat((b - 127.5f) / 128f)
            }
            // AUDIT FIX 2026-10-28: input buffer bhi run se pehle
            // rewind karna hai, warna TFLite end-position se padhega.
            input.rewind()
            val output = Array(1) { FloatArray(192) }
            interp.run(input, output)
            normalize(output[0])
        } catch (e: Exception) {
            null
        }
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm == 0f) return v
        return FloatArray(v.size) { v[it] / norm }
    }

    /** Cosine similarity (dono vectors pehle se normalized hon). */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return -1f
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot
    }

    fun matches(live: FloatArray, enrolled: FloatArray, threshold: Float = 0.55f): Boolean {
        return cosine(live, enrolled) >= threshold
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            // Ignore — close failure is not a user-visible error.
        }
    }
}
