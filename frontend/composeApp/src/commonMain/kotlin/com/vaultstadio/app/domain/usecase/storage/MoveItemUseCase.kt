/**
 * Move Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for moving an item.
 */
interface MoveItemUseCase {
    suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String? = null,
    ): ApiResult<StorageItem>
}

@Factory(binds = [MoveItemUseCase::class])
class MoveItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : MoveItemUseCase {

    override suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): ApiResult<StorageItem> = storageRepository.moveItem(itemId, destinationId, newName)
}
