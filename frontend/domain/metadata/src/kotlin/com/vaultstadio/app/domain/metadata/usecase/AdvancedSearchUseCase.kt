/**
 * Use case for advanced search with metadata filters.
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

interface AdvancedSearchUseCase {
    suspend operator fun invoke(
        query: String,
        searchContent: Boolean = false,
        fileTypes: List<String>? = null,
        minSize: Long? = null,
        maxSize: Long? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<PaginatedResponse<StorageItem>>
}
