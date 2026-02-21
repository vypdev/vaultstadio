/**
 * Update Quota Use Case (admin)
 */

package com.vaultstadio.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.common.exception.StorageException

interface UpdateQuotaUseCase {
    suspend operator fun invoke(userId: String, quotaBytes: Long?, adminId: String): Either<StorageException, User>
}

class UpdateQuotaUseCaseImpl(private val userService: UserService) : UpdateQuotaUseCase {
    override suspend fun invoke(
        userId: String,
        quotaBytes: Long?,
        adminId: String,
    ): Either<StorageException, User> =
        userService.updateQuota(userId, quotaBytes, adminId)
}
