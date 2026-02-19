/**
 * Get Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for getting a single storage item.
 */
interface GetItemUseCase {
    suspend operator fun invoke(itemId: String): Result<StorageItem>
}
