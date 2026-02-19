/**
 * Login Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.LoginResult
import org.koin.core.annotation.Factory

/**
 * Use case for user login.
 */
interface LoginUseCase {
    suspend operator fun invoke(email: String, password: String): ApiResult<LoginResult>
}

@Factory(binds = [LoginUseCase::class])
class LoginUseCaseImpl(
    private val authRepository: AuthRepository,
) : LoginUseCase {

    override suspend operator fun invoke(email: String, password: String): ApiResult<LoginResult> =
        authRepository.login(email, password)
}
