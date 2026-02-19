/**
 * Deactivate Device Use Case
 *
 * Application use case for deactivating a sync device.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.StorageException

interface DeactivateDeviceUseCase {

    suspend operator fun invoke(deviceId: String, userId: String): Either<StorageException, Unit>
}

class DeactivateDeviceUseCaseImpl(
    private val syncService: SyncService,
) : DeactivateDeviceUseCase {

    override suspend fun invoke(deviceId: String, userId: String): Either<StorageException, Unit> =
        syncService.deactivateDevice(deviceId, userId)
}
