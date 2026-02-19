/**
 * Cleanup Versions Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
/**
 * Use case for cleaning up old versions of a file.
 */
interface CleanupVersionsUseCase {
    suspend operator fun invoke(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): Result<Unit>
}

class CleanupVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CleanupVersionsUseCase {

    override suspend operator fun invoke(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): Result<Unit> = versionRepository.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
}
