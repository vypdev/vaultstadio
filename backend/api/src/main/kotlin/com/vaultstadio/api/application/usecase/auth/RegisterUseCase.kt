/**
 * Register Use Case
 *
 * Application use case for user registration.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.service.RegisterUserInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface RegisterUseCase {
    suspend operator fun invoke(input: RegisterUserInput): Either<StorageException, User>
}

class RegisterUseCaseImpl(private val userService: UserService) : RegisterUseCase {
    override suspend fun invoke(input: RegisterUserInput): Either<StorageException, User> =
        userService.register(input)
}
