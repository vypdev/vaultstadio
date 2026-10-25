/**
 * Create Folder Use Case
 *
 * Application use case for creating a new folder.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.CreateFolderInput
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for creating a folder.
 */
interface CreateFolderUseCase {

    suspend operator fun invoke(input: CreateFolderInput): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class CreateFolderUseCaseImpl(
    private val storageService: StorageService,
) : CreateFolderUseCase {

    override suspend fun invoke(input: CreateFolderInput): Either<StorageException, StorageItem> =
        storageService.createFolder(input)
}
