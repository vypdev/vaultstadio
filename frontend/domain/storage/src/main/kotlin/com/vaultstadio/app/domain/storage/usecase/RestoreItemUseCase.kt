/**
 * Restore Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for restoring an item from trash.
 */
interface RestoreItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}
