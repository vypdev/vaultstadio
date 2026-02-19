/**
 * Get Version Use Case
 *
 * Application use case for retrieving a specific version of an item.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.StorageException

interface GetVersionUseCase {

    suspend operator fun invoke(itemId: String, versionNumber: Int): Either<StorageException, FileVersion?>
}

class GetVersionUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : GetVersionUseCase {

    override suspend fun invoke(itemId: String, versionNumber: Int): Either<StorageException, FileVersion?> =
        fileVersionService.getVersion(itemId, versionNumber)
}
