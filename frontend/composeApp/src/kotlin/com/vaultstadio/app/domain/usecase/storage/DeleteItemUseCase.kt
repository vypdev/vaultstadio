/**
 * Delete Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
/**
 * Use case for permanently deleting an item.
 */
interface DeleteItemUseCase {
    suspend operator fun invoke(itemId: String): Result<Unit>
}

class DeleteItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : DeleteItemUseCase {

    override suspend operator fun invoke(itemId: String): Result<Unit> =
        storageRepository.deleteItemPermanently(itemId)
}
