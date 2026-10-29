package com.flora.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * MemoryCrypto — on-device yaadon ka encryption (user order
 * 2026-10-28: data phone me hi rahe, encrypted file ho, sirf
 * Flora khol paye).
 *
 * - Key AndroidKeyStore me hai (hardware-backed jahan milega),
 *   kabhi file/server/backup me nahi.
 * - Ciphertext AES/GCM hai: file haath lag bhi jaye to bina isi
 *   phone + isi app ke KeyStore ke padha nahi ja sakta. Doosri
 *   app, file manager, ya backup-restore se ye text nahi khulta.
 * - App-private storage pehle se sirf Flora ki thi; ye uske
 *   upar doosri parat hai — file copy hone par bhi content
 *   safe rahe.
 * - Cycle 3, F3 (fail-closed): encrypt fail par caller ko
 *   plaintext fallback KABHI nahi milta — encodeStored null
 *   deta hai aur store us turn ko likhta hi nahi (purana
 *   encrypted data waisa hi rehta hai, plaintext kabhi file
 *   me nahi jata). Data lose hone se encryption ka jhootha
 *   daava zyada khatarnak hai, isliye fail = no-write.
 *   Decrypt fail = purana plain data (LEGACY MIGRATION READ
 *   ONLY — purana plaintext padhna migration ke liye chahiye;
 *   naya plaintext kabhi nahi likha jata).
 */
object MemoryCrypto {
    private const val ALIAS = "flora_data_key"

    private fun key(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = ks.getKey(ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val spec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply { init(spec) }
                .generateKey()
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(plain: String): String? {
        return try {
            val k = key() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, k)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val out = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(cipherText, 0, out, iv.size, cipherText.size)
            Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decrypt(encoded: String): String? {
        return try {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            if (data.size < 12) return null
            val k = key() ?: return null
            val iv = data.copyOfRange(0, 12)
            val cipherText = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stores ka shared encode/decode (sweep 2026-10-29) — pehle yehi
     * do one-liner har store me private duplicate the.
     * decodeStored: ciphertext ko plain me badlo; fail ho to raw
     * (purana plain) — YE SIRF LEGACY-MIGRATION READ HAI.
     * encodeStored (Cycle 3, F3): plain ko ciphertext me badlo;
     * encrypt fail ho to NULL (plaintext fallback HATA DIYA
     * gaya hai). Saare stores null par likhte hi nahi — naya
     * plaintext kabhi file me nahi jata.
     */
    fun decodeStored(raw: String): String = decrypt(raw) ?: raw
    fun encodeStored(json: String): String? = encrypt(json)
}
