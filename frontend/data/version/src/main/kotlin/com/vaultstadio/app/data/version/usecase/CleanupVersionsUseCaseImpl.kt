/**
 * Implementation of CleanupVersionsUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.CleanupVersionsUseCase

class CleanupVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CleanupVersionsUseCase {

    override suspend fun invoke(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ) = versionRepository.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
}
