/**
 * Register Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.User
import org.koin.core.annotation.Factory

/**
 * Use case for user registration.
 */
interface RegisterUseCase {
    suspend operator fun invoke(email: String, username: String, password: String): ApiResult<User>
}

@Factory(binds = [RegisterUseCase::class])
class RegisterUseCaseImpl(
    private val authRepository: AuthRepository,
) : RegisterUseCase {

    override suspend operator fun invoke(email: String, username: String, password: String): ApiResult<User> =
        authRepository.register(email, username, password)
}
