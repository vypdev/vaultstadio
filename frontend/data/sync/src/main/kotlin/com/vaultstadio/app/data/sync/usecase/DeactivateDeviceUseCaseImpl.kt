/**
 * Deactivate device use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.usecase.DeactivateDeviceUseCase

class DeactivateDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : DeactivateDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): Result<Unit> =
        syncRepository.deactivateDevice(deviceId)
}
