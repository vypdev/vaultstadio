/**
 * Create Share Use Case
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.CreateShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink

interface CreateShareUseCase {
    suspend operator fun invoke(input: CreateShareInput): Either<StorageException, ShareLink>
}

class CreateShareUseCaseImpl(private val shareService: ShareService) : CreateShareUseCase {
    override suspend fun invoke(input: CreateShareInput): Either<StorageException, ShareLink> =
        shareService.createShare(input)
}
