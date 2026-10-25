/**
 * Get Trash Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for getting trashed items.
 */
interface GetTrashUseCase {
    suspend operator fun invoke(): Result<List<StorageItem>>
}
