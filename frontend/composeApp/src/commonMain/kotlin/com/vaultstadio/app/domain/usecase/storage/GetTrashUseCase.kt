/**
 * Get Trash Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting trashed items.
 */
interface GetTrashUseCase {
    suspend operator fun invoke(): ApiResult<List<StorageItem>>
}

@Factory(binds = [GetTrashUseCase::class])
class GetTrashUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetTrashUseCase {

    override suspend operator fun invoke(): ApiResult<List<StorageItem>> =
        storageRepository.getTrash()
}
