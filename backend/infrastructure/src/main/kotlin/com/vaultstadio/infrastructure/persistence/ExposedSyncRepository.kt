/**
 * VaultStadio Exposed Sync Repository Implementation
 */

package com.vaultstadio.infrastructure.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.repository.SyncRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.infrastructure.persistence.entities.SyncChangesTable
import com.vaultstadio.infrastructure.persistence.entities.SyncConflictsTable
import com.vaultstadio.infrastructure.persistence.entities.SyncDevicesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Exposed implementation of SyncRepository.
 */
class ExposedSyncRepository : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    // ========================================================================
    // Device Management
    // ========================================================================

    override suspend fun registerDevice(device: SyncDevice): Either<StorageException, SyncDevice> {
        return try {
            dbQuery {
                SyncDevicesTable.insert {
                    it[id] = device.id
                    it[userId] = device.userId
                    it[deviceId] = device.deviceId
                    it[deviceName] = device.deviceName
                    it[deviceType] = device.deviceType.name
                    it[lastSyncAt] = device.lastSyncAt
                    it[lastSyncCursor] = device.lastSyncCursor
                    it[isActive] = device.isActive
                    it[createdAt] = device.createdAt
                    it[updatedAt] = device.updatedAt
                }
            }
            device.right()
        } catch (e: Exception) {
            DatabaseException("Failed to register device: ${e.message}", e).left()
        }
    }

    override suspend fun findDevice(deviceId: String): Either<StorageException, SyncDevice?> {
        return try {
            dbQuery {
                SyncDevicesTable.selectAll()
                    .where { SyncDevicesTable.id eq deviceId }
                    .map { it.toSyncDevice() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find device: ${e.message}", e).left()
        }
    }

    override suspend fun findDeviceByUserAndId(
        userId: String,
        deviceId: String,
    ): Either<StorageException, SyncDevice?> {
        return try {
            dbQuery {
                SyncDevicesTable.selectAll()
                    .where {
                        (SyncDevicesTable.userId eq userId) and
                            (SyncDevicesTable.deviceId eq deviceId)
                    }
                    .map { it.toSyncDevice() }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find device: ${e.message}", e).left()
        }
    }

    override suspend fun listDevices(
        userId: String,
        activeOnly: Boolean,
    ): Either<StorageException, List<SyncDevice>> {
        return try {
            dbQuery {
                val query = SyncDevicesTable.selectAll()
                    .where { SyncDevicesTable.userId eq userId }

                if (activeOnly) {
                    query.andWhere { SyncDevicesTable.isActive eq true }
                }

                query.map { it.toSyncDevice() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to list devices: ${e.message}", e).left()
        }
    }

    override suspend fun updateDevice(device: SyncDevice): Either<StorageException, SyncDevice> {
        return try {
            dbQuery {
                SyncDevicesTable.update({ SyncDevicesTable.id eq device.id }) {
                    it[deviceName] = device.deviceName
                    it[lastSyncAt] = device.lastSyncAt
                    it[lastSyncCursor] = device.lastSyncCursor
                    it[isActive] = device.isActive
                    it[updatedAt] = device.updatedAt
                }
            }
            device.right()
        } catch (e: Exception) {
            DatabaseException("Failed to update device: ${e.message}", e).left()
        }
    }

    override suspend fun deactivateDevice(deviceId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                SyncDevicesTable.update({ SyncDevicesTable.id eq deviceId }) {
                    it[isActive] = false
                    it[updatedAt] = Clock.System.now()
                }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to deactivate device: ${e.message}", e).left()
        }
    }

    override suspend fun removeDevice(deviceId: String): Either<StorageException, Unit> {
        return try {
            dbQuery {
                SyncDevicesTable.deleteWhere { SyncDevicesTable.id eq deviceId }
            }
            Unit.right()
        } catch (e: Exception) {
            DatabaseException("Failed to remove device: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Change Tracking
    // ========================================================================

    override suspend fun recordChange(change: SyncChange): Either<StorageException, SyncChange> {
        return try {
            dbQuery {
                SyncChangesTable.insert {
                    it[id] = change.id
                    it[itemId] = change.itemId
                    it[changeType] = change.changeType.name
                    it[userId] = change.userId
                    it[deviceId] = change.deviceId
                    it[timestamp] = change.timestamp
                    it[cursor] = change.cursor
                    it[oldPath] = change.oldPath
                    it[newPath] = change.newPath
                    it[checksum] = change.checksum
                    it[metadata] = if (change.metadata.isNotEmpty()) {
                        json.encodeToString(change.metadata)
                    } else {
                        null
                    }
                }
            }
            change.right()
        } catch (e: Exception) {
            DatabaseException("Failed to record change: ${e.message}", e).left()
        }
    }

    override suspend fun getChangesSince(
        userId: String,
        cursor: Long?,
        limit: Int,
    ): Either<StorageException, List<SyncChange>> {
        return try {
            dbQuery {
                val query = SyncChangesTable.selectAll()
                    .where { SyncChangesTable.userId eq userId }

                if (cursor != null) {
                    query.andWhere { SyncChangesTable.cursor greater cursor }
                }

                query.orderBy(SyncChangesTable.cursor, SortOrder.ASC)
                    .limit(limit)
                    .map { it.toSyncChange() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get changes: ${e.message}", e).left()
        }
    }

    override suspend fun getCurrentCursor(userId: String): Either<StorageException, Long> {
        return try {
            dbQuery {
                SyncChangesTable
                    .select(SyncChangesTable.cursor.max())
                    .where { SyncChangesTable.userId eq userId }
                    .singleOrNull()
                    ?.get(SyncChangesTable.cursor.max()) ?: 0L
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get current cursor: ${e.message}", e).left()
        }
    }

    override suspend fun getChangesForItem(
        itemId: String,
        limit: Int,
    ): Either<StorageException, List<SyncChange>> {
        return try {
            dbQuery {
                SyncChangesTable.selectAll()
                    .where { SyncChangesTable.itemId eq itemId }
                    .orderBy(SyncChangesTable.cursor, SortOrder.DESC)
                    .limit(limit)
                    .map { it.toSyncChange() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get changes for item: ${e.message}", e).left()
        }
    }

    override suspend fun getChangesByType(
        userId: String,
        changeType: ChangeType,
        since: Instant?,
        limit: Int,
    ): Either<StorageException, List<SyncChange>> {
        return try {
            dbQuery {
                val query = SyncChangesTable.selectAll()
                    .where {
                        (SyncChangesTable.userId eq userId) and
                            (SyncChangesTable.changeType eq changeType.name)
                    }

                if (since != null) {
                    query.andWhere { SyncChangesTable.timestamp greater since }
                }

                query.orderBy(SyncChangesTable.timestamp, SortOrder.DESC)
                    .limit(limit)
                    .map { it.toSyncChange() }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get changes by type: ${e.message}", e).left()
        }
    }

    override fun streamChanges(userId: String, cursor: Long?): Flow<SyncChange> = flow {
        val changes = dbQuery {
            val query = SyncChangesTable.selectAll()
                .where { SyncChangesTable.userId eq userId }

            if (cursor != null) {
                query.andWhere { SyncChangesTable.cursor greater cursor }
            }

            query.orderBy(SyncChangesTable.cursor, SortOrder.ASC)
                .map { it.toSyncChange() }
        }
        changes.forEach { emit(it) }
    }

    override suspend fun pruneChanges(before: Instant): Either<StorageException, Int> {
        return try {
            dbQuery {
                SyncChangesTable.deleteWhere { timestamp less before }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to prune changes: ${e.message}", e).left()
        }
    }

    // ========================================================================
    // Conflict Management
    // ========================================================================

    override suspend fun createConflict(conflict: SyncConflict): Either<StorageException, SyncConflict> {
        return try {
            dbQuery {
                SyncConflictsTable.insert {
                    it[id] = conflict.id
                    it[itemId] = conflict.itemId
                    it[localChangeId] = conflict.localChange.id
                    it[remoteChangeId] = conflict.remoteChange.id
                    it[conflictType] = conflict.conflictType.name
                    it[createdAt] = conflict.createdAt
                }
            }
            conflict.right()
        } catch (e: Exception) {
            DatabaseException("Failed to create conflict: ${e.message}", e).left()
        }
    }

    override suspend fun findConflict(id: String): Either<StorageException, SyncConflict?> {
        return try {
            dbQuery {
                SyncConflictsTable.selectAll()
                    .where { SyncConflictsTable.id eq id }
                    .map { row ->
                        val localChange = getChangeById(row[SyncConflictsTable.localChangeId])
                        val remoteChange = getChangeById(row[SyncConflictsTable.remoteChangeId])
                        row.toSyncConflict(localChange, remoteChange)
                    }
                    .singleOrNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to find conflict: ${e.message}", e).left()
        }
    }

    private fun getChangeById(id: String): SyncChange? {
        return SyncChangesTable.selectAll()
            .where { SyncChangesTable.id eq id }
            .map { it.toSyncChange() }
            .singleOrNull()
    }

    override suspend fun getPendingConflicts(userId: String): Either<StorageException, List<SyncConflict>> {
        return try {
            dbQuery {
                // Get conflicts for items owned by user that are not resolved
                SyncConflictsTable.selectAll()
                    .where { SyncConflictsTable.resolvedAt.isNull() }
                    .map { row ->
                        val localChange = getChangeById(row[SyncConflictsTable.localChangeId])
                        val remoteChange = getChangeById(row[SyncConflictsTable.remoteChangeId])
                        if (localChange?.userId == userId || remoteChange?.userId == userId) {
                            row.toSyncConflict(localChange, remoteChange)
                        } else {
                            null
                        }
                    }
                    .filterNotNull()
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get pending conflicts: ${e.message}", e).left()
        }
    }

    override suspend fun getConflictsForItem(itemId: String): Either<StorageException, List<SyncConflict>> {
        return try {
            dbQuery {
                SyncConflictsTable.selectAll()
                    .where { SyncConflictsTable.itemId eq itemId }
                    .map { row ->
                        val localChange = getChangeById(row[SyncConflictsTable.localChangeId])
                        val remoteChange = getChangeById(row[SyncConflictsTable.remoteChangeId])
                        row.toSyncConflict(localChange, remoteChange)
                    }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to get conflicts for item: ${e.message}", e).left()
        }
    }

    override suspend fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolution,
        resolvedAt: Instant,
    ): Either<StorageException, SyncConflict> {
        return try {
            dbQuery {
                SyncConflictsTable.update({ SyncConflictsTable.id eq conflictId }) {
                    it[SyncConflictsTable.resolvedAt] = resolvedAt
                    it[SyncConflictsTable.resolution] = resolution.name
                }
            }
            findConflict(conflictId).fold(
                { it.left() },
                { conflict -> conflict?.right() ?: DatabaseException("Conflict not found after update").left() },
            )
        } catch (e: Exception) {
            DatabaseException("Failed to resolve conflict: ${e.message}", e).left()
        }
    }

    override suspend fun pruneResolvedConflicts(before: Instant): Either<StorageException, Int> {
        return try {
            dbQuery {
                SyncConflictsTable.deleteWhere {
                    SyncConflictsTable.resolvedAt.isNotNull() and (SyncConflictsTable.resolvedAt less before)
                }
            }.right()
        } catch (e: Exception) {
            DatabaseException("Failed to prune resolved conflicts: ${e.message}", e).left()
        }
    }
}
