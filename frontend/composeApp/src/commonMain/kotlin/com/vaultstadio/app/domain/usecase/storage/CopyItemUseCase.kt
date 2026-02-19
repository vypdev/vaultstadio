/**
 * Copy Item Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for copying an item.
 */
interface CopyItemUseCase {
    suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String? = null,
    ): ApiResult<StorageItem>
}

@Factory(binds = [CopyItemUseCase::class])
class CopyItemUseCaseImpl(
    private val storageRepository: StorageRepository,
) : CopyItemUseCase {

    override suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String?,
    ): ApiResult<StorageItem> = storageRepository.copyItem(itemId, destinationId, newName)
}
