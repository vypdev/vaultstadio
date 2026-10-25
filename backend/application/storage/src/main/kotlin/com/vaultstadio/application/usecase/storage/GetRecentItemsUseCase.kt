/**
 * Get Recent Items Use Case
 *
 * Application use case for listing recently modified items.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for getting recently modified items for a user.
 */
interface GetRecentItemsUseCase {

    suspend operator fun invoke(userId: String, limit: Int = 20): Either<StorageException, List<StorageItem>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class GetRecentItemsUseCaseImpl(
    private val storageService: StorageService,
) : GetRecentItemsUseCase {

    override suspend fun invoke(
        userId: String,
        limit: Int,
    ): Either<StorageException, List<StorageItem>> =
        storageService.getRecentItems(userId, limit)
}
