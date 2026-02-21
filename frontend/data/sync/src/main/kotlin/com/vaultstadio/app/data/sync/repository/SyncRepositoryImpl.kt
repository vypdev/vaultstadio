/**
 * Sync Repository implementation
 */

package com.vaultstadio.app.data.sync.repository

import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.sync.service.SyncService
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse

class SyncRepositoryImpl(
    private val syncService: SyncService,
) : SyncRepository {

    override suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ) = syncService.registerDevice(deviceId, deviceName, deviceType).toResult()

    override suspend fun getDevices(activeOnly: Boolean) =
        syncService.getDevices(activeOnly).toResult()

    override suspend fun deactivateDevice(deviceId: String) =
        syncService.deactivateDevice(deviceId).toResult()

    override suspend fun removeDevice(deviceId: String) =
        syncService.removeDevice(deviceId).toResult()

    override suspend fun pullChanges(
        deviceId: String,
        cursor: String?,
        limit: Int,
        includeDeleted: Boolean,
    ) = syncService.pullChanges(deviceId, cursor, limit, includeDeleted).toResult()

    override suspend fun getConflicts() =
        syncService.getConflicts().toResult()

    override suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution) =
        syncService.resolveConflict(conflictId, resolution).toResult()
}
