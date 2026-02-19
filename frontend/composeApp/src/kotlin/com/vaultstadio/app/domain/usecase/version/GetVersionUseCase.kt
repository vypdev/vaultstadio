/**
 * Get Version Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
import com.vaultstadio.app.domain.model.FileVersion
import org.koin.core.annotation.Factory

/**
 * Use case for getting a specific version of a file.
 */
interface GetVersionUseCase {
    suspend operator fun invoke(itemId: String, versionNumber: Int): Result<FileVersion>
}

@Factory(binds = [GetVersionUseCase::class])
class GetVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : GetVersionUseCase {

    override suspend operator fun invoke(itemId: String, versionNumber: Int): Result<FileVersion> =
        versionRepository.getVersion(itemId, versionNumber)
}
