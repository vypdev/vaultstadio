/**
 * Get Trash Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting trashed items.
 */
interface GetTrashUseCase {
    suspend operator fun invoke(): Result<List<StorageItem>>
}

@Factory(binds = [GetTrashUseCase::class])
class GetTrashUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetTrashUseCase {

    override suspend operator fun invoke(): Result<List<StorageItem>> =
        storageRepository.getTrash()
}
