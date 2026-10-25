/**
 * Get Item Use Case
 *
 * Application use case for fetching a single storage item by ID.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for getting a storage item by ID.
 */
interface GetItemUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class GetItemUseCaseImpl(
    private val storageService: StorageService,
) : GetItemUseCase {

    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem> =
        storageService.getItem(itemId, userId)
}
