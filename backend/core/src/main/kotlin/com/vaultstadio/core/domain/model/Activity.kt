/**
 * VaultStadio Activity and Audit Models
 *
 * Models for tracking user activities and system events.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Types of activities that can be tracked.
 */
@Serializable
enum class ActivityType {
    // File operations
    FILE_UPLOADED,
    FILE_DOWNLOADED,
    FILE_DELETED,
    FILE_MOVED,
    FILE_COPIED,
    FILE_RENAMED,
    FILE_RESTORED,

    // Folder operations
    FOLDER_CREATED,
    FOLDER_DELETED,
    FOLDER_MOVED,
    FOLDER_RENAMED,

    // Sharing
    SHARE_CREATED,
    SHARE_ACCESSED,
    SHARE_DELETED,

    // User operations
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,

    // Plugin operations
    PLUGIN_INSTALLED,
    PLUGIN_UNINSTALLED,
    PLUGIN_EXECUTED,

    // System operations
    SYSTEM_BACKUP,
    SYSTEM_RESTORE,
    SETTINGS_CHANGED,
}

/**
 * Represents a user activity for audit logging.
 *
 * @property id Unique identifier
 * @property type Type of activity
 * @property userId User who performed the activity (null for system activities)
 * @property itemId Related storage item ID (if applicable)
 * @property itemPath Path of the related item at the time of activity
 * @property details Additional details as JSON
 * @property ipAddress IP address of the client
 * @property userAgent User agent of the client
 * @property createdAt When the activity occurred
 */
@Serializable
data class Activity(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val userId: String?,
    val itemId: String? = null,
    val itemPath: String? = null,
    val details: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: Instant,
)

/**
 * Represents a system event (for internal processing).
 *
 * @property id Unique identifier
 * @property type Event type
 * @property payload Event payload as JSON
 * @property createdAt When the event was created
 * @property processedAt When the event was processed (null if pending)
 * @property retryCount Number of processing retries
 * @property error Error message if processing failed
 */
@Serializable
data class SystemEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val payload: String,
    val createdAt: Instant,
    val processedAt: Instant? = null,
    val retryCount: Int = 0,
    val error: String? = null,
)

/**
 * Represents storage statistics.
 *
 * @property totalFiles Total number of files
 * @property totalFolders Total number of folders
 * @property totalSize Total size in bytes
 * @property totalUsers Total number of users
 * @property activeUsers Number of active users
 * @property totalShares Total number of active shares
 * @property uploadsToday Number of uploads today
 * @property downloadsToday Number of downloads today
 * @property collectedAt When these statistics were collected
 */
@Serializable
data class StorageStatistics(
    val totalFiles: Long,
    val totalFolders: Long,
    val totalSize: Long,
    val totalUsers: Long,
    val activeUsers: Long,
    val totalShares: Long,
    val uploadsToday: Long,
    val downloadsToday: Long,
    val collectedAt: Instant,
)
