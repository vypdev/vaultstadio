/**
 * Get devices use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.SyncDevice
import com.vaultstadio.app.domain.sync.usecase.GetDevicesUseCase

class GetDevicesUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetDevicesUseCase {

    override suspend operator fun invoke(activeOnly: Boolean): Result<List<SyncDevice>> =
        syncRepository.getDevices(activeOnly)
}
