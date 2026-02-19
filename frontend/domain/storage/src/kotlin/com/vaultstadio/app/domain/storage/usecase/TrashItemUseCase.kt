/**
 * Trash Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for moving an item to trash.
 */
interface TrashItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}
