/**
 * Restore Version Use Case
 *
 * Application use case for restoring a file to a previous version.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.domain.service.RestoreVersionInput
import com.vaultstadio.core.exception.StorageException

interface RestoreVersionUseCase {

    suspend operator fun invoke(input: RestoreVersionInput, userId: String): Either<StorageException, FileVersion>
}

class RestoreVersionUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : RestoreVersionUseCase {

    override suspend fun invoke(input: RestoreVersionInput, userId: String): Either<StorageException, FileVersion> =
        fileVersionService.restoreVersion(input, userId)
}
