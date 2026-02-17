/**
 * Delete Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import org.koin.core.annotation.Factory

/**
 * Use case for permanently deleting an item.
 */
interface DeleteItemUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<Unit>
}

@Factory(binds = [DeleteItemUseCase::class])
class DeleteItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DeleteItemUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<Unit> =
        storageRepository.deleteItemPermanently(itemId)
}
