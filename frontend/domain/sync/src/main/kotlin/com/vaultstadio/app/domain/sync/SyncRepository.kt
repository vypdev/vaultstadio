/**
 * Repository interface for sync operations.
 */

package com.vaultstadio.app.domain.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.model.SyncResponse

interface SyncRepository {
    suspend fun registerDevice(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice>
    suspend fun getDevices(activeOnly: Boolean = true): Result<List<SyncDevice>>
    suspend fun deactivateDevice(deviceId: String): Result<Unit>
    suspend fun removeDevice(deviceId: String): Result<Unit>
    suspend fun pullChanges(
        deviceId: String,
        cursor: String? = null,
        limit: Int = 1000,
        includeDeleted: Boolean = true,
    ): Result<SyncResponse>
    suspend fun getConflicts(): Result<List<SyncConflict>>
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): Result<Unit>
}
