/**
 * Compare Versions Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.VersionDiff
/**
 * Use case for comparing two versions of a file.
 */
interface CompareVersionsUseCase {
    suspend operator fun invoke(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff>
}

class CompareVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CompareVersionsUseCase {

    override suspend operator fun invoke(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff> =
        versionRepository.compareVersions(itemId, fromVersion, toVersion)
}
