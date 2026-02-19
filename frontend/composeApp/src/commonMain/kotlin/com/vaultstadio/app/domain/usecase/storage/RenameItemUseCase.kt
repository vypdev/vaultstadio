/**
 * Rename Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for renaming an item.
 */
interface RenameItemUseCase {
    suspend operator fun invoke(itemId: String, newName: String): ApiResult<StorageItem>
}

@Factory(binds = [RenameItemUseCase::class])
class RenameItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : RenameItemUseCase {

    override suspend operator fun invoke(itemId: String, newName: String): ApiResult<StorageItem> =
        storageRepository.renameItem(itemId, newName)
}
