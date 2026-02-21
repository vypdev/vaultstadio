/**
 * Get search suggestions use case implementation.
 */

package com.vaultstadio.app.data.metadata.usecase

import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.usecase.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.result.Result

class GetSearchSuggestionsUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetSearchSuggestionsUseCase {

    override suspend operator fun invoke(prefix: String, limit: Int): Result<List<String>> =
        metadataRepository.getSearchSuggestions(prefix, limit)
}
