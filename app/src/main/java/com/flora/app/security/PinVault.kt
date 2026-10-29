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
 *
 * Cycle 3, F2 (PIN unlimited guesses fix): verify() par attempt
 * limit + time-based lockout. 5 lagatar galat tries ke baad
 * 1 minute ka lockout lagta hai; uske baad har galat try par
 * lockout doguna hota hai (cap 15 minute). Lockout ke dauraan
 * verify() turant false deta hai (decrypt bhi nahi karta) —
 * isliye lockout me brute-force aage nahi badh sakta. PERMANENT
 * brick kabhi nahi: lockout hamesha time-based hai, waqt beette
 * hi dobara koshish ho sakti hai; sahi PIN par counter reset ho
 * jata hai. Counter + lockout SharedPreferences me hai (app
 * restart se reset nahi hota — warna restart karke limit toda
 * ja sakta). Guardian ka Forgot-PIN anti-bypass rule waisa hi
 * hai (Guardian OFF bina PIN ke nahi hota) — ye lockout us
 * rule ko nahi todta, sirf galat guesses ko dheema karta hai.
 * Callers lockout ko pehchanne ke liye isLockedOut() /
 * lockoutRemainingSeconds() dekh sakte hain (verify() sirf
 * Boolean deta hai, taaki saare call sites ek hi verify se
 * guzrein — fix ek jagah hai).
 */
class PinVault(private val context: Context) {
    companion object {
        /** Lagatar galat tries, iske baad lockout lagta hai. */
        const val MAX_FAILED_ATTEMPTS = 5

        /** Pehla lockout: 1 minute. */
        const val BASE_LOCKOUT_MS = 60_000L

        /** Lockout kabhi isse zyada lamba nahi (permanent brick nahi). */
        const val MAX_LOCKOUT_MS = 15L * 60L * 1000L
    }

    private val alias = "flora_pin_key"
    private val fileName = "pin.enc"

    private fun guardPrefs() =
        context.getSharedPreferences("flora_pin_guard", Context.MODE_PRIVATE)

    /** Lockout abhi chal raha hai ya nahi (expiry ke baad false). */
    @Synchronized
    fun isLockedOut(): Boolean {
        val until = guardPrefs().getLong("lockout_until_ms", 0L)
        return until > System.currentTimeMillis()
    }

    /** Lockout me bacha hua waqt (seconds, upar ki taraf); lockout na ho to 0. */
    @Synchronized
    fun lockoutRemainingSeconds(): Long {
        val until = guardPrefs().getLong("lockout_until_ms", 0L)
        val left = until - System.currentTimeMillis()
        if (left <= 0L) return 0L
        return (left + 999L) / 1000L
    }

    private fun resetGuard() {
        guardPrefs().edit().remove("failed_count").remove("lockout_until_ms").apply()
    }

    private fun recordFailedAttempt() {
        val prefs = guardPrefs()
        val failed = prefs.getInt("failed_count", 0) + 1
        val editor = prefs.edit().putInt("failed_count", failed)
        if (failed >= MAX_FAILED_ATTEMPTS) {
            // Har extra galat try par doguna (cap ke saath):
            // 5th fail -> 1 min, 6th -> 2 min, 7th -> 4 min...
            val steps = failed - MAX_FAILED_ATTEMPTS
            var lockout = BASE_LOCKOUT_MS
            repeat(steps) {
                lockout = minOf(lockout * 2L, MAX_LOCKOUT_MS)
            }
            editor.putLong(
                "lockout_until_ms",
                System.currentTimeMillis() + lockout
            )
        }
        editor.apply()
    }

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
            // Naya PIN = naya counter (purane PIN ki galat tries
            // naye PIN par lockout na banayein).
            resetGuard()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** True only when the given PIN decrypts to the stored PIN. */
    @Synchronized
    fun verify(pin: String): Boolean {
        // Lockout me turant fail — decrypt tak nahi (koi
        // timing/brute-force faayda nahi).
        if (isLockedOut()) return false
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return false
        return try {
            val data = file.readBytes()
            if (data.size < 12) return false
            val iv = data.copyOfRange(0, 12)
            val cipherText = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(cipherText).toString(Charsets.UTF_8)
            if (plain == pin) {
                resetGuard()
                true
            } else {
                recordFailedAttempt()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun hasPin(): Boolean = File(context.filesDir, fileName).exists()

    fun clear() {
        File(context.filesDir, fileName).delete()
        resetGuard()
    }
}
