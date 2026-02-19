/**
 * Sync Pull Use Case
 *
 * Application use case for pulling sync changes (client requests changes since cursor).
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncRequest
import com.vaultstadio.core.domain.model.SyncResponse
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.StorageException

interface SyncPullUseCase {

    suspend operator fun invoke(request: SyncRequest, userId: String): Either<StorageException, SyncResponse>
}

class SyncPullUseCaseImpl(
    private val syncService: SyncService,
) : SyncPullUseCase {

    override suspend fun invoke(request: SyncRequest, userId: String): Either<StorageException, SyncResponse> =
        syncService.sync(request, userId)
}
