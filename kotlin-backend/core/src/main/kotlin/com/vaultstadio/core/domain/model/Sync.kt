/**
 * VaultStadio Sync Models
 *
 * Models for file synchronization between clients and server.
 * Supports delta sync, conflict detection, and resolution.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a sync state for a client device.
 *
 * @property id Unique identifier for this sync state
 * @property userId ID of the user
 * @property deviceId Unique identifier for the client device
 * @property deviceName Human-readable device name
 * @property deviceType Type of device (desktop, mobile, etc.)
 * @property lastSyncAt Last successful sync timestamp
 * @property lastSyncCursor Cursor/token for incremental sync
 * @property isActive Whether this device is actively syncing
 * @property createdAt When this device was registered
 * @property updatedAt When this sync state was last updated
 */
@Serializable
data class SyncDevice(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val lastSyncAt: Instant? = null,
    val lastSyncCursor: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Type of client device.
 */
@Serializable
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

/**
 * Represents a change to be synchronized.
 *
 * @property id Unique identifier for this change
 * @property itemId ID of the affected storage item
 * @property changeType Type of change
 * @property userId ID of the user who made the change
 * @property deviceId Device that made the change (null for server changes)
 * @property timestamp When the change occurred
 * @property cursor Monotonically increasing cursor for ordering
 * @property oldPath Previous path (for moves/renames)
 * @property newPath New path (for creates/moves/renames)
 * @property checksum File checksum (for content changes)
 * @property metadata Additional change metadata
 */
@Serializable
data class SyncChange(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val changeType: ChangeType,
    val userId: String,
    val deviceId: String? = null,
    val timestamp: Instant,
    val cursor: Long,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Type of sync change.
 */
@Serializable
enum class ChangeType {
    /** New file or folder created */
    CREATE,

    /** File content modified */
    MODIFY,

    /** Item renamed */
    RENAME,

    /** Item moved to different folder */
    MOVE,

    /** Item deleted */
    DELETE,

    /** Item restored from trash */
    RESTORE,

    /** Item moved to trash */
    TRASH,

    /** Item metadata changed */
    METADATA,
}

/**
 * Represents a sync conflict between local and remote changes.
 *
 * @property id Unique identifier for this conflict
 * @property itemId ID of the conflicting item
 * @property localChange The local change
 * @property remoteChange The remote change
 * @property conflictType Type of conflict
 * @property resolvedAt When the conflict was resolved (null if pending)
 * @property resolution How the conflict was resolved
 * @property createdAt When the conflict was detected
 */
@Serializable
data class SyncConflict(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val localChange: SyncChange,
    val remoteChange: SyncChange,
    val conflictType: ConflictType,
    val resolvedAt: Instant? = null,
    val resolution: ConflictResolution? = null,
    val createdAt: Instant,
) {
    val isPending: Boolean get() = resolvedAt == null
}

/**
 * Type of sync conflict.
 */
@Serializable
enum class ConflictType {
    /** Both sides modified the same file */
    EDIT_CONFLICT,

    /** File modified locally but deleted remotely */
    EDIT_DELETE,

    /** File deleted locally but modified remotely */
    DELETE_EDIT,

    /** Both sides created a file with the same name */
    CREATE_CREATE,

    /** Both sides moved the file to different locations */
    MOVE_MOVE,

    /** Parent folder was deleted */
    PARENT_DELETED,
}

/**
 * How a conflict was resolved.
 */
@Serializable
enum class ConflictResolution {
    /** Keep the local version */
    KEEP_LOCAL,

    /** Keep the remote version */
    KEEP_REMOTE,

    /** Keep both versions (rename one) */
    KEEP_BOTH,

    /** Merge changes */
    MERGE,

    /** User manually resolved */
    MANUAL,
}

/**
 * Request for syncing changes from a client.
 *
 * @property deviceId Client device identifier
 * @property cursor Last known sync cursor
 * @property limit Maximum number of changes to return
 * @property includeDeleted Include deleted items in response
 */
@Serializable
data class SyncRequest(
    val deviceId: String,
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)

/**
 * Response containing sync changes.
 *
 * @property changes List of changes since cursor
 * @property cursor New cursor for next sync
 * @property hasMore Whether there are more changes available
 * @property conflicts Any conflicts detected
 * @property serverTime Current server time
 */
@Serializable
data class SyncResponse(
    val changes: List<SyncChange>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflict> = emptyList(),
    val serverTime: Instant,
)

/**
 * Delta for a file block (for efficient large file sync).
 *
 * @property offset Offset in the file
 * @property length Length of the block
 * @property checksum Block checksum
 * @property data Block data (null if unchanged)
 */
@Serializable
data class BlockDelta(
    val offset: Long,
    val length: Int,
    val checksum: String,
    val data: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlockDelta) return false
        return offset == other.offset && length == other.length && checksum == other.checksum
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + length
        result = 31 * result + checksum.hashCode()
        return result
    }
}

/**
 * File signature for delta sync (rsync-like).
 *
 * @property itemId Storage item ID
 * @property versionNumber Version number
 * @property blockSize Size of each block
 * @property blocks List of block checksums
 */
@Serializable
data class FileSignature(
    val itemId: String,
    val versionNumber: Int,
    val blockSize: Int = 4096,
    val blocks: List<BlockChecksum>,
)

/**
 * Checksum for a file block.
 *
 * @property index Block index
 * @property weakChecksum Rolling checksum (Adler-32)
 * @property strongChecksum Strong checksum (MD5)
 */
@Serializable
data class BlockChecksum(
    val index: Int,
    val weakChecksum: Long,
    val strongChecksum: String,
)
