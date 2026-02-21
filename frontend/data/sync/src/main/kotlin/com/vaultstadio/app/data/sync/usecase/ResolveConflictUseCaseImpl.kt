/**
 * Resolve conflict use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.ConflictResolution
import com.vaultstadio.app.domain.sync.usecase.ResolveConflictUseCase

class ResolveConflictUseCaseImpl(
    private val syncRepository: SyncRepository,
) : ResolveConflictUseCase {

    override suspend operator fun invoke(conflictId: String, resolution: ConflictResolution): Result<Unit> =
        syncRepository.resolveConflict(conflictId, resolution)
}
