/**
 * VaultStadio Storage Domain Models
 *
 * Storage entities: item types, visibility, storage item, folder contents, quota.
 */

package com.vaultstadio.domain.storage.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents the type of a storage item.
 */
@Serializable
enum class ItemType {
    FILE,
    FOLDER,
}

/**
 * Represents the visibility/access level of a storage item.
 */
@Serializable
enum class Visibility {
    PRIVATE,
    SHARED,
    PUBLIC,
}

/**
 * Core storage item model representing both files and folders.
 */
@Serializable
data class StorageItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val type: ItemType,
    val parentId: String? = null,
    val ownerId: String,
    val size: Long = 0,
    val mimeType: String? = null,
    val checksum: String? = null,
    val storageKey: String? = null,
    val visibility: Visibility = Visibility.PRIVATE,
    val isTrashed: Boolean = false,
    val isStarred: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val trashedAt: Instant? = null,
    val version: Long = 1,
) {
    val extension: String?
        get() = if (type == ItemType.FILE) {
            name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        } else {
            null
        }

    val isRoot: Boolean
        get() = parentId == null

    val parentPath: String
        get() = path.substringBeforeLast('/', "")
}

/**
 * Represents a folder with its contents.
 */
@Serializable
data class FolderContents(
    val folder: StorageItem?,
    val children: List<StorageItem>,
    val totalSize: Long = 0,
    val itemCount: Int = 0,
)

/**
 * Represents storage quota information for a user.
 */
@Serializable
data class StorageQuota(
    val userId: String,
    val usedBytes: Long,
    val quotaBytes: Long? = null,
    val fileCount: Long,
    val folderCount: Long,
) {
    val usagePercentage: Double
        get() = quotaBytes?.let { usedBytes.toDouble() / it * 100 } ?: 0.0

    val isQuotaExceeded: Boolean
        get() = quotaBytes?.let { usedBytes >= it } ?: false

    val remainingBytes: Long?
        get() = quotaBytes?.let { (it - usedBytes).coerceAtLeast(0) }
}
