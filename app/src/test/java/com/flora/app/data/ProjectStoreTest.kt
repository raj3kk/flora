package com.flora.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * B2 unit tests — sirf pure-JVM layer (ProjectFileStore, models,
 * ZIP, extract). AndroidKeyStore wala MemoryProjectCipher yahan
 * nahi chalta, isliye test me wahi strict contract fake AES/GCM
 * cipher se check hota hai: encrypt null = no-write, decrypt
 * null/tamper = fail-closed, raw bytes kabhi content nahi.
 * NOTE: repo CI sirf :app:assembleDebug chalata hai, isliye ye
 * tests CI par execute nahi hote — ye local `./gradlew
 * :app:testDebugUnitTest` se chalane ke liye hain.
 */
class ProjectStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeCipher : ProjectCipher {
        private val key = KeyGenerator.getInstance("AES").apply { init(128) }.generateKey()

        override fun encrypt(plain: String): String? {
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv
                val out = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
                val combined = ByteArray(iv.size + out.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(out, 0, combined, iv.size, out.size)
                java.util.Base64.getEncoder().encodeToString(combined)
            } catch (e: Exception) {
                null
            }
        }

        override fun decrypt(encoded: String): String? {
            return try {
                val data = java.util.Base64.getDecoder().decode(encoded)
                if (data.size < 12) return null
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data.copyOfRange(0, 12)))
                String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
    }

    private class NullEncryptCipher : ProjectCipher {
        override fun encrypt(plain: String): String? = null
        override fun decrypt(encoded: String): String? = null
    }

    private fun store(): ProjectFileStore =
        ProjectFileStore(tmp.newFolder(), FakeCipher())

    @Test
    fun modelsJsonRoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        val index = ProjectIndex(
            listOf(
                ProjectMeta(
                    id = "p1", name = "Test", createdAtMs = 1000L,
                    files = listOf(
                        ProjectFileMeta(
                            id = "f1", name = "index.html", updatedAtMs = 2000L,
                            versions = listOf(ProjectFileVersion("v1", 1500L))
                        )
                    )
                )
            )
        )
        val encoded = json.encodeToString(ProjectIndex.serializer(), index)
        val decoded = json.decodeFromString(ProjectIndex.serializer(), encoded)
        assertEquals(index, decoded)
        assertEquals("index.html", decoded.projects[0].files[0].name)
        assertEquals("v1", decoded.projects[0].files[0].versions[0].name)
    }

    @Test
    fun fileSaveReadRoundTrip() {
        val s = store()
        val saved = s.saveText("proj1", "file1", "<h1>Namaste</h1>")
        assertTrue(saved.isSuccess)
        assertFalse(saved.getOrNull()?.versionCreated ?: true)
        assertEquals("<h1>Namaste</h1>", s.readText("proj1", "file1").getOrNull())
    }

    @Test
    fun tamperedCiphertextRefusesFailClosed() {
        val s = store()
        assertTrue(s.saveText("proj1", "file1", "secret-content").isSuccess)
        val stored = s.storedFile("proj1", "file1")
        val bytes = stored.readBytes()
        val tampered = bytes.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0xFF).toByte()
        stored.writeBytes(tampered)
        val read = s.readText("proj1", "file1")
        assertTrue(read.isFailure)
        // Fail-closed: raw/tampered bytes kabhi content ki tarah nahi milte.
        assertNull(read.getOrNull())
    }

    @Test
    fun encryptFailureWritesNothing() {
        val s = ProjectFileStore(tmp.newFolder(), NullEncryptCipher())
        val saved = s.saveText("proj1", "file1", "kuch")
        assertTrue(saved.isFailure)
        assertFalse(s.storedFile("proj1", "file1").exists())
    }

    @Test
    fun versionSnapshotKeepsPreviousContent() {
        val s = store()
        assertTrue(s.saveText("proj1", "file1", "pehla").isSuccess)
        val saved2 = s.saveText("proj1", "file1", "doosra")
        assertTrue(saved2.isSuccess)
        assertTrue(saved2.getOrNull()?.versionCreated ?: false)
        assertEquals("v1", saved2.getOrNull()?.versionName)
        assertEquals(listOf("v1"), s.listVersionNames("proj1", "file1"))
        assertEquals("pehla", s.readVersionText("proj1", "file1", "v1").getOrNull())
        assertEquals("doosra", s.readText("proj1", "file1").getOrNull())
    }

    @Test
    fun zipWriteReadRoundTrip() {
        val s = store()
        val out = tmp.newFile("out.zip")
        val entries = linkedMapOf("index.html" to "<h1>Hi</h1>", "style.css" to "h1 { color: red; }")
        assertTrue(s.writeZip(out, entries).isSuccess)
        val found = mutableMapOf<String, String>()
        ZipInputStream(out.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                found[entry.name] = String(zip.readBytes(), Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        assertEquals(entries, found)
    }

    @Test
    fun zipEntryNameSanitized() {
        val s = store()
        val out = tmp.newFile("evil.zip")
        assertTrue(s.writeZip(out, mapOf("../evil.txt" to "x")).isSuccess)
        val names = mutableListOf<String>()
        ZipInputStream(out.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names.add(entry.name)
                entry = zip.nextEntry
            }
        }
        assertEquals(listOf("evil.txt"), names)
    }

    @Test
    fun extractFirstTextEntrySkipsBinary() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("pic.png"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("index.html"))
            zip.write("<h1>Extracted</h1>".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        val extracted = extractFirstTextEntry(baos.toByteArray())
        if (extracted == null) fail("expected an extracted text entry")
        assertEquals("index.html", extracted?.name)
        assertEquals("<h1>Extracted</h1>", extracted?.content)
    }

    @Test
    fun extractFromBinaryOnlyZipIsNull() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("pic.png"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }
        assertNull(extractFirstTextEntry(baos.toByteArray()))
    }
}
