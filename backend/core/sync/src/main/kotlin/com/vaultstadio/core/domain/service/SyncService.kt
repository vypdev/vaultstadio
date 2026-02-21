/**
 * VaultStadio Sync Service
 *
 * Business logic for file synchronization operations.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.DeviceType
import com.vaultstadio.core.domain.model.FileSignature
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.model.SyncRequest
import com.vaultstadio.core.domain.model.SyncResponse
import com.vaultstadio.core.domain.repository.SyncRepository
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration.Companion.days

/**
 * Input for registering a sync device.
 *
 * @property deviceId Unique device identifier
 * @property deviceName Human-readable device name
 * @property deviceType Type of device
 */
data class RegisterDeviceInput(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
)

/**
 * Input for recording a change from a client.
 *
 * @property itemId Storage item ID
 * @property changeType Type of change
 * @property deviceId Device making the change
 * @property oldPath Previous path (for moves/renames)
 * @property newPath New path
 * @property checksum File checksum (for content changes)
 * @property metadata Additional metadata
 */
data class RecordChangeInput(
    val itemId: String,
    val changeType: ChangeType,
    val deviceId: String? = null,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Service for managing file synchronization.
 *
 * @property syncRepository Repository for sync persistence
 */
class SyncService(
    private val syncRepository: SyncRepository,
) {

    // ========================================================================
    // Device Management
    // ========================================================================

    /**
     * Register a new sync device for a user.
     *
     * @param input Device registration input
     * @param userId User ID
     * @return The registered device
     */
    suspend fun registerDevice(
        input: RegisterDeviceInput,
        userId: String,
    ): Either<StorageException, SyncDevice> {
        // Check if device already exists
        return syncRepository.findDeviceByUserAndId(userId, input.deviceId).flatMap { existing ->
            if (existing != null) {
                // Reactivate existing device
                syncRepository.updateDevice(
                    existing.copy(
                        isActive = true,
                        deviceName = input.deviceName,
                        updatedAt = Clock.System.now(),
                    ),
                )
            } else {
                // Create new device
                val now = Clock.System.now()
                val device = SyncDevice(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    deviceId = input.deviceId,
                    deviceName = input.deviceName,
                    deviceType = input.deviceType,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
                syncRepository.registerDevice(device)
            }
        }
    }

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
    ): Either<StorageException, List<SyncDevice>> {
        return syncRepository.listDevices(userId, activeOnly)
    }

    /**
     * Deactivate a device.
     *
     * @param deviceId Device ID
     * @param userId User ID (for authorization)
     * @return Unit on success
     */
    suspend fun deactivateDevice(
        deviceId: String,
        userId: String,
    ): Either<StorageException, Unit> {
        return syncRepository.findDeviceByUserAndId(userId, deviceId).flatMap { device ->
            when {
                device == null -> ItemNotFoundException(
                    itemId = deviceId,
                    message = "Device not found: $deviceId",
                ).left()
                device.userId != userId -> AuthorizationException(
                    message = "Not authorized to deactivate this device",
                ).left()
                else -> syncRepository.deactivateDevice(device.id)
            }
        }
    }

    /**
     * Remove a device and its sync state.
     *
     * @param deviceId Device ID
     * @param userId User ID (for authorization)
     * @return Unit on success
     */
    suspend fun removeDevice(
        deviceId: String,
        userId: String,
    ): Either<StorageException, Unit> {
        return syncRepository.findDeviceByUserAndId(userId, deviceId).flatMap { device ->
            when {
                device == null -> ItemNotFoundException(
                    itemId = deviceId,
                    message = "Device not found: $deviceId",
                ).left()
                device.userId != userId -> AuthorizationException(
                    message = "Not authorized to remove this device",
                ).left()
                else -> syncRepository.removeDevice(device.id)
            }
        }
    }

    // ========================================================================
    // Change Tracking
    // ========================================================================

    /**
     * Record a change from a file operation.
     *
     * @param input Change input
     * @param userId User ID
     * @return The recorded change
     */
    suspend fun recordChange(
        input: RecordChangeInput,
        userId: String,
    ): Either<StorageException, SyncChange> {
        return syncRepository.getCurrentCursor(userId).flatMap { currentCursor ->
            val change = SyncChange(
                id = UUID.randomUUID().toString(),
                itemId = input.itemId,
                changeType = input.changeType,
                userId = userId,
                deviceId = input.deviceId,
                timestamp = Clock.System.now(),
                cursor = currentCursor + 1,
                oldPath = input.oldPath,
                newPath = input.newPath,
                checksum = input.checksum,
                metadata = input.metadata,
            )
            syncRepository.recordChange(change)
        }
    }

    /**
     * Sync changes for a device.
     *
     * @param request Sync request
     * @param userId User ID
     * @return Sync response with changes
     */
    suspend fun sync(
        request: SyncRequest,
        userId: String,
    ): Either<StorageException, SyncResponse> {
        // Verify device belongs to user
        return syncRepository.findDeviceByUserAndId(userId, request.deviceId).flatMap { device ->
            when {
                device == null -> ItemNotFoundException(
                    itemId = request.deviceId,
                    message = "Device not found: ${request.deviceId}",
                ).left()
                !device.isActive -> InvalidOperationException(
                    operation = "sync",
                    message = "Device is not active",
                ).left()
                else -> performSync(device, request, userId)
            }
        }
    }

    private suspend fun performSync(
        device: SyncDevice,
        request: SyncRequest,
        userId: String,
    ): Either<StorageException, SyncResponse> {
        val cursor = request.cursor?.toLongOrNull() ?: 0L

        return syncRepository.getChangesSince(userId, cursor, request.limit).flatMap { changes ->
            syncRepository.getPendingConflicts(userId).flatMap { conflicts ->
                syncRepository.getCurrentCursor(userId).flatMap { currentCursor ->
                    // Update device sync state
                    syncRepository.updateDevice(
                        device.copy(
                            lastSyncAt = Clock.System.now(),
                            lastSyncCursor = currentCursor.toString(),
                            updatedAt = Clock.System.now(),
                        ),
                    ).map {
                        SyncResponse(
                            changes = changes,
                            cursor = currentCursor.toString(),
                            hasMore = changes.size >= request.limit,
                            conflicts = conflicts,
                            serverTime = Clock.System.now(),
                        )
                    }
                }
            }
        }
    }

    /**
     * Push changes from a client.
     *
     * @param changes List of changes from client
     * @param deviceId Device ID
     * @param userId User ID
     * @return List of conflicts (if any)
     */
    suspend fun pushChanges(
        changes: List<RecordChangeInput>,
        deviceId: String,
        userId: String,
    ): Either<StorageException, List<SyncConflict>> {
        val conflicts = mutableListOf<SyncConflict>()

        for (change in changes) {
            val result = detectConflict(change, userId)
            result.fold(
                { error -> return error.left() },
                { conflict ->
                    if (conflict != null) {
                        conflicts.add(conflict)
                    } else {
                        recordChange(change.copy(deviceId = deviceId), userId)
                    }
                },
            )
        }

        return conflicts.right()
    }

    private suspend fun detectConflict(
        change: RecordChangeInput,
        userId: String,
    ): Either<StorageException, SyncConflict?> {
        // Get recent changes for the same item
        return syncRepository.getChangesForItem(change.itemId, 1).flatMap { recentChanges ->
            if (recentChanges.isEmpty()) {
                null.right()
            } else {
                val lastChange = recentChanges.first()

                // Check for conflict
                val conflictType = when {
                    // Both modified the same file
                    change.changeType == ChangeType.MODIFY &&
                        lastChange.changeType == ChangeType.MODIFY -> ConflictType.EDIT_CONFLICT

                    // Local edit, remote delete
                    change.changeType == ChangeType.MODIFY &&
                        lastChange.changeType == ChangeType.DELETE -> ConflictType.EDIT_DELETE

                    // Local delete, remote edit
                    change.changeType == ChangeType.DELETE &&
                        lastChange.changeType == ChangeType.MODIFY -> ConflictType.DELETE_EDIT

                    else -> null
                }

                if (conflictType != null) {
                    val conflict = SyncConflict(
                        id = UUID.randomUUID().toString(),
                        itemId = change.itemId,
                        localChange = SyncChange(
                            id = UUID.randomUUID().toString(),
                            itemId = change.itemId,
                            changeType = change.changeType,
                            userId = userId,
                            deviceId = change.deviceId,
                            timestamp = Clock.System.now(),
                            cursor = 0,
                            oldPath = change.oldPath,
                            newPath = change.newPath,
                            checksum = change.checksum,
                        ),
                        remoteChange = lastChange,
                        conflictType = conflictType,
                        createdAt = Clock.System.now(),
                    )
                    syncRepository.createConflict(conflict).map { it }
                } else {
                    null.right()
                }
            }
        }
    }

    // ========================================================================
    // Conflict Resolution
    // ========================================================================

    /**
     * Get pending conflicts for a user.
     *
     * @param userId User ID
     * @return List of pending conflicts
     */
    suspend fun getPendingConflicts(userId: String): Either<StorageException, List<SyncConflict>> {
        return syncRepository.getPendingConflicts(userId)
    }

    /**
     * Resolve a conflict.
     *
     * @param conflictId Conflict ID
     * @param resolution How to resolve
     * @param userId User ID
     * @return The resolved conflict
     */
    suspend fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolution,
        userId: String,
    ): Either<StorageException, SyncConflict> {
        return syncRepository.findConflict(conflictId).flatMap { conflict ->
            when {
                conflict == null -> ItemNotFoundException(
                    itemId = conflictId,
                    message = "Conflict not found: $conflictId",
                ).left()
                !conflict.isPending -> InvalidOperationException(
                    operation = "resolveConflict",
                    message = "Conflict is already resolved",
                ).left()
                else -> syncRepository.resolveConflict(
                    conflictId,
                    resolution,
                    Clock.System.now(),
                )
            }
        }
    }

    // ========================================================================
    // Delta Sync
    // ========================================================================

    /**
     * Generate a file signature for delta sync.
     *
     * @param itemId Storage item ID
     * @param versionNumber Version number
     * @param blockSize Block size for chunking
     * @return File signature
     */
    suspend fun generateFileSignature(
        itemId: String,
        versionNumber: Int,
        blockSize: Int = 4096,
    ): Either<StorageException, FileSignature> {
        // In production, this would read the file and generate block checksums
        return FileSignature(
            itemId = itemId,
            versionNumber = versionNumber,
            blockSize = blockSize,
            blocks = emptyList(), // Would be populated with block checksums
        ).right()
    }

    // ========================================================================
    // Maintenance
    // ========================================================================

    /**
     * Prune old sync data.
     *
     * @param olderThanDays Delete data older than this many days
     * @return Number of items pruned
     */
    suspend fun pruneOldData(olderThanDays: Int = 30): Either<StorageException, Int> {
        val threshold = Clock.System.now().minus(olderThanDays.days)

        return syncRepository.pruneChanges(threshold).flatMap { changesDeleted ->
            syncRepository.pruneResolvedConflicts(threshold).map { conflictsDeleted ->
                changesDeleted + conflictsDeleted
            }
        }
    }
}
