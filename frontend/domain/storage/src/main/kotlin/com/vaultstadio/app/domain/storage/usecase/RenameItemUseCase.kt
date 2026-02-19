/**
 * Rename Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for renaming an item.
 */
interface RenameItemUseCase {
    suspend operator fun invoke(itemId: String, newName: String): Result<StorageItem>
}
