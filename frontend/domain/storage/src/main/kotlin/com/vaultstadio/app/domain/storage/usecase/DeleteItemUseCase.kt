/**
 * Delete Item Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result

/**
 * Use case for permanently deleting an item.
 */
interface DeleteItemUseCase {
    suspend operator fun invoke(itemId: String): Result<Unit>
}
