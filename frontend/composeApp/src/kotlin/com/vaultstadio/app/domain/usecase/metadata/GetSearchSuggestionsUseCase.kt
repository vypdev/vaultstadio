/**
 * Get Search Suggestions Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
/**
 * Use case for getting search suggestions based on a prefix.
 */
interface GetSearchSuggestionsUseCase {
    suspend operator fun invoke(prefix: String, limit: Int = 10): Result<List<String>>
}

class GetSearchSuggestionsUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetSearchSuggestionsUseCase {

    override suspend operator fun invoke(prefix: String, limit: Int): Result<List<String>> =
        metadataRepository.getSearchSuggestions(prefix, limit)
}
