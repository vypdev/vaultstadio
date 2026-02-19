/**
 * Rename Item Use Case
 *
 * Application use case for renaming a storage item.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for renaming an item.
 */
interface RenameItemUseCase {

    suspend operator fun invoke(
        itemId: String,
        newName: String,
        userId: String,
    ): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class RenameItemUseCaseImpl(
    private val storageService: StorageService,
) : RenameItemUseCase {

    override suspend fun invoke(
        itemId: String,
        newName: String,
        userId: String,
    ): Either<StorageException, StorageItem> =
        storageService.renameItem(itemId, newName, userId)
}
