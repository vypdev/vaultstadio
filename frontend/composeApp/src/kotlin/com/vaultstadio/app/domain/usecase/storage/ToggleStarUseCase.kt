/**
 * Toggle Star Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for toggling star on an item.
 */
interface ToggleStarUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}

@Factory(binds = [ToggleStarUseCase::class])
class ToggleStarUseCaseImpl(
    private val storageRepository: StorageRepository,
) : ToggleStarUseCase {

    override suspend operator fun invoke(itemId: String): Result<StorageItem> =
        storageRepository.toggleStar(itemId)
}
