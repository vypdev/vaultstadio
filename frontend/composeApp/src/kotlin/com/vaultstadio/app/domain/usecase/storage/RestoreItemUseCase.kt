/**
 * Restore Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for restoring an item from trash.
 */
interface RestoreItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}

@Factory(binds = [RestoreItemUseCase::class])
class RestoreItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RestoreItemUseCase {

    override suspend operator fun invoke(itemId: String): Result<StorageItem> =
        storageRepository.restoreItem(itemId)
}
