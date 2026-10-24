/**
 * Search Use Case
 *
 * Application use case for searching storage items by name.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageItem

interface SearchUseCase {
    suspend operator fun invoke(
        query: String,
        userId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<StorageException, PagedResult<StorageItem>>
}

class SearchUseCaseImpl(private val storageService: StorageService) : SearchUseCase {
    override suspend fun invoke(
        query: String,
        userId: String,
        limit: Int,
        offset: Int,
    ): Either<StorageException, PagedResult<StorageItem>> =
        storageService.search(query, userId, limit, offset)
}
