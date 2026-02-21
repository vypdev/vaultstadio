/**
 * Get Share Use Case
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface GetShareUseCase {
    suspend operator fun invoke(shareId: String): Either<StorageException, ShareLink>
}

class GetShareUseCaseImpl(private val shareService: ShareService) : GetShareUseCase {
    override suspend fun invoke(shareId: String): Either<StorageException, ShareLink> =
        shareService.getShare(shareId)
}
