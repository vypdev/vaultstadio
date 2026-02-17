/**
 * Update Profile Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.User
import org.koin.core.annotation.Factory

/**
 * Use case for updating user profile.
 */
interface UpdateProfileUseCase {
    suspend operator fun invoke(username: String? = null, avatarUrl: String? = null): ApiResult<User>
}

@Factory(binds = [UpdateProfileUseCase::class])
class UpdateProfileUseCaseImpl(
    private val authRepository: AuthRepository,
) : UpdateProfileUseCase {

    override suspend operator fun invoke(username: String?, avatarUrl: String?): ApiResult<User> =
        authRepository.updateProfile(username, avatarUrl)
}
