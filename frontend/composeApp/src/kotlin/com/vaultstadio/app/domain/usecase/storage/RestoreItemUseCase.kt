/**
 * Restore Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
/**
 * Use case for restoring an item from trash.
 */
interface RestoreItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}

class RestoreItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RestoreItemUseCase {

    override suspend operator fun invoke(itemId: String): Result<StorageItem> =
        storageRepository.restoreItem(itemId)
}
