package com.flora.app.backup

import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * B6 Backup/Import — backup blob format (R-B6-1, R-B6-2).
 *
 * Pure JVM (koi Android class nahi) — isliye unit test me seedha
 * chalta hai. File layout:
 *   [magic "FLORABKP" (8 byte)][version 1 (1 byte)]
 *   [salt 16 byte][IV 12 byte][AES/GCM ciphertext]
 * Plaintext header me sirf magic + version + salt + IV hai — koi
 * identity, koi content, koi section naam nahi. Ciphertext ke
 * andar versioned manifest hai: har section ka naam + SHA-256
 * + JSON body. GCM tag + har section ka hash verify hota hai;
 * unknown version / corrupt / GCM fail / hash mismatch par
 * fail-closed refusal — koi partial restore nahi.
 *
 * Key: PBKDF2-HMAC-SHA256, header wala salt (salt secret nahi),
 * 200,000 iterations, 256-bit AES key. Backup PIN kahin save
 * nahi hota (R-B6-3 separation by construction: Guardian PIN
 * vault ka alias/file/verify path isse kabhi nahi milta — is PIN
 * ka koi KeyStore alias, koi file, koi session hi nahi hai;
 * Guardian ka 15-minute timeout is par kabhi laagu nahi hota).
 * Galat PIN = GCM fail = refusal. Bhoola hua backup PIN
 * UNRECOVERABLE hai by design — koi recovery backdoor nahi.
 */
class BackupFormatException(message: String) : Exception(message)

object FloraBackupCrypto {

    const val MAGIC = "FLORABKP"
    const val FORMAT_VERSION = 1
    const val SALT_BYTES = 16
    const val IV_BYTES = 12
    const val PBKDF2_ITERATIONS = 200_000
    const val KEY_BITS = 256
    const val FILE_EXTENSION = ".flora-backup"

    /** Section naam — collect/restore dono isi ek list se chalte hain. */
    object Sections {
        const val CHATS = "chats"
        const val MEMORIES = "memories"
        const val JOURNAL = "journal"
        const val DOCS = "docs"
        const val PROJECTS = "projects"
        const val PROJECT_MEMORY = "project_memory"
        const val SETTINGS = "settings"
        const val ROUTINES = "routines"
        const val MACROS = "macros"
        const val SECRETS = "secrets"

        val ALL = listOf(
            CHATS, MEMORIES, JOURNAL, DOCS, PROJECTS, PROJECT_MEMORY,
            SETTINGS, ROUTINES, MACROS, SECRETS
        )
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * Sections (naam -> JSON string) se poora blob file bytes
     * banata hai. Har section ka hash manifest me jaata hai.
     */
    fun encryptBlob(pin: String, sections: Map<String, String>): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val iv = ByteArray(IV_BYTES)
        SecureRandom().nextBytes(iv)
        val manifest = JSONObject()
        manifest.put("v", FORMAT_VERSION)
        val arr = JSONArray()
        for (name in sections.keys.sorted()) {
            val body = sections[name] ?: ""
            val entry = JSONObject()
            entry.put("name", name)
            entry.put("sha256", sha256Hex(body))
            entry.put("json", body)
            arr.put(entry)
        }
        manifest.put("sections", arr)
        val plain = manifest.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pin, salt), GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plain)
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        val out = ByteArray(magic.size + 1 + salt.size + iv.size + cipherText.size)
        var pos = 0
        System.arraycopy(magic, 0, out, pos, magic.size)
        pos += magic.size
        out[pos] = FORMAT_VERSION.toByte()
        pos += 1
        System.arraycopy(salt, 0, out, pos, salt.size)
        pos += salt.size
        System.arraycopy(iv, 0, out, pos, iv.size)
        pos += iv.size
        System.arraycopy(cipherText, 0, out, pos, cipherText.size)
        return out
    }

    /**
     * Blob kholta hai. Galat PIN, corrupt file, unknown version
     * ya hash mismatch par BackupFormatException — caller is par
     * koi restore step nahi chalata (koi partial write nahi).
     */
    fun decryptBlob(pin: String, bytes: ByteArray): Map<String, String> {
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        val headerSize = magic.size + 1 + SALT_BYTES + IV_BYTES
        if (bytes.size <= headerSize) {
            throw BackupFormatException("Backup file tooti hui lag rahi hai.")
        }
        for (i in magic.indices) {
            if (bytes[i] != magic[i]) {
                throw BackupFormatException("Ye Flora backup file nahi lag rahi.")
            }
        }
        val version = bytes[magic.size].toInt()
        if (version != FORMAT_VERSION) {
            throw BackupFormatException(
                "Ye backup naye/purane format ka hai (version $version) — is app version se nahi khulega."
            )
        }
        val salt = bytes.copyOfRange(magic.size + 1, magic.size + 1 + SALT_BYTES)
        val iv = bytes.copyOfRange(magic.size + 1 + SALT_BYTES, headerSize)
        val cipherText = bytes.copyOfRange(headerSize, bytes.size)
        val plain = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(pin, salt), GCMParameterSpec(128, iv))
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            throw BackupFormatException(
                "Backup nahi khula — PIN galat ho sakta hai ya file kharab hai. Kuch bhi badla nahi gaya hai."
            )
        }
        val manifest = try {
            JSONObject(String(plain, Charsets.UTF_8))
        } catch (e: Exception) {
            throw BackupFormatException("Backup ka andar ka hisaab padha nahi gaya.")
        }
        if (manifest.optInt("v", -1) != FORMAT_VERSION) {
            throw BackupFormatException("Backup manifest ka version samajh nahi aaya.")
        }
        val arr = manifest.optJSONArray("sections")
            ?: throw BackupFormatException("Backup me sections ka hisaab nahi mila.")
        val out = linkedMapOf<String, String>()
        for (i in 0 until arr.length()) {
            val entry = arr.optJSONObject(i)
                ?: throw BackupFormatException("Backup ka ek section toota hua hai.")
            val name = entry.optString("name", "")
            if (name.isEmpty() || out.containsKey(name)) {
                throw BackupFormatException("Backup ka section naam galat ya dohraya hua hai.")
            }
            val body = entry.optString("json", "")
            val expected = entry.optString("sha256", "")
            if (expected.isEmpty() || sha256Hex(body) != expected) {
                throw BackupFormatException("Backup ka '$name' hissa badla hua lag raha hai — poora restore roka gaya hai.")
            }
            out[name] = body
        }
        return out
    }

    /** R-B6-10: naam me sirf date-time — koi identity/content nahi. */
    fun backupFileName(nowMs: Long): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date(nowMs))
        return "Flora-Backup-$stamp$FILE_EXTENSION"
    }

    fun isBackupFileName(name: String): Boolean = name.endsWith(FILE_EXTENSION)
}
