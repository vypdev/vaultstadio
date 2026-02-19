/**
 * Delete Share Use Case
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ShareRepository
/**
 * Use case for deleting a share link.
 */
interface DeleteShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}

class DeleteShareUseCaseImpl(
    private val shareRepository: ShareRepository,
) : DeleteShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        shareRepository.deleteShare(shareId)
}
