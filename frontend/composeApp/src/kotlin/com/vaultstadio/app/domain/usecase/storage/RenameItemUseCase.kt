/**
 * Rename Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
/**
 * Use case for renaming an item.
 */
interface RenameItemUseCase {
    suspend operator fun invoke(itemId: String, newName: String): Result<StorageItem>
}

class RenameItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RenameItemUseCase {

    override suspend operator fun invoke(itemId: String, newName: String): Result<StorageItem> =
        storageRepository.renameItem(itemId, newName)
}
