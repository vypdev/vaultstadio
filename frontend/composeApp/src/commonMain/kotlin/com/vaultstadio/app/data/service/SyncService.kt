/**
 * Sync Service
 */

package com.vaultstadio.app.data.service

import com.vaultstadio.app.data.api.SyncApi
import com.vaultstadio.app.data.dto.sync.RegisterDeviceRequestDTO
import com.vaultstadio.app.data.dto.sync.SyncRequestDTO
import com.vaultstadio.app.data.mapper.toConflictList
import com.vaultstadio.app.data.mapper.toDeviceList
import com.vaultstadio.app.data.mapper.toDomain
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.ConflictResolution
import com.vaultstadio.app.domain.model.DeviceType
import com.vaultstadio.app.domain.model.SyncConflict
import com.vaultstadio.app.domain.model.SyncDevice
import com.vaultstadio.app.domain.model.SyncResponse
import org.koin.core.annotation.Single

@Single
class SyncService(private val syncApi: SyncApi) {

    suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): ApiResult<SyncDevice> =
        syncApi.registerDevice(RegisterDeviceRequestDTO(deviceId, deviceName, deviceType.name))
            .map { it.toDomain() }

    suspend fun getDevices(activeOnly: Boolean = true): ApiResult<List<SyncDevice>> =
        syncApi.getDevices(activeOnly).map { it.toDeviceList() }

    suspend fun deactivateDevice(deviceId: String): ApiResult<Unit> =
        syncApi.deactivateDevice(deviceId).map { }

    suspend fun removeDevice(deviceId: String): ApiResult<Unit> =
        syncApi.removeDevice(deviceId)

    suspend fun pullChanges(
        deviceId: String,
        cursor: String? = null,
        limit: Int = 1000,
        includeDeleted: Boolean = true,
    ): ApiResult<SyncResponse> =
        syncApi.pullChanges(deviceId, SyncRequestDTO(cursor, limit, includeDeleted))
            .map { it.toDomain() }

    suspend fun getConflicts(): ApiResult<List<SyncConflict>> =
        syncApi.getConflicts().map { it.toConflictList() }

    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): ApiResult<Unit> =
        syncApi.resolveConflict(conflictId, resolution.name).map { }
}
