/**
 * Logout All Use Case
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface LogoutAllUseCase {
    suspend operator fun invoke(userId: String): Either<StorageException, Unit>
}

class LogoutAllUseCaseImpl(private val userService: UserService) : LogoutAllUseCase {
    override suspend fun invoke(userId: String): Either<StorageException, Unit> =
        userService.logoutAll(userId)
}
