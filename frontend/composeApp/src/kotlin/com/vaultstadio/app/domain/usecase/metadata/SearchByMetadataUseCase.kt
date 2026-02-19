/**
 * Search By Metadata Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.MetadataSearchResult
import com.vaultstadio.app.domain.model.PaginatedResponse
/**
 * Use case for searching files by metadata key/value pairs.
 */
interface SearchByMetadataUseCase {
    suspend operator fun invoke(
        key: String,
        value: String? = null,
        pluginId: String? = null,
        limit: Int = 50,
    ): Result<PaginatedResponse<MetadataSearchResult>>
}

class SearchByMetadataUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : SearchByMetadataUseCase {

    override suspend operator fun invoke(
        key: String,
        value: String?,
        pluginId: String?,
        limit: Int,
    ): Result<PaginatedResponse<MetadataSearchResult>> =
        metadataRepository.searchByMetadata(key, value, pluginId, limit)
}
