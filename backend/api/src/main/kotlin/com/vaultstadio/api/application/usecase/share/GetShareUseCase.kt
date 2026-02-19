/**
 * Get Share Use Case
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface GetShareUseCase {
    suspend operator fun invoke(shareId: String): Either<StorageException, ShareLink>
}

class GetShareUseCaseImpl(private val shareService: ShareService) : GetShareUseCase {
    override suspend fun invoke(shareId: String): Either<StorageException, ShareLink> =
        shareService.getShare(shareId)
}
