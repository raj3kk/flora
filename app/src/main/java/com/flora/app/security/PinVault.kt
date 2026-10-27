package com.flora.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * PIN storage — approved with strict conditions:
 * - Encrypted on-device secure store (AndroidKeyStore AES/GCM) ONLY.
 * - NEVER in a plain file, backup, server, or message.
 * - Default OFF; unlock only after voice + Guardian check (GuardianGate).
 * - The PIN value is never logged, never returned by any getter.
 */
class PinVault(private val context: Context) {
    private val alias = "flora_pin_key"
    private val fileName = "pin.enc"

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

    /** Returns true only if encryption + write succeeded. */
    fun store(pin: String): Boolean {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            val out = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(cipherText, 0, out, iv.size, cipherText.size)
            File(context.filesDir, fileName).writeBytes(out)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** True only when the given PIN decrypts to the stored PIN. */
    fun verify(pin: String): Boolean {
        return try {
            val data = File(context.filesDir, fileName).readBytes()
            if (data.size < 12) return false
            val iv = data.copyOfRange(0, 12)
            val cipherText = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText).toString(Charsets.UTF_8)
            plain == pin
        } catch (e: Exception) {
            false
        }
    }

    fun hasPin(): Boolean = File(context.filesDir, fileName).exists()

    fun clear() {
        File(context.filesDir, fileName).delete()
    }
}
