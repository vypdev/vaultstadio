/**
 * Use case for getting search suggestions based on a prefix.
 */

package com.vaultstadio.app.domain.metadata.usecase

import com.vaultstadio.app.domain.result.Result

interface GetSearchSuggestionsUseCase {
    suspend operator fun invoke(prefix: String, limit: Int = 10): Result<List<String>>
}
