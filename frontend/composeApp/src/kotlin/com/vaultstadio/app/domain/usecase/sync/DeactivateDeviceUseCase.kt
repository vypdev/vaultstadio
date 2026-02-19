/**
 * Deactivate Device Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
/**
 * Use case for deactivating a sync device.
 */
interface DeactivateDeviceUseCase {
    suspend operator fun invoke(deviceId: String): Result<Unit>
}

class DeactivateDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : DeactivateDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): Result<Unit> =
        syncRepository.deactivateDevice(deviceId)
}
