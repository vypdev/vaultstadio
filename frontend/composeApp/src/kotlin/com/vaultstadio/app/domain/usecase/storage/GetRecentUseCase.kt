/**
 * Get Recent Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting recent items.
 */
interface GetRecentUseCase {
    suspend operator fun invoke(limit: Int = 20): Result<List<StorageItem>>
}

@Factory(binds = [GetRecentUseCase::class])
class GetRecentUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetRecentUseCase {

    override suspend operator fun invoke(limit: Int): Result<List<StorageItem>> =
        storageRepository.getRecent(limit)
}
