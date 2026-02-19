/**
 * Restore Item Use Case
 *
 * Application use case for restoring an item from trash.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for restoring an item from trash.
 */
interface RestoreItemUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class RestoreItemUseCaseImpl(
    private val storageService: StorageService,
) : RestoreItemUseCase {

    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem> =
        storageService.restoreItem(itemId, userId)
}
