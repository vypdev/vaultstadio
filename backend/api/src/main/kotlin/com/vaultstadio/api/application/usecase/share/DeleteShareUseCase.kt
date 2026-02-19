/**
 * Delete Share Use Case
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface DeleteShareUseCase {
    suspend operator fun invoke(shareId: String, userId: String): Either<StorageException, Unit>
}

class DeleteShareUseCaseImpl(private val shareService: ShareService) : DeleteShareUseCase {
    override suspend fun invoke(shareId: String, userId: String): Either<StorageException, Unit> =
        shareService.deleteShare(shareId, userId)
}
