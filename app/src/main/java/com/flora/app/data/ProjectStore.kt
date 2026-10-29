package com.flora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val Context.projectDataStore: DataStore<Preferences> by preferencesDataStore(name = "flora_projects")

/**
 * B2 Builder core — project store (2026-10-29).
 *
 * Layout (app-private filesDir, koi shared folder nahi):
 *   filesDir/projects/<projectId>/<fileId>.enc
 *   filesDir/projects/<projectId>/versions/<fileId>/<vN>.enc
 * Har project file ek alag encrypted file hai. DataStore me sirf
 * chhota metadata index rehta hai (ProjectModels) — file bytes
 * kabhi DataStore me nahi jate (B2-16 split).
 *
 * Encryption-proof / fail-closed (B2-06): ye store MemoryCrypto
 * ke shared fail-open fallback ko CHHUTA BHI NAHI.
 * File content sirf strict encrypt/decrypt se guzarta hai: decrypt
 * null de to file khulti hi nahi — raw bytes kabhi content ki
 * tarah nahi padhe jate, aur tampered file refuse hoti hai, chupchaap
 * khul nahi jati. MemoryCrypto ka shared fallback baaki stores
 * (Docs/AutoLog/ChatMemory) ke liye waisa hi barkaraar hai.
 *
 * Network: is file me koi network import/call nahi hai (B2-15).
 * Server project code kabhi nahi dekhta — B2 me backup bhi nahi
 * (wo B6 ka kaam hai). File content kabhi log nahi hota.
 */
interface ProjectCipher {
    fun encrypt(plain: String): String?
    fun decrypt(encoded: String): String?
}

/** Production cipher — seedha MemoryCrypto ke strict AES/GCM par. */
object MemoryProjectCipher : ProjectCipher {
    override fun encrypt(plain: String): String? = MemoryCrypto.encrypt(plain)
    override fun decrypt(encoded: String): String? = MemoryCrypto.decrypt(encoded)
}

data class SaveResult(val versionCreated: Boolean, val versionName: String?)

/**
 * Pure file layer — Android Context ke bina, isliye unit-testable
 * hai (test me fake ProjectCipher + temp dir dekar). ProjectStore
 * (neeche) isi par DataStore index ka wrapper hai.
 */
class ProjectFileStore(val root: File, val cipher: ProjectCipher) {

    companion object {
        const val MAX_CONTENT_CHARS = 1_000_000
        const val MAX_EXTRACT_BYTES = 200_000
        private val ID_OK = Regex("^[A-Za-z0-9_-]{1,64}$")
        private val TEXT_EXTS = setOf("html", "htm", "css", "js", "json", "txt", "md")
    }

    private fun checkId(id: String) {
        require(ID_OK.matches(id)) { "bad id" }
    }

    fun projectDir(projectId: String): File {
        checkId(projectId)
        return File(root, "projects/$projectId")
    }

    /** Test/support: stored encrypted file ka File (content kabhi raw na padha jaye). */
    fun storedFile(projectId: String, fileId: String): File {
        checkId(fileId)
        return File(projectDir(projectId), "$fileId.enc")
    }

    private fun versionDir(projectId: String, fileId: String): File {
        checkId(fileId)
        return File(projectDir(projectId), "versions/$fileId")
    }

    fun listVersionNames(projectId: String, fileId: String): List<String> {
        val dir = versionDir(projectId, fileId)
        if (!dir.isDirectory) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .mapNotNull { it.name.removeSuffix(".enc").takeIf { n -> n.matches(Regex("^v[0-9]+$")) } }
            .sortedBy { it.removePrefix("v").toIntOrNull() ?: 0 }
    }

