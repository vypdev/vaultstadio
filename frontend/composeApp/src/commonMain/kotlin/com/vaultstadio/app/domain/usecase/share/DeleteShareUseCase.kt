/**
 * Delete Share Use Case
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.ShareRepository
import org.koin.core.annotation.Factory

/**
 * Use case for deleting a share link.
 */
interface DeleteShareUseCase {
    suspend operator fun invoke(shareId: String): ApiResult<Unit>
}

@Factory(binds = [DeleteShareUseCase::class])
class DeleteShareUseCaseImpl(
    private val shareRepository: ShareRepository,
) : DeleteShareUseCase {

    override suspend operator fun invoke(shareId: String): ApiResult<Unit> =
        shareRepository.deleteShare(shareId)
}
