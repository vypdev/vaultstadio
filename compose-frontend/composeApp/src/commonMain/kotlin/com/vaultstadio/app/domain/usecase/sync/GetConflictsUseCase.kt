/**
 * Get Conflicts Use Case
 */

package com.vaultstadio.app.domain.usecase.sync

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.SyncRepository
import com.vaultstadio.app.domain.model.SyncConflict
import org.koin.core.annotation.Factory

/**
 * Use case for getting sync conflicts.
 */
interface GetConflictsUseCase {
    suspend operator fun invoke(): ApiResult<List<SyncConflict>>
}

@Factory(binds = [GetConflictsUseCase::class])
class GetConflictsUseCaseImpl(
    private val syncRepository: SyncRepository,
) : GetConflictsUseCase {

    override suspend operator fun invoke(): ApiResult<List<SyncConflict>> =
        syncRepository.getConflicts()
}
