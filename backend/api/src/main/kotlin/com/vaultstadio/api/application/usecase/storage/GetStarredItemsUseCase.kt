/**
 * Get Starred Items Use Case
 *
 * Application use case for listing starred items.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for getting the list of starred items for a user.
 */
interface GetStarredItemsUseCase {

    suspend operator fun invoke(userId: String): Either<StorageException, List<StorageItem>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class GetStarredItemsUseCaseImpl(
    private val storageService: StorageService,
) : GetStarredItemsUseCase {

    override suspend fun invoke(userId: String): Either<StorageException, List<StorageItem>> =
        storageService.getStarredItems(userId)
}
