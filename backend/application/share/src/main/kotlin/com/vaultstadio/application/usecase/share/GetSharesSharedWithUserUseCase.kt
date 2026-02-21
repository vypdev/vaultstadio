/**
 * Get Shares Shared With User Use Case
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface GetSharesSharedWithUserUseCase {
    suspend operator fun invoke(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>>
}

class GetSharesSharedWithUserUseCaseImpl(private val shareService: ShareService) : GetSharesSharedWithUserUseCase {
    override suspend fun invoke(userId: String, activeOnly: Boolean): Either<StorageException, List<ShareLink>> =
        shareService.getSharesSharedWithUser(userId, activeOnly)
}
