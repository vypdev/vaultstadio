/**
 * Get Shares By Item Use Case
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface GetSharesByItemUseCase {
    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, List<ShareLink>>
}

class GetSharesByItemUseCaseImpl(private val shareService: ShareService) : GetSharesByItemUseCase {
    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, List<ShareLink>> =
        shareService.getSharesByItem(itemId, userId)
}
