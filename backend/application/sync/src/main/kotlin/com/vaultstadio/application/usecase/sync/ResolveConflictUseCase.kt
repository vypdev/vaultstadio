/**
 * Resolve Conflict Use Case
 *
 * Application use case for resolving a sync conflict.
 */

package com.vaultstadio.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.domain.common.exception.StorageException

interface ResolveConflictUseCase {

    suspend operator fun invoke(
        conflictId: String,
        resolution: ConflictResolution,
        userId: String,
    ): Either<StorageException, SyncConflict>
}

class ResolveConflictUseCaseImpl(
    private val syncService: SyncService,
) : ResolveConflictUseCase {

    override suspend fun invoke(
        conflictId: String,
        resolution: ConflictResolution,
        userId: String,
    ): Either<StorageException, SyncConflict> =
        syncService.resolveConflict(conflictId, resolution, userId)
}