    /**
     * Save: pehle naya content encrypt karo — fail ho to kuch mat
     * chhedo (na naya likho, na version banao). Phir purani
     * ciphertext ko version file me copy karo, phir nayi file likho.
     * Har save par Auto Log entry caller (BuilderScreen) likhta hai.
     */
    fun saveText(projectId: String, fileId: String, plain: String): Result<SaveResult> {
        return try {
            checkId(projectId)
            checkId(fileId)
            require(plain.length <= MAX_CONTENT_CHARS) { "file too large" }
            val encoded = cipher.encrypt(plain)
                ?: return Result.failure(IllegalStateException("encrypt failed"))
            val dir = projectDir(projectId)
            if (!dir.exists()) dir.mkdirs()
            val target = storedFile(projectId, fileId)
            var versionName: String? = null
            if (target.exists()) {
                val oldBytes = target.readBytes()
                val vdir = versionDir(projectId, fileId)
                if (!vdir.exists()) vdir.mkdirs()
                val next = (listVersionNames(projectId, fileId).size + 1)
                versionName = "v$next"
                File(vdir, "$versionName.enc").writeBytes(oldBytes)
            }
            target.writeBytes(encoded.toByteArray(Charsets.UTF_8))
            Result.success(SaveResult(versionCreated = versionName != null, versionName = versionName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Strict read: decrypt null = fail-closed, raw bytes kabhi content nahi. */
    fun readText(projectId: String, fileId: String): Result<String> {
        return try {
            val target = storedFile(projectId, fileId)
            if (!target.exists()) return Result.failure(IllegalStateException("file not found"))
            val encoded = String(target.readBytes(), Charsets.UTF_8)
            val plain = cipher.decrypt(encoded)
                ?: return Result.failure(IllegalStateException("decrypt failed"))
            Result.success(plain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Version snapshot ka strict read (rollback UI Doctor scope hai — B2 sirf record karta hai). */
    fun readVersionText(projectId: String, fileId: String, versionName: String): Result<String> {
        return try {
            checkId(projectId)
            checkId(fileId)
            require(versionName.matches(Regex("^v[0-9]+$"))) { "bad version" }
            val f = File(versionDir(projectId, fileId), "$versionName.enc")
            if (!f.exists()) return Result.failure(IllegalStateException("version not found"))
            val plain = cipher.decrypt(String(f.readBytes(), Charsets.UTF_8))
                ?: return Result.failure(IllegalStateException("decrypt failed"))
            Result.success(plain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ZIP writer — stdlib java.util.zip, koi naya dep nahi (B2-04). */
    fun writeZip(outFile: File, entries: Map<String, String>): Result<Unit> {
        return try {
            outFile.parentFile?.mkdirs()
            ZipOutputStream(outFile.outputStream()).use { zip ->
                for ((rawName, content) in entries) {
                    val name = sanitizeZipName(rawName) ?: continue
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun sanitizeZipName(raw: String): String? {
        val base = raw.replace('\\', '/').substringAfterLast('/')
        if (base.isEmpty() || base == "." || base == "..") return null
        if (base.contains("..")) return null
        return base
    }
}

/**
 * Picked archive se pehli text entry nikaalo (B2-10, E2 add-on).
 * Sirf text entries (.html/.css/.js/.json/.txt/.md), size cap ke
 * andar. Binary entry, khaali archive, ya cap se badi entry par
 * null — caller plain-language error dikhata hai, content log nahi hota.
 * Zip Slip se bachne ke liye entry naam ka basename hi liya jata hai.
 */
data class ExtractedFile(val name: String, val content: String)

fun extractFirstTextEntry(zipBytes: ByteArray): ExtractedFile? {
    val textExts = setOf("html", "htm", "css", "js", "json", "txt", "md")
    return try {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val base = entry.name.replace('\\', '/').substringAfterLast('/')
                    val ext = base.substringAfterLast('.', "").lowercase()
                    if (base.isNotEmpty() && ext in textExts) {
                        val out = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val n = zip.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > ProjectFileStore.MAX_EXTRACT_BYTES) return null
                            out.write(buf, 0, n)
                        }
                        return ExtractedFile(base, String(out.toByteArray(), Charsets.UTF_8))
                    }
                }
                entry = zip.nextEntry
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

/** Android wrapper: DataStore metadata index + ProjectFileStore. */
class ProjectStore(
    context: Context,
    cipher: ProjectCipher = MemoryProjectCipher,
) {
    private val appContext = context.applicationContext
    private val fileStore = ProjectFileStore(appContext.filesDir, cipher)
    private val indexKey = stringPreferencesKey("projects_index_json")
    private val json = Json { ignoreUnknownKeys = true }

    val index: Flow<ProjectIndex> = appContext.projectDataStore.data.map { prefs ->
        val raw = prefs[indexKey] ?: ""
        if (raw.isBlank()) {
            ProjectIndex()
        } else {
            // Index bhi encrypted hai; decrypt fail = khaali index,
            // raw tampered JSON kabhi parse nahi hota.
            val decrypted = MemoryCrypto.decrypt(raw)
            if (decrypted == null) {
                ProjectIndex()
            } else {
                try {
                    json.decodeFromString(ProjectIndex.serializer(), decrypted)
                } catch (e: Exception) {
                    ProjectIndex()
                }
            }
        }
    }

    private suspend fun saveIndex(index: ProjectIndex) {
        val encoded = MemoryCrypto.encodeStored(json.encodeToString(ProjectIndex.serializer(), index))
        if (encoded != null) {
            appContext.projectDataStore.edit { it[indexKey] = encoded }
        }
    }

    suspend fun listProjects(): List<ProjectMeta> = index.first().projects

    suspend fun createProject(name: String): ProjectMeta {
        val clean = name.trim().take(100).ifBlank { "Mera project" }
        val current = index.first()
        val meta = ProjectMeta(
            id = UUID.randomUUID().toString().substring(0, 8),
            name = clean,
            createdAtMs = System.currentTimeMillis(),
        )
        saveIndex(current.copy(projects = current.projects + meta))
        return meta
    }

    /** Nayi file ya maujooda file ka naya version — index + snapshot dono update hote hain. */
    suspend fun saveProjectFile(projectId: String, fileName: String, content: String): Result<ProjectFileMeta> {
        val current = index.first()
        val project = current.projects.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("project not found"))
        val cleanName = fileName.trim().replace('\\', '/').substringAfterLast('/').take(100)
        if (cleanName.isBlank()) return Result.failure(IllegalStateException("file name empty"))
        val existing = project.files.firstOrNull { it.name == cleanName }
        val fileId = existing?.id ?: UUID.randomUUID().toString().substring(0, 8)
        val saved = fileStore.saveText(projectId, fileId, content)
        if (saved.isFailure) return Result.failure(saved.exceptionOrNull() ?: IllegalStateException("save failed"))
        val savedValue = saved.getOrNull() ?: return Result.failure(IllegalStateException("save failed"))
        val now = System.currentTimeMillis()
        val updatedMeta = if (existing != null) {
            val newVersions = if (savedValue.versionCreated && savedValue.versionName != null) {
                existing.versions + ProjectFileVersion(savedValue.versionName, now)
            } else {
                existing.versions
            }
            existing.copy(name = cleanName, updatedAtMs = now, versions = newVersions)
        } else {
            ProjectFileMeta(id = fileId, name = cleanName, updatedAtMs = now)
        }
        val updatedProject = project.copy(
            files = if (existing != null) {
                project.files.map { if (it.id == fileId) updatedMeta else it }
            } else {
                project.files + updatedMeta
            }
        )
        saveIndex(current.copy(projects = current.projects.map { if (it.id == projectId) updatedProject else it }))
        return Result.success(updatedMeta)
    }

    suspend fun readProjectFile(projectId: String, fileId: String): Result<String> =
        fileStore.readText(projectId, fileId)

    /**
     * B4-DOCTOR rollback (DOC-P1-5) — sirf REUSE, dobara nahi likha:
     * versions list index (ProjectFileMeta.versions) se, purana
     * content readVersionText (strict) se, wapas save
     * saveProjectFile se (jo maujooda content ka naya snapshot
     * bhi bana deta hai — rollback khud bhi wapas liya ja sakta hai).
     */
    suspend fun listFileVersions(projectId: String, fileId: String): List<String> {
        val current = index.first()
        val project = current.projects.firstOrNull { it.id == projectId } ?: return emptyList()
        val file = project.files.firstOrNull { it.id == fileId } ?: return emptyList()
        return file.versions.map { it.name }
    }

    suspend fun restoreVersion(projectId: String, fileId: String, versionName: String): Result<Unit> {
        val current = index.first()
        val project = current.projects.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("project not found"))
        val file = project.files.firstOrNull { it.id == fileId }
            ?: return Result.failure(IllegalStateException("file not found"))
        val old = fileStore.readVersionText(projectId, fileId, versionName)
        if (old.isFailure) return Result.failure(old.exceptionOrNull() ?: IllegalStateException("version read failed"))
        val oldContent = old.getOrNull() ?: return Result.failure(IllegalStateException("version read failed"))
        val saved = saveProjectFile(projectId, file.name, oldContent)
        return if (saved.isSuccess) Result.success(Unit) else Result.failure(
            saved.exceptionOrNull() ?: IllegalStateException("restore save failed")
        )
    }

    /** Export: pehle saari files strictly decrypt karo — koi fail ho to ZIP banta hi nahi. */
    suspend fun exportProjectZip(projectId: String, outFile: File): Result<Unit> {
        val current = index.first()
        val project = current.projects.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("project not found"))
        val entries = linkedMapOf<String, String>()
        for (f in project.files) {
            val content = fileStore.readText(projectId, f.id)
            if (content.isFailure) return Result.failure(content.exceptionOrNull() ?: IllegalStateException("read failed"))
            entries[f.name] = content.getOrNull() ?: ""
        }
        return fileStore.writeZip(outFile, entries)
    }
}
