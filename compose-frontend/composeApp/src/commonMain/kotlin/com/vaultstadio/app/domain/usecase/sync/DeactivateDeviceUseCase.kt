/**
 * Deactivate Device Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.SyncRepository
import org.koin.core.annotation.Factory

/**
 * Use case for deactivating a sync device.
 */
interface DeactivateDeviceUseCase {
    suspend operator fun invoke(deviceId: String): ApiResult<Unit>
}

@Factory(binds = [DeactivateDeviceUseCase::class])
class DeactivateDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : DeactivateDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): ApiResult<Unit> =
        syncRepository.deactivateDevice(deviceId)
}
