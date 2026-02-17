/**
 * VaultStadio File Version Model
 *
 * Represents a version of a file in the storage system.
 * Supports version history, restore, and cleanup policies.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a specific version of a file.
 *
 * @property id Unique identifier for this version
 * @property itemId ID of the storage item this version belongs to
 * @property versionNumber Sequential version number (1, 2, 3, ...)
 * @property size Size of this version in bytes
 * @property checksum SHA-256 checksum of this version
 * @property storageKey Key used to locate this version in the storage backend
 * @property createdBy ID of the user who created this version
 * @property createdAt When this version was created
 * @property comment Optional comment describing changes in this version
 * @property isLatest Whether this is the current/latest version
 * @property restoredFrom Version number this was restored from (null if not a restore)
 */
@Serializable
data class FileVersion(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val storageKey: String,
    val createdBy: String,
    val createdAt: Instant,
    val comment: String? = null,
    val isLatest: Boolean = true,
    val restoredFrom: Int? = null,
) {
    /**
     * Checks if this version is a restore operation.
     */
    val isRestore: Boolean get() = restoredFrom != null
}

/**
 * Version history for a file.
 *
 * @property item The storage item
 * @property versions List of all versions (newest first)
 * @property totalVersions Total number of versions
 * @property totalSize Total size of all versions combined
 */
@Serializable
data class FileVersionHistory(
    val item: StorageItem,
    val versions: List<FileVersion>,
    val totalVersions: Int,
    val totalSize: Long,
) {
    /**
     * Returns the latest version.
     */
    val latestVersion: FileVersion? get() = versions.firstOrNull { it.isLatest }

    /**
     * Returns the oldest version.
     */
    val oldestVersion: FileVersion? get() = versions.lastOrNull()
}

/**
 * Configuration for version retention policy.
 *
 * @property maxVersions Maximum number of versions to keep (null for unlimited)
 * @property maxAgeDays Maximum age of versions in days (null for unlimited)
 * @property minVersionsToKeep Minimum versions to keep regardless of age
 * @property excludePatterns File patterns to exclude from versioning
 */
@Serializable
data class VersionRetentionPolicy(
    val maxVersions: Int? = 10,
    val maxAgeDays: Int? = 90,
    val minVersionsToKeep: Int = 1,
    val excludePatterns: List<String> = emptyList(),
) {
    companion object {
        /**
         * Default retention policy.
         */
        val DEFAULT = VersionRetentionPolicy()

        /**
         * Keep all versions forever.
         */
        val KEEP_ALL = VersionRetentionPolicy(
            maxVersions = null,
            maxAgeDays = null,
            minVersionsToKeep = 1,
        )
    }
}

/**
 * Represents a diff between two file versions.
 *
 * @property fromVersion Source version number
 * @property toVersion Target version number
 * @property sizeChange Change in size (positive = larger, negative = smaller)
 * @property additions Number of lines/blocks added
 * @property deletions Number of lines/blocks deleted
 * @property isBinary Whether this is a binary file (no text diff available)
 * @property patches List of patch operations (for text files)
 */
@Serializable
data class VersionDiff(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int = 0,
    val deletions: Int = 0,
    val isBinary: Boolean = false,
    val patches: List<DiffPatch> = emptyList(),
)

/**
 * Represents a single patch in a diff.
 *
 * @property operation Type of operation (ADD, DELETE, MODIFY)
 * @property startLine Starting line number
 * @property endLine Ending line number
 * @property oldContent Original content (for DELETE/MODIFY)
 * @property newContent New content (for ADD/MODIFY)
 */
@Serializable
data class DiffPatch(
    val operation: DiffOperation,
    val startLine: Int,
    val endLine: Int,
    val oldContent: String? = null,
    val newContent: String? = null,
)

/**
 * Type of diff operation.
 */
@Serializable
enum class DiffOperation {
    ADD,
    DELETE,
    MODIFY,
    CONTEXT,
}
