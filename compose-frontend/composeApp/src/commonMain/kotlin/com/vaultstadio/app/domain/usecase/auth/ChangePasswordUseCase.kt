/**
 * Change Password Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import org.koin.core.annotation.Factory

/**
 * Use case for changing user password.
 */
interface ChangePasswordUseCase {
    suspend operator fun invoke(currentPassword: String, newPassword: String): ApiResult<Unit>
}

@Factory(binds = [ChangePasswordUseCase::class])
class ChangePasswordUseCaseImpl(
    private val authRepository: AuthRepository,
) : ChangePasswordUseCase {

    override suspend operator fun invoke(currentPassword: String, newPassword: String): ApiResult<Unit> =
        authRepository.changePassword(currentPassword, newPassword)
}
