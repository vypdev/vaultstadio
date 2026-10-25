/**
 * Copy Item Use Case
 *
 * Application use case for copying a storage item.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.CopyItemInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

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
