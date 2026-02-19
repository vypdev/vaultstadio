/**
 * Cleanup Versions Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import org.koin.core.annotation.Factory

/**
 * Use case for cleaning up old versions of a file.
 */
interface CleanupVersionsUseCase {
    suspend operator fun invoke(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): ApiResult<Unit>
}

@Factory(binds = [CleanupVersionsUseCase::class])
class CleanupVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CleanupVersionsUseCase {

    override suspend operator fun invoke(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): ApiResult<Unit> = versionRepository.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
}
