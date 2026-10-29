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
 * ke strict encrypt/decrypt par hi chalta hai — encrypt fail par
 * null (koi plaintext fallback nahi), decrypt fail par file khulti
 * hi nahi. MemoryCrypto.decodeStored ka raw-read sirf legacy
 * migration READ hai (purana plaintext padhna), fail-open WRITE
 * fallback kabhi nahi hai — ye store us read ko call bhi nahi
 * karta (index flow me seedha decrypt). Baaki stores
 * (Docs/AutoLog/ChatMemory) ka legacy-read vyavhaar waisa hi hai.
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
                // A3 P2-9: naam kabhi dobara istemal nahi — max + 1
                // (size + 1 nahi, warna bahar se hati version file
                // ka naam dobara ban kar purana snapshot overwrite
                // ho jata). Normal flow me naam format wahi (vN).
                val next = (listVersionNames(projectId, fileId)
                    .mapNotNull { it.removePrefix("v").toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
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

    /**
     * A3 P2-7: encode fail par chupchaap skip nahi — caller ko
     * false milta hai (koi signal na hone se metadata loss chhup
     * jata tha). Caller is par failure dikhata hai.
     */
    private suspend fun saveIndex(index: ProjectIndex): Boolean {
        val encoded = MemoryCrypto.encodeStored(json.encodeToString(ProjectIndex.serializer(), index))
            ?: return false
        appContext.projectDataStore.edit { it[indexKey] = encoded }
        return true
    }

    /**
     * A3 P2-8 (R-A3-A7, fail-closed only): stored index maujood
     * hai par decrypt/parse nahi hota to true — khaali index
     * (koi index abhi bana hi nahi) corrupt nahi hai. FilesDir
     * ko dobara scan karke recover karne ki koshish is fix me
     * NAHI hai (alag item banega agar kabhi chahiye to).
     */
    suspend fun isIndexCorrupt(): Boolean {
        val raw = appContext.projectDataStore.data.first()[indexKey] ?: ""
        if (raw.isBlank()) return false
        val decrypted = MemoryCrypto.decrypt(raw) ?: return true
        return try {
            json.decodeFromString(ProjectIndex.serializer(), decrypted)
            false
        } catch (e: Exception) {
            true
        }
    }

    suspend fun listProjects(): List<ProjectMeta> = index.first().projects

    suspend fun createProject(name: String): ProjectMeta {
        if (isIndexCorrupt()) {
            // Fail-closed: corrupt index par overwrite rok do —
            // is index ko padh kar save karna purane projects ka
            // metadata orphan kar dega. Recovery = backup restore.
            throw IllegalStateException("project index kharab hai — naya project banane se purana index overwrite ho jayega, isliye roka gaya")
        }
        val clean = name.trim().take(100).ifBlank { "Mera project" }
        val current = index.first()
        val meta = ProjectMeta(
            id = UUID.randomUUID().toString().substring(0, 8),
            name = clean,
            createdAtMs = System.currentTimeMillis(),
        )
        if (!saveIndex(current.copy(projects = current.projects + meta))) {
            throw IllegalStateException("project index save nahi hua — project bana par list me nahi dikhega, dobara koshish karo")
        }
        return meta
    }

    /** Nayi file ya maujooda file ka naya version — index + snapshot dono update hote hain. */
    suspend fun saveProjectFile(projectId: String, fileName: String, content: String): Result<ProjectFileMeta> {
        if (isIndexCorrupt()) {
            // Fail-closed (A3 P2-8): corrupt index par file likhne
            // se pehle hi roko — warna save ke baad ka index
            // overwrite purana metadata orphan kar dega.
            return Result.failure(IllegalStateException("project index kharab hai — save karne se purana index overwrite ho jayega, isliye roka gaya"))
        }
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
        if (!saveIndex(current.copy(projects = current.projects.map { if (it.id == projectId) updatedProject else it }))) {
            // A3 P2-7: index save fail ko chhupaya nahi — file
            // bytes likh chuke hain, par metadata (version list)
            // save nahi hui, isliye caller ko failure milega.
            return Result.failure(IllegalStateException("project index save nahi hua"))
        }
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

    /**
     * B6 restore (R-B6-9): REPLACE, kabhi merge nahi — B6
     * "restore" (device-state restore) ka matlab yehi hai, ye
     * restoreVersion (project-file version rollback) se bilkul
     * alag hai, dono ko UI/log me kabhi milaya nahi jaata.
     * Layout/version format chheda nahi gaya: wahi projects/
     * dir, wahi .enc files, wahi index shape. Farq sirf itna:
     * version history backup ka hissa nahi hai (sirf current
     * file content restore hota hai), isliye purane versions
     * restore ke baad nahi rehte. IDs backup wali hi rehti
     * hain (naye random ID nahi bante), taaki file mapping
     * toote nahi. Sab kuch validate hone ke baad hi purana
     * tree hataya jaata hai; koi file save fail ho to false
     * (caller poora import fail batata hai).
     */
    suspend fun replaceAllProjects(
        projects: List<ProjectMeta>,
        contents: Map<String, Map<String, String>>,
    ): Boolean {
        val idOk = Regex("^[A-Za-z0-9_-]{1,64}$")
        for (project in projects) {
            if (!idOk.matches(project.id) || project.name.isBlank()) return false
            for (file in project.files) {
                if (!idOk.matches(file.id) || file.name.isBlank()) return false
                val content = contents[project.id]?.get(file.name)
                    ?: return false
                if (content.length > ProjectFileStore.MAX_CONTENT_CHARS) return false
            }
        }
        return try {
            val projectsDir = File(appContext.filesDir, "projects")
            if (projectsDir.exists()) projectsDir.deleteRecursively()
            for (project in projects) {
                for (file in project.files) {
                    val content = contents[project.id]?.get(file.name)
                        ?: return false
                    val saved = fileStore.saveText(project.id, file.id, content)
                    if (saved.isFailure) return false
                }
            }
            val restored = projects.map { project ->
                project.copy(
                    createdAtMs = project.createdAtMs,
                    files = project.files.map { it.copy(versions = emptyList()) },
                )
            }
            // A3 P2-7: index save fail = restore fail (caller
            // BackupFailure dega, success ka jhootha daava nahi).
            // NOTE: ye jaanboojh kar replace hai — B6 restore
            // khud intentional full-replace hai, P2-8 corrupt-gate
            // is par lagu nahi hota (wahi recovery raasta hai).
            if (!saveIndex(ProjectIndex(projects = restored))) return false
            true
        } catch (e: Exception) {
            false
        }
    }
}
