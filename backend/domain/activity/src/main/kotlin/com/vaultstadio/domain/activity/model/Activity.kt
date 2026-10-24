package com.vaultstadio.domain.activity.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ActivityType {
    FILE_UPLOADED,
    FILE_DOWNLOADED,
    FILE_DELETED,
    FILE_MOVED,
    FILE_COPIED,
    FILE_RENAMED,
    FILE_RESTORED,
    FOLDER_CREATED,
    FOLDER_DELETED,
    FOLDER_MOVED,
    FOLDER_RENAMED,
    SHARE_CREATED,
    SHARE_ACCESSED,
    SHARE_DELETED,
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    PLUGIN_INSTALLED,
    PLUGIN_UNINSTALLED,
    PLUGIN_EXECUTED,
    SYSTEM_BACKUP,
    SYSTEM_RESTORE,
    SETTINGS_CHANGED,
}

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
