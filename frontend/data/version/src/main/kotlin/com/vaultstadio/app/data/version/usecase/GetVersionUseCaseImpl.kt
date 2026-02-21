/**
 * Implementation of GetVersionUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.GetVersionUseCase

class GetVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : GetVersionUseCase {

    override suspend fun invoke(itemId: String, versionNumber: Int) =
        versionRepository.getVersion(itemId, versionNumber)
}
