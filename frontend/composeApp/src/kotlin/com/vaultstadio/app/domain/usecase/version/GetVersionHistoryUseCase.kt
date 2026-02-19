/**
 * Get Version History Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.FileVersionHistory
/**
 * Use case for getting the version history of a file.
 */
interface GetVersionHistoryUseCase {
    suspend operator fun invoke(itemId: String): Result<FileVersionHistory>
}

class GetVersionHistoryUseCaseImpl(
    private val versionRepository: VersionRepository,
) : GetVersionHistoryUseCase {

    override suspend operator fun invoke(itemId: String): Result<FileVersionHistory> =
        versionRepository.getVersionHistory(itemId)
}
