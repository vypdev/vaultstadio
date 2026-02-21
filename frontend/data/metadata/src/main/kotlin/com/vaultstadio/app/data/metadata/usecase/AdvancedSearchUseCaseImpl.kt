/**
 * Advanced search use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.usecase.AdvancedSearchUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

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
