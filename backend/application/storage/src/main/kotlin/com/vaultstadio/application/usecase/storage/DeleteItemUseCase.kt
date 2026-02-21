/**
 * Delete Item Use Case
 *
 * Application use case for permanently deleting an item.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException

/**
 * Use case for permanently deleting an item.
 */
interface DeleteItemUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, Unit>
}

/**
 * Default implementation delegating to [StorageService].
 */
class DeleteItemUseCaseImpl(
    private val storageService: StorageService,
) : DeleteItemUseCase {

    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, Unit> =
        storageService.deleteItem(itemId, userId)
}
