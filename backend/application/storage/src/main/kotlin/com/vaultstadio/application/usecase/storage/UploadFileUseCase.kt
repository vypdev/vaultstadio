/**
 * Upload File Use Case
 *
 * Application use case for uploading a file.
 */

package com.vaultstadio.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.domain.service.UploadFileInput
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem

/**
 * Use case for uploading a file.
 */
interface UploadFileUseCase {

    suspend operator fun invoke(input: UploadFileInput): Either<StorageException, StorageItem>
}

/**
 * Default implementation delegating to [StorageService].
 */
class UploadFileUseCaseImpl(
    private val storageService: StorageService,
) : UploadFileUseCase {

    override suspend fun invoke(input: UploadFileInput): Either<StorageException, StorageItem> =
        storageService.uploadFile(input)
}
