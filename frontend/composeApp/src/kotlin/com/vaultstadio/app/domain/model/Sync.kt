/**
 * Sync Domain Models
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

enum class DeviceType {
    DESKTOP_WINDOWS,
    DESKTOP_MAC,
    DESKTOP_LINUX,
    MOBILE_ANDROID,
    MOBILE_IOS,
    WEB,
    CLI,
    OTHER,
}

data class SyncDevice(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val lastSyncAt: Instant? = null,
    val isActive: Boolean,
    val createdAt: Instant,
)

enum class ChangeType {
    CREATE,
    MODIFY,
    RENAME,
    MOVE,
    DELETE,
    RESTORE,
    TRASH,
    METADATA,
}

data class SyncChange(
    val id: String,
    val itemId: String,
    val changeType: ChangeType,
    val timestamp: Instant,
    val cursor: Long,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
)

enum class ConflictType {
    EDIT_CONFLICT,
    EDIT_DELETE,
    DELETE_EDIT,
    CREATE_CREATE,
    MOVE_MOVE,
    PARENT_DELETED,
}

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    KEEP_BOTH,
    MERGE,
    MANUAL,
}

data class SyncConflict(
    val id: String,
    val itemId: String,
    val conflictType: ConflictType,
    val localChange: SyncChange,
    val remoteChange: SyncChange,
    val createdAt: Instant,
    val isPending: Boolean,
)

data class SyncResponse(
    val changes: List<SyncChange>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflict>,
    val serverTime: Instant,
)

/**
 * Request for syncing changes (pagination and options).
 */
data class SyncRequest(
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)
