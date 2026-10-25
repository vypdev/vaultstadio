/**
 * Get Starred Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for getting starred items.
 */
interface GetStarredUseCase {
    suspend operator fun invoke(): Result<List<StorageItem>>
}
