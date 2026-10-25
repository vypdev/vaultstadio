/**
 * Access Share Use Case
 *
 * Validates token/password and returns share + item for public share access.
 */

package com.vaultstadio.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.service.AccessShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.StorageItem

interface AccessShareUseCase {
    suspend operator fun invoke(input: AccessShareInput): Either<StorageException, Pair<ShareLink, StorageItem>>
}

class AccessShareUseCaseImpl(private val shareService: ShareService) : AccessShareUseCase {
    override suspend fun invoke(input: AccessShareInput): Either<StorageException, Pair<ShareLink, StorageItem>> =
        shareService.accessShare(input)
}
