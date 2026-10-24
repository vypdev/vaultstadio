/**
 * Get conflicts use case implementation.
 */

package com.vaultstadio.app.data.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.model.SyncConflict
import com.vaultstadio.app.domain.sync.usecase.GetConflictsUseCase

class GetConflictsUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetConflictsUseCase {

    override suspend operator fun invoke(): Result<List<SyncConflict>> =
        syncRepository.getConflicts()
}
