/**
 * Move Item Use Case
 *
 * Application use case for moving a storage item.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.MoveItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for moving an item to a new parent.
 */
interface MoveItemUseCase {

    suspend operator fun invoke(input: MoveItemInput): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class MoveItemUseCaseImpl(
    private val storageService: StorageService,
) : MoveItemUseCase {

    override suspend fun invoke(input: MoveItemInput): Either<StorageException, StorageItem> =
        storageService.moveItem(input)
}
