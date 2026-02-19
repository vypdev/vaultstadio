/**
 * Logout Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import org.koin.core.annotation.Factory

/**
 * Use case for user logout.
 */
interface LogoutUseCase {
    suspend operator fun invoke(): ApiResult<Unit>
}

@Factory(binds = [LogoutUseCase::class])
class LogoutUseCaseImpl(
    private val authRepository: AuthRepository,
) : LogoutUseCase {

    override suspend operator fun invoke(): ApiResult<Unit> =
        authRepository.logout()
}
