/**
 * Record Change Use Case
 *
 * Application use case for recording a sync change from a client (push / delta).
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.service.RecordChangeInput
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.StorageException

interface RecordChangeUseCase {

    suspend operator fun invoke(input: RecordChangeInput, userId: String): Either<StorageException, SyncChange>
}

class RecordChangeUseCaseImpl(
    private val syncService: SyncService,
) : RecordChangeUseCase {

    override suspend fun invoke(input: RecordChangeInput, userId: String): Either<StorageException, SyncChange> =
        syncService.recordChange(input, userId)
}
