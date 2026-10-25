/**
 * Get Trash Items Use Case
 *
 * Application use case for listing trashed items.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

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
