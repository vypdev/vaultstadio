/**
 * Logout Use Case
 *
 * Application use case for logging out (invalidating session).
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface LogoutUseCase {
    suspend operator fun invoke(token: String): Either<StorageException, Unit>
}

class LogoutUseCaseImpl(private val userService: UserService) : LogoutUseCase {
    override suspend fun invoke(token: String): Either<StorageException, Unit> =
        userService.logout(token)
}
