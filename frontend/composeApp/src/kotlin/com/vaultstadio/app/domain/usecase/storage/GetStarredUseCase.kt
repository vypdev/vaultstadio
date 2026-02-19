/**
 * Get Starred Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
/**
 * Use case for getting starred items.
 */
interface GetStarredUseCase {
    suspend operator fun invoke(): Result<List<StorageItem>>
}

class GetStarredUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetStarredUseCase {

    override suspend operator fun invoke(): Result<List<StorageItem>> =
        storageRepository.getStarred()
}
