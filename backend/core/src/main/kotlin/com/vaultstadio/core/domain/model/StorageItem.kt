/**
 * VaultStadio Core Domain Models
 *
 * These models represent the core entities of the storage system.
 * They are storage-agnostic and plugin-neutral.
 */

package com.vaultstadio.core.domain.model

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
 *
 * This is the central entity of the storage system. It is intentionally
 * generic and does not contain any file-type-specific metadata.
 * Plugins can extend this with additional metadata through the
 * StorageItemMetadata entity.
 *
 * @property id Unique identifier for the item
 * @property name Display name of the item
 * @property path Full path in the virtual filesystem (e.g., "/documents/reports")
 * @property type Whether this is a file or folder
 * @property parentId ID of the parent folder (null for root items)
 * @property ownerId ID of the user who owns this item
 * @property size Size in bytes (0 for folders)
 * @property mimeType MIME type of the file (null for folders)
 * @property checksum SHA-256 checksum of the file content
 * @property storageKey Key used to locate the file in the storage backend
 * @property visibility Access level of the item
 * @property isTrashed Whether the item is in trash
 * @property isStarred Whether the item is starred/favorited
 * @property createdAt When the item was created
 * @property updatedAt When the item was last updated
 * @property trashedAt When the item was moved to trash (null if not trashed)
 * @property version Version number for optimistic locking
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
    /**
     * Returns the file extension, or null if not applicable.
     */
    val extension: String?
        get() = if (type == ItemType.FILE) {
            name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
        } else {
            null
        }

    /**
     * Checks if this item is at the root level.
     */
    val isRoot: Boolean
        get() = parentId == null

    /**
     * Returns the parent path.
     */
    val parentPath: String
        get() = path.substringBeforeLast('/', "")
}

/**
 * Represents a folder with its contents.
 *
 * @property folder The folder item
 * @property children List of items contained in this folder
 * @property totalSize Total size of all files in this folder (recursive)
 * @property itemCount Total number of items in this folder (recursive)
 */
@Serializable
data class FolderContents(
    val folder: StorageItem?,
    val children: List<StorageItem>,
    val totalSize: Long = 0,
    val itemCount: Int = 0,
)

/**
 * Represents extended metadata for a storage item.
 * This is used by plugins to attach additional metadata.
 *
 * @property id Unique identifier
 * @property itemId ID of the storage item this metadata belongs to
 * @property pluginId ID of the plugin that created this metadata
 * @property key Metadata key
 * @property value Metadata value (JSON)
 * @property createdAt When this metadata was created
 * @property updatedAt When this metadata was last updated
 */
@Serializable
data class StorageItemMetadata(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val pluginId: String,
    val key: String,
    val value: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Represents a share link for a storage item.
 *
 * @property id Unique identifier
 * @property itemId ID of the shared item
 * @property token Unique share token
 * @property createdBy ID of the user who created the share
 * @property expiresAt When this share expires (null for no expiration)
 * @property password Optional password for accessing the share
 * @property maxDownloads Maximum number of downloads allowed (null for unlimited)
 * @property downloadCount Current download count
 * @property isActive Whether the share is active
 * @property createdAt When the share was created
 */
@Serializable
data class ShareLink(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val token: String,
    val createdBy: String,
    val expiresAt: Instant? = null,
    val password: String? = null,
    val maxDownloads: Int? = null,
    val downloadCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val sharedWithUsers: List<String> = emptyList(),
) {
    /**
     * Checks if the share has expired.
     */
    fun isExpired(now: Instant): Boolean = expiresAt?.let { it < now } ?: false

    /**
     * Checks if download limit has been reached.
     */
    fun isDownloadLimitReached(): Boolean = maxDownloads?.let { downloadCount >= it } ?: false
}

/**
 * Represents storage quota information for a user.
 *
 * @property userId User ID
 * @property usedBytes Bytes currently used
 * @property quotaBytes Maximum allowed bytes (null for unlimited)
 * @property fileCount Number of files
 * @property folderCount Number of folders
 */
@Serializable
data class StorageQuota(
    val userId: String,
    val usedBytes: Long,
    val quotaBytes: Long? = null,
    val fileCount: Long,
    val folderCount: Long,
) {
    /**
     * Returns the usage percentage (0-100).
     */
    val usagePercentage: Double
        get() = quotaBytes?.let { (usedBytes.toDouble() / it) * 100 } ?: 0.0

    /**
     * Checks if quota is exceeded.
     */
    val isQuotaExceeded: Boolean
        get() = quotaBytes?.let { usedBytes >= it } ?: false

    /**
     * Returns remaining bytes.
     */
    val remainingBytes: Long?
        get() = quotaBytes?.let { (it - usedBytes).coerceAtLeast(0) }
}
