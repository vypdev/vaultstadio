/**
 * Trash Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for moving an item to trash.
 */
interface TrashItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}

@Factory(binds = [TrashItemUseCase::class])
class TrashItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : TrashItemUseCase {

    override suspend operator fun invoke(itemId: String): Result<StorageItem> =
        storageRepository.trashItem(itemId)
}
