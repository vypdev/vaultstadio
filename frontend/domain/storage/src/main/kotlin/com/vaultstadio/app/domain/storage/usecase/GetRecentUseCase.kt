/**
 * Get Recent Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for getting recently accessed items.
 */
interface GetRecentUseCase {
    suspend operator fun invoke(limit: Int = 20): Result<List<StorageItem>>
}
