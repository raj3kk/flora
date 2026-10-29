package com.flora.app.backup

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * B6 unit tests — sirf pure-JVM crypto/format layer.
 * Repo CI sirf :app:assembleDebug chalata hai, isliye ye tests
 * CI par execute nahi hote — local
 * `./gradlew :app:testDebugUnitTest` se chalane ke liye hain.
 * Koi asli PIN/secret fixture me nahi — sirf synthetic PIN.
 */
// Robolectric runner: plain JUnit par org.json sirf stub hota hai
// (har method RuntimeException) — runner ke neeche real org.json
// implementation chalti hai, isliye encrypt/decrypt + manifest
// tests asli code path par execute hote hain.
@RunWith(RobolectricTestRunner::class)
class FloraBackupCryptoTest {

    private val pin = "test-pin-1234"

    private val sections = mapOf(
        FloraBackupCrypto.Sections.CHATS to "[{\"u\":true,\"t\":\"hello\",\"ts\":1}]",
        FloraBackupCrypto.Sections.SETTINGS to "{\"theme\":\"light\"}",
        FloraBackupCrypto.Sections.SECRETS to "{\"link\":{}}",
    )

    @Test
    fun roundTripKeepsEverySection() {
        val blob = FloraBackupCrypto.encryptBlob(pin, sections)
        val back = FloraBackupCrypto.decryptBlob(pin, blob)
        assertEquals(sections, back)
    }

    @Test
    fun headerHasMagicVersionSaltAndIvOnly() {
        val blob = FloraBackupCrypto.encryptBlob(pin, sections)
        val magic = FloraBackupCrypto.MAGIC.toByteArray(Charsets.US_ASCII)
        for (i in magic.indices) assertEquals(magic[i], blob[i])
        assertEquals(FloraBackupCrypto.FORMAT_VERSION.toByte(), blob[magic.size])
        val headerSize = magic.size + 1 +
            FloraBackupCrypto.SALT_BYTES + FloraBackupCrypto.IV_BYTES
        assertTrue(blob.size > headerSize)
    }

    @Test
    fun wrongPinIsRefused() {
        val blob = FloraBackupCrypto.encryptBlob(pin, sections)
        try {
            FloraBackupCrypto.decryptBlob("wrong-pin-0000", blob)
            fail("Galat PIN par decrypt hona hi nahi chahiye.")
        } catch (e: BackupFormatException) {
            assertTrue(e.message?.contains("PIN") == true || e.message?.isNotEmpty() == true)
        }
    }

    @Test
    fun tamperedCiphertextIsRefused() {
        val blob = FloraBackupCrypto.encryptBlob(pin, sections)
        val tampered = blob.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0x01).toByte()
        try {
            FloraBackupCrypto.decryptBlob(pin, tampered)
            fail("Tampered blob par decrypt hona hi nahi chahiye.")
        } catch (e: BackupFormatException) {
            // Expected — GCM tag fail.
        }
    }

    @Test
    fun unknownVersionIsRefused() {
        val blob = FloraBackupCrypto.encryptBlob(pin, sections)
        val tampered = blob.copyOf()
        tampered[FloraBackupCrypto.MAGIC.length] = 99.toByte()
        try {
            FloraBackupCrypto.decryptBlob(pin, tampered)
            fail("Unknown version par decrypt hona hi nahi chahiye.")
        } catch (e: BackupFormatException) {
            // Expected.
        }
    }

    @Test
    fun sectionHashMismatchIsRefused() {
        // Manifest ka hash bigaad kar, sahi key se dobara
        // encrypt karke blob banao — GCM pass hoga, hash fail hoga.
        val salt = ByteArray(FloraBackupCrypto.SALT_BYTES) { 7 }
        val iv = ByteArray(FloraBackupCrypto.IV_BYTES) { 3 }
        val manifest = JSONObject()
        manifest.put("v", FloraBackupCrypto.FORMAT_VERSION)
        val arr = JSONArray()
        val entry = JSONObject()
        entry.put("name", FloraBackupCrypto.Sections.CHATS)
        entry.put("sha256", "0000000000000000000000000000000000000000000000000000000000000000")
        entry.put("json", "[{\"u\":true,\"t\":\"hello\",\"ts\":1}]")
        arr.put(entry)
        manifest.put("sections", arr)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            FloraBackupCrypto.deriveKey(pin, salt),
            GCMParameterSpec(128, iv)
        )
        val cipherText = cipher.doFinal(manifest.toString().toByteArray(Charsets.UTF_8))
        val magic = FloraBackupCrypto.MAGIC.toByteArray(Charsets.US_ASCII)
        val blob = magic + byteArrayOf(FloraBackupCrypto.FORMAT_VERSION.toByte()) +
            salt + iv + cipherText
        try {
            FloraBackupCrypto.decryptBlob(pin, blob)
            fail("Hash mismatch par decrypt hona hi nahi chahiye.")
        } catch (e: BackupFormatException) {
            // Expected.
        }
    }

    @Test
    fun fileNameHasNoIdentityAndHasExtension() {
        val name = FloraBackupCrypto.backupFileName(1790000000000L)
        assertTrue(name.startsWith("Flora-Backup-"))
        assertTrue(FloraBackupCrypto.isBackupFileName(name))
        assertFalse(FloraBackupCrypto.isBackupFileName("photo.jpg"))
    }

    @Test
    fun emptySectionsRoundTrip() {
        val blob = FloraBackupCrypto.encryptBlob(pin, emptyMap())
        assertEquals(emptyMap<String, String>(), FloraBackupCrypto.decryptBlob(pin, blob))
    }
}
