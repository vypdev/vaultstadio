/**
 * Implementation of GetVersionHistoryUseCase.
 */

package com.vaultstadio.app.data.version.usecase

import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.GetVersionHistoryUseCase

class GetVersionHistoryUseCaseImpl(
    private val versionRepository: VersionRepository,
) : GetVersionHistoryUseCase {

    override suspend fun invoke(itemId: String) = versionRepository.getVersionHistory(itemId)
}
