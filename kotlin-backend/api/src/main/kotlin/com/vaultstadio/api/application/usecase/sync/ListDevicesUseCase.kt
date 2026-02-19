/**
 * List Devices Use Case
 *
 * Application use case for listing sync devices for a user.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.StorageException

interface ListDevicesUseCase {

    suspend operator fun invoke(userId: String, activeOnly: Boolean = true): Either<StorageException, List<SyncDevice>>
}

class ListDevicesUseCaseImpl(
    private val syncService: SyncService,
) : ListDevicesUseCase {

    override suspend fun invoke(userId: String, activeOnly: Boolean): Either<StorageException, List<SyncDevice>> =
        syncService.listDevices(userId, activeOnly)
}
