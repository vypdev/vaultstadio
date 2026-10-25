/**
 * Search by metadata use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.usecase.SearchByMetadataUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse

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
