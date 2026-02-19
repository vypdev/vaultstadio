/**
 * Compare Versions Use Case
 *
 * Application use case for comparing two file versions.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.StorageException

interface CompareVersionsUseCase {

    suspend operator fun invoke(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ): Either<StorageException, VersionDiff>
}

class CompareVersionsUseCaseImpl(
    private val fileVersionService: FileVersionService,
) : CompareVersionsUseCase {

    override suspend fun invoke(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ): Either<StorageException, VersionDiff> =
        fileVersionService.compareVersions(itemId, fromVersion, toVersion)
}
