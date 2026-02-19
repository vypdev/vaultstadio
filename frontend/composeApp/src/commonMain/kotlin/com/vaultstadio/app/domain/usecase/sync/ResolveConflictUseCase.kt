/**
 * Resolve Conflict Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.ConflictResolution
import org.koin.core.annotation.Factory

/**
 * Use case for resolving a sync conflict.
 */
interface ResolveConflictUseCase {
    suspend operator fun invoke(conflictId: String, resolution: ConflictResolution): ApiResult<Unit>
}

@Factory(binds = [ResolveConflictUseCase::class])
class ResolveConflictUseCaseImpl(
    private val syncRepository: SyncRepository,
) : ResolveConflictUseCase {

    override suspend operator fun invoke(conflictId: String, resolution: ConflictResolution): ApiResult<Unit> =
        syncRepository.resolveConflict(conflictId, resolution)
}
