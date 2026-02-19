/**
 * Copy Item Use Case
 *
 * Application use case for copying a storage item.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for copying an item to a destination folder.
 */
interface CopyItemUseCase {

    suspend operator fun invoke(input: CopyItemInput): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class CopyItemUseCaseImpl(
    private val storageService: StorageService,
) : CopyItemUseCase {

    override suspend fun invoke(input: CopyItemInput): Either<StorageException, StorageItem> =
        storageService.copyItem(input)
}
