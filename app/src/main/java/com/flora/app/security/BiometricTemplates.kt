package com.flora.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Voice/face match templates — v2.3 prep, LOCAL ONLY (push nahi).
 *
 * Rules (design se, koi exception nahi):
 * - Templates sirf encrypted on-device rehte hain (AndroidKeyStore
 *   AES/GCM, PinVault wala pattern, alag key alias).
 * - Kabhi server, backup, file-share ya log par nahi jate.
 * - Template ka matlab sirf numbers ka vector hai — koi photo,
 *   koi audio sample, koi raw biometric yahan save nahi hota.
 * - Enrolment default OFF hai; delete-all enrolment screen se
 *   hoga (UI alag step me aayega).
 * - Embedding values kabhi log nahi hoti, kabhi getter se
 *   poori tarah bahar nahi aati — match function ke andar hi
 *   compare hoti hai (load* sirf matcher ke liye hai).
 */
class BiometricTemplates(private val context: Context) {
    private val alias = "flora_bio_key"
    private val faceFile = "face_template.enc"
    private val voiceFile = "voice_template.enc"
    // Wake-word template (v2.4): sirf MFCC mean vector (12 numbers),
    // koi audio sample nahi. Face/voice wali hi key + pattern.
    private val wakeFile = "wake_template.enc"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    private fun storeVector(fileName: String, vector: FloatArray): Boolean {
        return try {
            val buf = ByteBuffer.allocate(4 + vector.size * 4)
            buf.putInt(vector.size)
            for (v in vector) buf.putFloat(v)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(buf.array())
            val out = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(cipherText, 0, out, iv.size, cipherText.size)
            File(context.filesDir, fileName).writeBytes(out)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadVector(fileName: String): FloatArray? {
        return try {
            val data = File(context.filesDir, fileName).readBytes()
            if (data.size < 12) return null
            val iv = data.copyOfRange(0, 12)
            val cipherText = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText)
            val buf = ByteBuffer.wrap(plain)
            val size = buf.int
            if (size <= 0 || size > 4096) return null
            if (plain.size < 4 + size * 4) return null
            FloatArray(size) { buf.float }
        } catch (e: Exception) {
            null
        }
    }

    fun storeFace(vector: FloatArray): Boolean = storeVector(faceFile, vector)
    fun loadFace(): FloatArray? = loadVector(faceFile)

    fun storeWake(vector: FloatArray): Boolean = storeVector(wakeFile, vector)
    fun loadWake(): FloatArray? = loadVector(wakeFile)
    fun hasWake(): Boolean = File(context.filesDir, wakeFile).exists()

    fun clearAll() {
        File(context.filesDir, faceFile).delete()
        File(context.filesDir, voiceFile).delete()
        File(context.filesDir, wakeFile).delete()
    }
}
