/**
 * Register Device Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.DeviceType
import com.vaultstadio.app.domain.model.SyncDevice
import org.koin.core.annotation.Factory

/**
 * Use case for registering a device for sync.
 */
interface RegisterDeviceUseCase {
    suspend operator fun invoke(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice>
}

@Factory(binds = [RegisterDeviceUseCase::class])
class RegisterDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : RegisterDeviceUseCase {

    override suspend operator fun invoke(
        deviceId: String,
        deviceName: String,
        deviceType: DeviceType,
    ): Result<SyncDevice> = syncRepository.registerDevice(deviceId, deviceName, deviceType)
}
