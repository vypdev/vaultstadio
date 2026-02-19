/**
 * Remove Device Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
/**
 * Use case for removing a sync device.
 */
interface RemoveDeviceUseCase {
    suspend operator fun invoke(deviceId: String): Result<Unit>
}

class RemoveDeviceUseCaseImpl(
    private val syncRepository: SyncRepository,
) : RemoveDeviceUseCase {

    override suspend operator fun invoke(deviceId: String): Result<Unit> =
        syncRepository.removeDevice(deviceId)
}
