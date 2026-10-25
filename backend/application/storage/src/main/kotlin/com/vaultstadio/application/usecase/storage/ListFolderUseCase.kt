/**
 * List Folder Use Case
 *
 * Application use case for listing folder contents. Routes depend on this instead of StorageService.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemQuery

/**
 * Use case for listing folder contents with pagination and sorting.
 */
interface ListFolderUseCase {

    suspend operator fun invoke(
        folderId: String?,
        userId: String,
        query: StorageItemQuery,
    ): Either<StorageException, PagedResult<StorageItem>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class ListFolderUseCaseImpl(
    private val storageService: StorageService,
) : ListFolderUseCase {

    override suspend fun invoke(
        folderId: String?,
        userId: String,
        query: StorageItemQuery,
    ): Either<StorageException, PagedResult<StorageItem>> =
        storageService.listFolder(folderId, userId, query)
}
