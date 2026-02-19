/**
 * Get Starred Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting starred items.
 */
interface GetStarredUseCase {
    suspend operator fun invoke(): Result<List<StorageItem>>
}

@Factory(binds = [GetStarredUseCase::class])
class GetStarredUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetStarredUseCase {

    override suspend operator fun invoke(): Result<List<StorageItem>> =
        storageRepository.getStarred()
}
