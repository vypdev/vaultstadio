package com.vaultstadio.app.data.auth.usecase

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.RegisterUseCase
import com.vaultstadio.app.domain.result.Result

class RegisterUseCaseImpl(
    private val authRepository: AuthRepository,
) : RegisterUseCase {

    override suspend operator fun invoke(email: String, username: String, password: String): Result<User> =
        authRepository.register(email, username, password)
}
