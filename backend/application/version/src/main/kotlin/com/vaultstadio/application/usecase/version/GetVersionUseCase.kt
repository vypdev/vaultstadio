/**
 * Get Version Use Case
 *
 * Application use case for retrieving a specific version of an item.
 */

package com.vaultstadio.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.common.exception.StorageException

interface GetVersionUseCase {

    suspend operator fun invoke(itemId: String, versionNumber: Int): Either<StorageException, FileVersion?>
}

class GetVersionUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : GetVersionUseCase {

    override suspend fun invoke(itemId: String, versionNumber: Int): Either<StorageException, FileVersion?> =
        fileVersionService.getVersion(itemId, versionNumber)
}
