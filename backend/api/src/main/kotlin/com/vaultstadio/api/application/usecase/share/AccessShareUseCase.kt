/**
 * Access Share Use Case
 *
 * Validates token/password and returns share + item for public share access.
 */

package com.vaultstadio.api.application.usecase.share

import arrow.core.Either
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.AccessShareInput
import com.vaultstadio.core.domain.service.ShareService
import com.vaultstadio.core.exception.StorageException

interface AccessShareUseCase {
    suspend operator fun invoke(input: AccessShareInput): Either<StorageException, Pair<ShareLink, StorageItem>>
}

class AccessShareUseCaseImpl(private val shareService: ShareService) : AccessShareUseCase {
    override suspend fun invoke(input: AccessShareInput): Either<StorageException, Pair<ShareLink, StorageItem>> =
        shareService.accessShare(input)
}
