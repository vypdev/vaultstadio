package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.usecase.LogoutUseCase
import com.vaultstadio.app.domain.result.Result

class LogoutUseCaseImpl(
    private val authRepository: AuthRepository,
) : LogoutUseCase {

    override suspend operator fun invoke(): Result<Unit> =
        authRepository.logout()
}
