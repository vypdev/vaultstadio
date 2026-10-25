/**
 * Toggle Star Use Case
 *
 * Application use case for toggling star status on an item.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for toggling the starred state of an item.
 */
interface ToggleStarUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class ToggleStarUseCaseImpl(
    private val storageService: StorageService,
) : ToggleStarUseCase {

    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, StorageItem> =
        storageService.toggleStar(itemId, userId)
}
