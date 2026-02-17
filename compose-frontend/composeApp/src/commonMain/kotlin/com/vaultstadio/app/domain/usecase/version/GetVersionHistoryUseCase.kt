/**
 * Get Version History Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.FileVersionHistory
import org.koin.core.annotation.Factory

/**
 * Use case for getting the version history of a file.
 */
interface GetVersionHistoryUseCase {
    suspend operator fun invoke(itemId: String): ApiResult<FileVersionHistory>
}

@Factory(binds = [GetVersionHistoryUseCase::class])
class GetVersionHistoryUseCaseImpl(
    private val versionRepository: VersionRepository,
) : GetVersionHistoryUseCase {

    override suspend operator fun invoke(itemId: String): ApiResult<FileVersionHistory> =
        versionRepository.getVersionHistory(itemId)
}
