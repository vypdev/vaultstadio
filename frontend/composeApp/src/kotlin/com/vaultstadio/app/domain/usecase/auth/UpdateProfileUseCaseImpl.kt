package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.UpdateProfileUseCase
import com.vaultstadio.app.domain.result.Result

class UpdateProfileUseCaseImpl(
    private val authRepository: AuthRepository,
) : UpdateProfileUseCase {

    override suspend operator fun invoke(username: String?, avatarUrl: String?): Result<User> =
        authRepository.updateProfile(username, avatarUrl)
}
