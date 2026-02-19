/**
 * Get Shares By User Use Case
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface GetSharesByUserUseCase {
    suspend operator fun invoke(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>>
}

class GetSharesByUserUseCaseImpl(private val shareService: ShareService) : GetSharesByUserUseCase {
    override suspend fun invoke(userId: String, activeOnly: Boolean): Either<StorageException, List<ShareLink>> =
        shareService.getSharesByUser(userId, activeOnly)
}
