/**
 * Advanced Search Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.StorageItem
import kotlinx.datetime.Instant
/**
 * Use case for advanced search with metadata filters.
 */
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

class AdvancedSearchUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : AdvancedSearchUseCase {

    override suspend operator fun invoke(
        query: String,
        searchContent: Boolean,
        fileTypes: List<String>?,
        minSize: Long?,
        maxSize: Long?,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = metadataRepository.advancedSearch(
        query,
        searchContent,
        fileTypes,
        minSize,
        maxSize,
        fromDate,
        toDate,
        limit,
        offset,
    )
}
