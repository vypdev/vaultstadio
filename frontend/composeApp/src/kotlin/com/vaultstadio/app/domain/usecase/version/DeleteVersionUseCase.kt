/**
 * Delete Version Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
import org.koin.core.annotation.Factory

/**
 * Use case for deleting a specific version of a file.
 */
interface DeleteVersionUseCase {
    suspend operator fun invoke(versionId: String): Result<Unit>
}

@Factory(binds = [DeleteVersionUseCase::class])
class DeleteVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : DeleteVersionUseCase {

    override suspend operator fun invoke(versionId: String): Result<Unit> =
        versionRepository.deleteVersion(versionId)
}
