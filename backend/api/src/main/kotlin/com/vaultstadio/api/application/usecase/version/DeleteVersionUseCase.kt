/**
 * Delete Version Use Case
 *
 * Application use case for deleting a file version.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.StorageException

interface DeleteVersionUseCase {

    suspend operator fun invoke(versionId: String, userId: String): Either<StorageException, Unit>
}

class DeleteVersionUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : DeleteVersionUseCase {

    override suspend fun invoke(versionId: String, userId: String): Either<StorageException, Unit> =
        fileVersionService.deleteVersion(versionId, userId)
}
