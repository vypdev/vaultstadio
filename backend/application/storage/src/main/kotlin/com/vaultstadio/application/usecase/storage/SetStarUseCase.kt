/**
 * Set Star Use Case
 *
 * Application use case for setting star status on an item to a specific value.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

interface SetStarUseCase {
    suspend operator fun invoke(itemId: String, userId: String, starred: Boolean): Either<StorageException, StorageItem>
}

class SetStarUseCaseImpl(private val storageService: StorageService) : SetStarUseCase {
    override suspend fun invoke(
        itemId: String,
        userId: String,
        starred: Boolean,
    ): Either<StorageException, StorageItem> =
        storageService.setStar(itemId, userId, starred)
}
