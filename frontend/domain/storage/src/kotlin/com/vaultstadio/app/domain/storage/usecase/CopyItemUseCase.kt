/**
 * Copy Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for copying an item.
 */
interface CopyItemUseCase {
    suspend operator fun invoke(
        itemId: String,
        destinationId: String?,
        newName: String? = null,
    ): Result<StorageItem>
}
