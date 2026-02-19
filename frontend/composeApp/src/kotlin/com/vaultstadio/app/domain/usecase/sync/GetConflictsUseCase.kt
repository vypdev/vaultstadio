/**
 * Get Conflicts Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.SyncConflict
/**
 * Use case for getting sync conflicts.
 */
interface GetConflictsUseCase {
    suspend operator fun invoke(): Result<List<SyncConflict>>
}

class GetConflictsUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetConflictsUseCase {

    override suspend operator fun invoke(): Result<List<SyncConflict>> =
        syncRepository.getConflicts()
}
