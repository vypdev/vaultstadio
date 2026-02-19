/**
 * Get Shares By Item Use Case
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface GetSharesByItemUseCase {
    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, List<ShareLink>>
}

class GetSharesByItemUseCaseImpl(private val shareService: ShareService) : GetSharesByItemUseCase {
    override suspend fun invoke(itemId: String, userId: String): Either<StorageException, List<ShareLink>> =
        shareService.getSharesByItem(itemId, userId)
}
