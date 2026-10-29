package com.flora.app.data

import kotlinx.serialization.Serializable

/**
 * B2 Builder core — project metadata models (2026-10-29).
 *
 * Sirf metadata hai: ids, naam, waqt, version pointers. File ka
 * content kabhi isme nahi aata — content sirf app-private
 * filesDir me encrypted file me rehta hai (ProjectStore).
 * Isliye DataStore me sirf ye chhota index likha jata hai.
 */
@Serializable
data class ProjectFileVersion(
    val name: String,
    val savedAtMs: Long,
)

@Serializable
data class ProjectFileMeta(
    val id: String,
    val name: String,
    val updatedAtMs: Long,
    val versions: List<ProjectFileVersion> = emptyList(),
)

@Serializable
data class ProjectMeta(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val files: List<ProjectFileMeta> = emptyList(),
)

@Serializable
data class ProjectIndex(
    val projects: List<ProjectMeta> = emptyList(),
)
