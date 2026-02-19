/**
 * Get Shares Shared With User Use Case
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface GetSharesSharedWithUserUseCase {
    suspend operator fun invoke(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>>
}

class GetSharesSharedWithUserUseCaseImpl(private val shareService: ShareService) : GetSharesSharedWithUserUseCase {
    override suspend fun invoke(userId: String, activeOnly: Boolean): Either<StorageException, List<ShareLink>> =
        shareService.getSharesSharedWithUser(userId, activeOnly)
}
