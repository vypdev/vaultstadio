/**
 * Compare Versions Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.VersionDiff
import org.koin.core.annotation.Factory

/**
 * Use case for comparing two versions of a file.
 */
interface CompareVersionsUseCase {
    suspend operator fun invoke(itemId: String, fromVersion: Int, toVersion: Int): ApiResult<VersionDiff>
}

@Factory(binds = [CompareVersionsUseCase::class])
class CompareVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CompareVersionsUseCase {

    override suspend operator fun invoke(itemId: String, fromVersion: Int, toVersion: Int): ApiResult<VersionDiff> =
        versionRepository.compareVersions(itemId, fromVersion, toVersion)
}
