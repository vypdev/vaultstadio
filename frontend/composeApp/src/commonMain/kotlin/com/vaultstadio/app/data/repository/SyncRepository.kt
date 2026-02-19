/**
 * Sync Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.SyncService
import com.vaultstadio.app.domain.model.ConflictResolution
import com.vaultstadio.app.domain.model.DeviceType
import com.vaultstadio.app.domain.model.SyncConflict
import com.vaultstadio.app.domain.model.SyncDevice
import com.vaultstadio.app.domain.model.SyncResponse
import org.koin.core.annotation.Single

/**
 * Repository interface for sync operations.
 */
interface SyncRepository {
    suspend fun registerDevice(deviceId: String, deviceName: String, deviceType: DeviceType): ApiResult<SyncDevice>
    suspend fun getDevices(activeOnly: Boolean = true): ApiResult<List<SyncDevice>>
    suspend fun deactivateDevice(deviceId: String): ApiResult<Unit>
    suspend fun removeDevice(deviceId: String): ApiResult<Unit>
    suspend fun pullChanges(
        deviceId: String,
        cursor: String? = null,
        limit: Int = 1000,
        includeDeleted: Boolean = true,
    ): ApiResult<SyncResponse>
    suspend fun getConflicts(): ApiResult<List<SyncConflict>>
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): ApiResult<Unit>
}

@Single(binds = [SyncRepository::class])
class SyncRepositoryImpl(
    private val syncService: SyncService,
) : SyncRepository {

    override suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): ApiResult<SyncDevice> = syncService.registerDevice(deviceId, deviceName, deviceType)

    override suspend fun getDevices(activeOnly: Boolean): ApiResult<List<SyncDevice>> =
        syncService.getDevices(activeOnly)

    override suspend fun deactivateDevice(deviceId: String): ApiResult<Unit> =
        syncService.deactivateDevice(deviceId)

    override suspend fun removeDevice(deviceId: String): ApiResult<Unit> =
        syncService.removeDevice(deviceId)

    override suspend fun pullChanges(
        deviceId: String,
        cursor: String?,
        limit: Int,
        includeDeleted: Boolean,
    ): ApiResult<SyncResponse> = syncService.pullChanges(deviceId, cursor, limit, includeDeleted)

    override suspend fun getConflicts(): ApiResult<List<SyncConflict>> =
        syncService.getConflicts()

    override suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): ApiResult<Unit> =
        syncService.resolveConflict(conflictId, resolution)
}
