package com.flora.app.voice

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
 * VoiceVault — enrolled d-vector ka encrypted on-device store.
 * PinVault jaisa hi pattern (AndroidKeyStore AES/GCM), sirf
 * voice vector ke liye alag alias/file:
 * - Kabhi plain file, backup, server, ya message me nahi.
 * - Voice template akela PIN unlock kabhi nahi karega —
 *   Guardian check bhi chahiye (standing rule).
 */
class VoiceVault(private val context: Context) {
    private val alias = "flora_voice_key"
    private val fileName = "voicevec.enc"

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

    fun store(vec: FloatArray): Boolean {
        return try {
            val plain = ByteBuffer.allocate(vec.size * 4)
                .apply { asFloatBuffer().put(vec) }.array()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plain)
            val out = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(cipherText, 0, out, iv.size, cipherText.size)
            File(context.filesDir, fileName).writeBytes(out)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun load(): FloatArray? {
        return try {
            val data = File(context.filesDir, fileName).readBytes()
            if (data.size < 12) return null
            val iv = data.copyOfRange(0, 12)
            val cipherText = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText)
            val buf = ByteBuffer.wrap(plain).asFloatBuffer()
            val out = FloatArray(plain.size / 4)
            buf.get(out)
            out
        } catch (e: Exception) {
            null
        }
    }

    fun hasEnrolled(): Boolean = File(context.filesDir, fileName).exists()

    fun clear() {
        File(context.filesDir, fileName).delete()
    }
}
