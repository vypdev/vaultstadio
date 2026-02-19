/**
 * Search Use Case
 */

package com.vaultstadio.app.domain.storage.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem

/**
 * Use case for searching items.
 */
interface SearchUseCase {
    suspend operator fun invoke(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<PaginatedResponse<StorageItem>>
}
