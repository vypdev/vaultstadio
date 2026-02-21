/**
 * Implementation of DeleteShareUseCase.
 */

package com.vaultstadio.app.data.share.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.share.ShareRepository
import com.vaultstadio.app.domain.share.usecase.DeleteShareUseCase

class DeleteShareUseCaseImpl(
    private val shareRepository: ShareRepository,
) : DeleteShareUseCase {
    override suspend fun invoke(shareId: String): Result<Unit> =
        shareRepository.deleteShare(shareId)
}
