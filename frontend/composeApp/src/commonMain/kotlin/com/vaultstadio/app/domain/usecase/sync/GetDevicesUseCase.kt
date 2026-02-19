/**
 * Get Devices Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.SyncDevice
import org.koin.core.annotation.Factory

/**
 * Use case for getting registered sync devices.
 */
interface GetDevicesUseCase {
    suspend operator fun invoke(activeOnly: Boolean = true): ApiResult<List<SyncDevice>>
}

@Factory(binds = [GetDevicesUseCase::class])
class GetDevicesUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetDevicesUseCase {

    override suspend operator fun invoke(activeOnly: Boolean): ApiResult<List<SyncDevice>> =
        syncRepository.getDevices(activeOnly)
}
