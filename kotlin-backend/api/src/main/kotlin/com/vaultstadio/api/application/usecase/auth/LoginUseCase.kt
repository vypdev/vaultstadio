/**
 * Login Use Case
 *
 * Application use case for user login.
 */

package com.vaultstadio.api.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.service.LoginInput
import com.vaultstadio.core.domain.service.LoginResult
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface LoginUseCase {
    suspend operator fun invoke(input: LoginInput): Either<StorageException, LoginResult>
}

class LoginUseCaseImpl(private val userService: UserService) : LoginUseCase {
    override suspend fun invoke(input: LoginInput): Either<StorageException, LoginResult> =
        userService.login(input)
}
