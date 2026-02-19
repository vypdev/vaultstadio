/**
 * Get Trash Items Use Case
 *
 * Application use case for listing trashed items.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for getting the list of trashed items for a user.
 */
interface GetTrashItemsUseCase {

    suspend operator fun invoke(userId: String): Either<StorageException, List<StorageItem>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class GetTrashItemsUseCaseImpl(
    private val storageService: StorageService,
) : GetTrashItemsUseCase {

    override suspend fun invoke(userId: String): Either<StorageException, List<StorageItem>> =
        storageService.getTrashItems(userId)
}
