/**
 * Implementation of RestoreVersionUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.RestoreVersionUseCase

class RestoreVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : RestoreVersionUseCase {

    override suspend fun invoke(itemId: String, versionNumber: Int, comment: String?) =
        versionRepository.restoreVersion(itemId, versionNumber, comment)
}
