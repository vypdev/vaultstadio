/**
 * Implementation of DeleteVersionUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.DeleteVersionUseCase

class DeleteVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : DeleteVersionUseCase {

    override suspend fun invoke(versionId: String) = versionRepository.deleteVersion(versionId)
}
