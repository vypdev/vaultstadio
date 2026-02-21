/**
 * Get Quota Use Case
 */

package com.vaultstadio.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageQuota

interface GetQuotaUseCase {
    suspend operator fun invoke(userId: String): Either<StorageException, StorageQuota>
}

class GetQuotaUseCaseImpl(private val userService: UserService) : GetQuotaUseCase {
    override suspend fun invoke(userId: String): Either<StorageException, StorageQuota> =
        userService.getQuota(userId)
}
