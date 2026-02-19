/**
 * Set Star Use Case
 *
 * Application use case for setting star status on an item to a specific value.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

interface SetStarUseCase {
    suspend operator fun invoke(itemId: String, userId: String, starred: Boolean): Either<StorageException, StorageItem>
}

class SetStarUseCaseImpl(private val storageService: StorageService) : SetStarUseCase {
    override suspend fun invoke(itemId: String, userId: String, starred: Boolean): Either<StorageException, StorageItem> =
        storageService.setStar(itemId, userId, starred)
}
