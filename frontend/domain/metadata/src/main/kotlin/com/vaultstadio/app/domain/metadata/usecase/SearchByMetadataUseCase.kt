/**
 * Use case for searching files by metadata key/value pairs.
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse

interface SearchByMetadataUseCase {
    suspend operator fun invoke(
        key: String,
        value: String? = null,
        pluginId: String? = null,
        limit: Int = 50,
    ): Result<PaginatedResponse<MetadataSearchResult>>
}
