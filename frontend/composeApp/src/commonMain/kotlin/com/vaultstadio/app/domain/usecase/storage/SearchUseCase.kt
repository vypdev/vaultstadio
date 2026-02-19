/**
 * Search Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.StorageItem
import org.koin.core.annotation.Factory

/**
 * Use case for searching items.
 */
interface SearchUseCase {
    suspend operator fun invoke(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): ApiResult<PaginatedResponse<StorageItem>>
}

@Factory(binds = [SearchUseCase::class])
class SearchUseCaseImpl(
    private val storageRepository: StorageRepository,
) : SearchUseCase {

    override suspend operator fun invoke(
        query: String,
        limit: Int,
        offset: Int,
    ): ApiResult<PaginatedResponse<StorageItem>> = storageRepository.search(query, limit, offset)
}
