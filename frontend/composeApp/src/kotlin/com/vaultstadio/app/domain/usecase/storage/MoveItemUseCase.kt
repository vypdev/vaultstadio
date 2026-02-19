/**
 * Move Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
/**
 * Use case for moving an item.
 */
interface MoveItemUseCase {
    suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String? = null,
    ): Result<StorageItem>
}

class MoveItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : MoveItemUseCase {

    override suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = storageRepository.moveItem(itemId, destinationId, newName)
}
