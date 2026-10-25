package com.vaultstadio.app.data.auth.usecase

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.usecase.LoginUseCase
import com.vaultstadio.app.domain.result.Result

class LoginUseCaseImpl(
    private val authRepository: AuthRepository,
) : LoginUseCase {

    override suspend operator fun invoke(email: String, password: String): Result<LoginResult> =
        authRepository.login(email, password)
}
