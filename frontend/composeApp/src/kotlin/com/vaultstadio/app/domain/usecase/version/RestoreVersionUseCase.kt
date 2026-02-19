/**
 * Restore Version Use Case
 */

package com.vaultstadio.app.domain.usecase.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.VersionRepository
/**
 * Use case for restoring a file to a previous version.
 */
interface RestoreVersionUseCase {
    suspend operator fun invoke(itemId: String, versionNumber: Int, comment: String? = null): Result<Unit>
}

class RestoreVersionUseCaseImpl(
    private val versionRepository: VersionRepository,
) : RestoreVersionUseCase {

    override suspend operator fun invoke(itemId: String, versionNumber: Int, comment: String?): Result<Unit> =
        versionRepository.restoreVersion(itemId, versionNumber, comment)
}
