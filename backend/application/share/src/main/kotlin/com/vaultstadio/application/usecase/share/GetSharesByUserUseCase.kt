/**
 * Get Shares By User Use Case
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface GetSharesByUserUseCase {
    suspend operator fun invoke(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>>
}

class GetSharesByUserUseCaseImpl(private val shareService: ShareService) : GetSharesByUserUseCase {
    override suspend fun invoke(userId: String, activeOnly: Boolean): Either<StorageException, List<ShareLink>> =
        shareService.getSharesByUser(userId, activeOnly)
}
