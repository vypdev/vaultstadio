/**
 * Register Device Use Case
 *
 * Application use case for registering a sync device.
 */

package com.vaultstadio.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncDevice
import com.vaultstadio.core.domain.service.RegisterDeviceInput
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.domain.common.exception.StorageException

interface RegisterDeviceUseCase {

    suspend operator fun invoke(input: RegisterDeviceInput, userId: String): Either<StorageException, SyncDevice>
}

class RegisterDeviceUseCaseImpl(
    private val syncService: SyncService,
) : RegisterDeviceUseCase {

    override suspend fun invoke(input: RegisterDeviceInput, userId: String): Either<StorageException, SyncDevice> =
        syncService.registerDevice(input, userId)
}
