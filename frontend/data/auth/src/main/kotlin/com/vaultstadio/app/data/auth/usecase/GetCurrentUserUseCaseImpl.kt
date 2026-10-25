package com.vaultstadio.app.data.auth.usecase

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

class GetCurrentUserUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {

    override val currentUserFlow: StateFlow<User?> get() = authRepository.currentUserFlow

    override suspend operator fun invoke(): Result<User> =
        authRepository.getCurrentUser()

    override suspend fun refresh() = authRepository.refreshCurrentUser()

    override fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
