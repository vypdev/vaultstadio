/**
 * Internal storage query helpers used by StorageService.
 * Extracted to keep StorageService under the line limit.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.repository.StorageItemRepository
import com.vaultstadio.core.exception.AuthorizationException
import com.vaultstadio.core.exception.ItemNotFoundException
import com.vaultstadio.core.exception.StorageException

/**
 * Get an item by ID and verify the user owns it.
 */
internal suspend fun getItemInternal(
    repository: StorageItemRepository,
    itemId: String,
    userId: String,
): Either<StorageException, StorageItem> {
    return when (val result = repository.findById(itemId)) {
        is Either.Left -> result
        is Either.Right -> {
            val item = result.value
            if (item == null) {
                ItemNotFoundException("Item not found").left()
            } else if (item.ownerId != userId) {
                AuthorizationException("Access denied").left()
            } else {
                item.right()
            }
        }
    }
}
