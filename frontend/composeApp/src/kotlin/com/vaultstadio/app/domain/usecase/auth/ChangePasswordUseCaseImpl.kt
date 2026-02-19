package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.usecase.ChangePasswordUseCase
import com.vaultstadio.app.domain.result.Result

class ChangePasswordUseCaseImpl(
    private val authRepository: AuthRepository,
) : ChangePasswordUseCase {

    override suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        authRepository.changePassword(currentPassword, newPassword)
}
