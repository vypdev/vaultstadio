/**
 * Delete Version Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.VersionRepository
import org.koin.core.annotation.Factory

/**
 * Use case for deleting a specific version of a file.
 */
interface DeleteVersionUseCase {
    suspend operator fun invoke(versionId: String): ApiResult<Unit>
}

@Factory(binds = [DeleteVersionUseCase::class])
class DeleteVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : DeleteVersionUseCase {

    override suspend operator fun invoke(versionId: String): ApiResult<Unit> =
        versionRepository.deleteVersion(versionId)
}
