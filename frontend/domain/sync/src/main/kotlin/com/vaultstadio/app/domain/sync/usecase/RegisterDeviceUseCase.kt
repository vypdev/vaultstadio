/**
 * Use case for registering a device for sync.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncDevice

interface RegisterDeviceUseCase {
    suspend operator fun invoke(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice>
}
