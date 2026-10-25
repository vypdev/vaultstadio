/**
 * Get Pending Conflicts Use Case
 *
 * Application use case for listing pending sync conflicts for a user.
 */

package com.vaultstadio.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.domain.common.exception.StorageException

interface GetPendingConflictsUseCase {

    suspend operator fun invoke(userId: String): Either<StorageException, List<SyncConflict>>
}

class GetPendingConflictsUseCaseImpl(
    private val syncService: SyncService,
) : GetPendingConflictsUseCase {

    override suspend fun invoke(userId: String): Either<StorageException, List<SyncConflict>> =
        syncService.getPendingConflicts(userId)
}
