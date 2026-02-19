/**
 * Toggle Star Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for toggling star on an item.
 */
interface ToggleStarUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}
