package com.vaultstadio.api.routes.version

import com.vaultstadio.core.domain.model.FileVersion
import kotlinx.serialization.Serializable

/**
 * Request to create a new version.
 */
@Serializable
data class CreateVersionRequest(
    val comment: String? = null,
)

/**
 * Request to restore a version.
 */
@Serializable
data class RestoreVersionRequest(
    val versionNumber: Int,
    val comment: String? = null,
)

/**
 * File version response.
 */
@Serializable
data class FileVersionResponse(
    val id: String,
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val createdBy: String,
    val createdAt: String,
    val comment: String?,
    val isLatest: Boolean,
    val restoredFrom: Int?,
)

/**
 * Version history response.
 */
@Serializable
data class VersionHistoryResponse(
    val itemId: String,
    val itemName: String,
    val versions: List<FileVersionResponse>,
    val totalVersions: Int,
    val totalSize: Long,
)

/**
 * Version diff response.
 */
@Serializable
data class VersionDiffResponse(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int,
    val deletions: Int,
    val isBinary: Boolean,
    val patches: List<DiffPatchResponse>,
)

/**
 * Diff patch response.
 */
@Serializable
data class DiffPatchResponse(
    val operation: String,
    val startLine: Int,
    val endLine: Int,
    val oldContent: String?,
    val newContent: String?,
)

/**
 * Request for retention policy cleanup.
 */
@Serializable
data class CleanupRequest(
    val maxVersions: Int? = null,
    val maxAgeDays: Int? = null,
    val minVersionsToKeep: Int = 1,
)

@Serializable
internal data class VersionDownloadResponse(
    val versionId: String,
    val storageKey: String,
    val size: Long,
    val checksum: String,
)

@Serializable
internal data class VersionRestoreResponse(val message: String, val newVersion: FileVersionResponse)

@Serializable
internal data class VersionCleanupResponse(val message: String, val deletedVersions: List<String>)

internal fun FileVersion.toResponse() = FileVersionResponse(
    id = id,
    itemId = itemId,
    versionNumber = versionNumber,
    size = size,
    checksum = checksum,
    createdBy = createdBy,
    createdAt = createdAt.toString(),
    comment = comment,
    isLatest = isLatest,
    restoredFrom = restoredFrom,
)
