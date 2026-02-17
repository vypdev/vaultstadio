/**
 * File Version Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

data class FileVersion(
    val id: String,
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val createdBy: String,
    val createdAt: Instant,
    val comment: String? = null,
    val isLatest: Boolean,
    val restoredFrom: Int? = null,
) {
    val isRestore: Boolean get() = restoredFrom != null
}

data class FileVersionHistory(
    val itemId: String,
    val itemName: String,
    val versions: List<FileVersion>,
    val totalVersions: Int,
    val totalSize: Long,
)

data class VersionDiff(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int,
    val deletions: Int,
    val isBinary: Boolean,
)

/**
 * Request to restore a file version.
 */
data class RestoreVersionRequest(
    val versionNumber: Int,
    val comment: String? = null,
)
