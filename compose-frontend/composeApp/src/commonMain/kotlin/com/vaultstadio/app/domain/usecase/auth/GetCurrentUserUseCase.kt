/**
 * Get Current User Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

/**
 * Use case for getting the current user.
 */
interface GetCurrentUserUseCase {
    val currentUserFlow: StateFlow<User?>
    suspend operator fun invoke(): ApiResult<User>
    suspend fun refresh()
    fun isLoggedIn(): Boolean
}

@Factory(binds = [GetCurrentUserUseCase::class])
class GetCurrentUserUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {

    override val currentUserFlow: StateFlow<User?> get() = authRepository.currentUserFlow

    override suspend operator fun invoke(): ApiResult<User> =
        authRepository.getCurrentUser()

    override suspend fun refresh() = authRepository.refreshCurrentUser()

    override fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
