/**
 * Get Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for getting a single storage item.
 */
interface GetItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}

@Factory(binds = [GetItemUseCase::class])
class GetItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetItemUseCase {

    override suspend operator fun invoke(itemId: String): Result<StorageItem> =
        storageRepository.getItem(itemId)
}
