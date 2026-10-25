/**
 * Register device use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.DeviceType
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.usecase.RegisterDeviceUseCase

class RegisterDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : RegisterDeviceUseCase {

    override suspend operator fun invoke(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice> = syncRepository.registerDevice(deviceId, deviceName, deviceType)
}
