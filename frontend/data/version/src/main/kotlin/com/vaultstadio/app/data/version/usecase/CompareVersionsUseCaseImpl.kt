/**
 * Implementation of CompareVersionsUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.CompareVersionsUseCase

class CompareVersionsUseCaseImpl(
    private val versionRepository: VersionRepository,
) : CompareVersionsUseCase {

    override suspend fun invoke(itemId: String, fromVersion: Int, toVersion: Int) =
        versionRepository.compareVersions(itemId, fromVersion, toVersion)
}
