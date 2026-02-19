/**
 * Restore Version Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import org.koin.core.annotation.Factory

/**
 * Use case for restoring a file to a previous version.
 */
interface RestoreVersionUseCase {
    suspend operator fun invoke(itemId: String, versionNumber: Int, comment: String? = null): ApiResult<Unit>
}

@Factory(binds = [RestoreVersionUseCase::class])
class RestoreVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : RestoreVersionUseCase {

    override suspend operator fun invoke(itemId: String, versionNumber: Int, comment: String?): ApiResult<Unit> =
        versionRepository.restoreVersion(itemId, versionNumber, comment)
}
