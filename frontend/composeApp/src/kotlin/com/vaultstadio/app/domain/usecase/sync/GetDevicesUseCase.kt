/**
 * Get Devices Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.SyncDevice
/**
 * Use case for getting registered sync devices.
 */
interface GetDevicesUseCase {
    suspend operator fun invoke(activeOnly: Boolean = true): Result<List<SyncDevice>>
}

class GetDevicesUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetDevicesUseCase {

    override suspend operator fun invoke(activeOnly: Boolean): Result<List<SyncDevice>> =
        syncRepository.getDevices(activeOnly)
}
