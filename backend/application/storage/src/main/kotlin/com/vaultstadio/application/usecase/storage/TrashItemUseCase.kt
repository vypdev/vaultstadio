/**
 * Trash Item Use Case
 *
 * Application use case for moving an item to trash.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for moving an item to trash.
 */
interface TrashItemUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class TrashItemUseCaseImpl(
    private val storageService: StorageService,
) : TrashItemUseCase {

    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem> =
        storageService.trashItem(itemId, userId)
}
