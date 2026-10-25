/**
 * Remove Device Use Case
 *
 * Application use case for removing a sync device.
 */

package com.vaultstadio.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.domain.common.exception.StorageException

interface RemoveDeviceUseCase {

    suspend operator fun invoke(deviceId: String, userId: String): Either<StorageException, Unit>
}

class RemoveDeviceUseCaseImpl(
    private val syncService: SyncService,
) : RemoveDeviceUseCase {

    override suspend fun invoke(deviceId: String, userId: String): Either<StorageException, Unit> =
        syncService.removeDevice(deviceId, userId)
}
