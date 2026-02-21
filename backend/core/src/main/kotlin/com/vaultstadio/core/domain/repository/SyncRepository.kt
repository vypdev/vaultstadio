/**
 * VaultStadio Sync Repository
 *
 * Interface for sync state and change tracking persistence.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for sync operations.
 */
interface SyncRepository {

    // ========================================================================
    // Device Management
    // ========================================================================

    /**
     * Register a new sync device.
     *
     * @param device The device to register
     * @return The registered device
     */
    suspend fun registerDevice(device: SyncDevice): Either<StorageException, SyncDevice>

    /**
     * Find a device by ID.
     *
     * @param deviceId Device identifier
     * @return The device or null
     */
    suspend fun findDevice(deviceId: String): Either<StorageException, SyncDevice?>

    /**
     * Find a device by user ID and device ID.
     *
     * @param userId User ID
     * @param deviceId Device identifier
     * @return The device or null
     */
    suspend fun findDeviceByUserAndId(
        userId: String,
        deviceId: String,
    ): Either<StorageException, SyncDevice?>

    /**
     * List all devices for a user.
     *
     * @param userId User ID
     * @param activeOnly Only return active devices
     * @return List of devices
     */
    suspend fun listDevices(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<SyncDevice>>

    /**
     * Update device sync state.
     *
     * @param device Updated device
     * @return The updated device
     */
    suspend fun updateDevice(device: SyncDevice): Either<StorageException, SyncDevice>

    /**
     * Deactivate a device.
     *
     * @param deviceId Device identifier
     * @return Unit on success
     */
    suspend fun deactivateDevice(deviceId: String): Either<StorageException, Unit>

    /**
     * Remove a device and its sync state.
     *
     * @param deviceId Device identifier
     * @return Unit on success
     */
    suspend fun removeDevice(deviceId: String): Either<StorageException, Unit>

    // ========================================================================
    // Change Tracking
    // ========================================================================

    /**
     * Record a new change.
     *
     * @param change The change to record
     * @return The recorded change with assigned cursor
     */
    suspend fun recordChange(change: SyncChange): Either<StorageException, SyncChange>

    /**
     * Get changes since a cursor for a user.
     *
     * @param userId User ID
     * @param cursor Cursor to start from (null for all changes)
     * @param limit Maximum number of changes to return
     * @return List of changes
     */
    suspend fun getChangesSince(
        userId: String,
        cursor: Long? = null,
        limit: Int = 1000,
    ): Either<StorageException, List<SyncChange>>

    /**
     * Get the current (highest) cursor for a user.
     *
     * @param userId User ID
     * @return Current cursor value
     */
    suspend fun getCurrentCursor(userId: String): Either<StorageException, Long>

    /**
     * Get changes for a specific item.
     *
     * @param itemId Storage item ID
     * @param limit Maximum number of changes
     * @return List of changes, newest first
     */
    suspend fun getChangesForItem(
        itemId: String,
        limit: Int = 100,
    ): Either<StorageException, List<SyncChange>>

    /**
     * Get changes by type.
     *
     * @param userId User ID
     * @param changeType Type of change
     * @param since Changes since this timestamp
     * @param limit Maximum number of changes
     * @return List of changes
     */
    suspend fun getChangesByType(
        userId: String,
        changeType: ChangeType,
        since: Instant? = null,
        limit: Int = 100,
    ): Either<StorageException, List<SyncChange>>

    /**
     * Stream changes in real-time.
     *
     * @param userId User ID
     * @param cursor Starting cursor
     * @return Flow of changes
     */
    fun streamChanges(userId: String, cursor: Long? = null): Flow<SyncChange>

    /**
     * Prune old changes.
     *
     * @param before Delete changes before this timestamp
     * @return Number of changes deleted
     */
    suspend fun pruneChanges(before: Instant): Either<StorageException, Int>

    // ========================================================================
    // Conflict Management
    // ========================================================================

    /**
     * Create a new conflict.
     *
     * @param conflict The conflict to create
     * @return The created conflict
     */
    suspend fun createConflict(conflict: SyncConflict): Either<StorageException, SyncConflict>

    /**
     * Find a conflict by ID.
     *
     * @param id Conflict ID
     * @return The conflict or null
     */
    suspend fun findConflict(id: String): Either<StorageException, SyncConflict?>

    /**
     * Get pending conflicts for a user.
     *
     * @param userId User ID
     * @return List of unresolved conflicts
     */
    suspend fun getPendingConflicts(userId: String): Either<StorageException, List<SyncConflict>>

    /**
     * Get conflicts for a specific item.
     *
     * @param itemId Storage item ID
     * @return List of conflicts for this item
     */
    suspend fun getConflictsForItem(itemId: String): Either<StorageException, List<SyncConflict>>

    /**
     * Resolve a conflict.
     *
     * @param conflictId Conflict ID
     * @param resolution How the conflict was resolved
     * @param resolvedAt When it was resolved
     * @return The updated conflict
     */
    suspend fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolution,
        resolvedAt: Instant,
    ): Either<StorageException, SyncConflict>

    /**
     * Delete old resolved conflicts.
     *
     * @param before Delete conflicts resolved before this timestamp
     * @return Number of conflicts deleted
     */
    suspend fun pruneResolvedConflicts(before: Instant): Either<StorageException, Int>
}
