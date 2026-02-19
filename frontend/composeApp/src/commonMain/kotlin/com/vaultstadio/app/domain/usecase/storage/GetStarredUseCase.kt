/**
 * Get Starred Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting starred items.
 */
interface GetStarredUseCase {
    suspend operator fun invoke(): ApiResult<List<StorageItem>>
}

@Factory(binds = [GetStarredUseCase::class])
class GetStarredUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetStarredUseCase {

    override suspend operator fun invoke(): ApiResult<List<StorageItem>> =
        storageRepository.getStarred()
}
