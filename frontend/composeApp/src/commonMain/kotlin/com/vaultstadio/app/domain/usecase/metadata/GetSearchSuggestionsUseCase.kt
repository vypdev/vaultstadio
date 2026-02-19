/**
 * Get Search Suggestions Use Case
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.MetadataRepository
import org.koin.core.annotation.Factory

/**
 * Use case for getting search suggestions based on a prefix.
 */
interface GetSearchSuggestionsUseCase {
    suspend operator fun invoke(prefix: String, limit: Int = 10): ApiResult<List<String>>
}

@Factory(binds = [GetSearchSuggestionsUseCase::class])
class GetSearchSuggestionsUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetSearchSuggestionsUseCase {

    override suspend operator fun invoke(prefix: String, limit: Int): ApiResult<List<String>> =
        metadataRepository.getSearchSuggestions(prefix, limit)
}
