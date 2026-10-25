/**
 * Remove device use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.usecase.RemoveDeviceUseCase

class RemoveDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : RemoveDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): Result<Unit> =
        syncRepository.removeDevice(deviceId)
}
