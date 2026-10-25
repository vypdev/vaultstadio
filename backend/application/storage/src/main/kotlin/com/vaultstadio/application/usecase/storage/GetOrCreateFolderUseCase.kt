/**
 * Get Or Create Folder Use Case
 *
 * Application use case for getting an existing folder by path or creating it.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.GetOrCreateFolderResult
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException

interface GetOrCreateFolderUseCase {
    suspend operator fun invoke(
        name: String,
        parentId: String?,
        ownerId: String,
    ): Either<StorageException, GetOrCreateFolderResult>
}

class GetOrCreateFolderUseCaseImpl(private val storageService: StorageService) : GetOrCreateFolderUseCase {
    override suspend fun invoke(
        name: String,
        parentId: String?,
        ownerId: String,
    ): Either<StorageException, GetOrCreateFolderResult> =
        storageService.getOrCreateFolder(name, parentId, ownerId)
}
