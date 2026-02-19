/**
 * Copy Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
/**
 * Use case for copying an item.
 */
interface CopyItemUseCase {
    suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String? = null,
    ): Result<StorageItem>
}

class CopyItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CopyItemUseCase {

    override suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): Result<StorageItem> = storageRepository.copyItem(itemId, destinationId, newName)
}
