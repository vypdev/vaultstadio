/**
 * Remove Device Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.SyncRepository
import org.koin.core.annotation.Factory

/**
 * Use case for removing a sync device.
 */
interface RemoveDeviceUseCase {
    suspend operator fun invoke(deviceId: String): ApiResult<Unit>
}

@Factory(binds = [RemoveDeviceUseCase::class])
class RemoveDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : RemoveDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): ApiResult<Unit> =
        syncRepository.removeDevice(deviceId)
}
