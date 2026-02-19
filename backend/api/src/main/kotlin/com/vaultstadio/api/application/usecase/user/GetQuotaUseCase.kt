/**
 * Get Quota Use Case
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageQuota
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface GetQuotaUseCase {
    suspend operator fun invoke(userId: String): Either<StorageException, StorageQuota>
}

class GetQuotaUseCaseImpl(private val userService: UserService) : GetQuotaUseCase {
    override suspend fun invoke(userId: String): Either<StorageException, StorageQuota> =
        userService.getQuota(userId)
}
