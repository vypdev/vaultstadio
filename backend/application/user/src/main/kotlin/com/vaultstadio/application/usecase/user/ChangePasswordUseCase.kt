/**
 * Change Password Use Case
 */

package com.vaultstadio.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.StorageException

interface ChangePasswordUseCase {
    suspend operator fun invoke(
        userId: String,
        currentPassword: String,
        newPassword: String,
    ): Either<StorageException, Unit>
}

class ChangePasswordUseCaseImpl(private val userService: UserService) : ChangePasswordUseCase {
    override suspend fun invoke(
        userId: String,
        currentPassword: String,
        newPassword: String,
    ): Either<StorageException, Unit> =
        userService.changePassword(userId, currentPassword, newPassword)
}
