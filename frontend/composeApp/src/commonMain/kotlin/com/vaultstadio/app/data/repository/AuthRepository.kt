/**
 * Auth Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.AuthService
import com.vaultstadio.app.domain.model.LoginResult
import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    val currentUserFlow: StateFlow<User?>
    val currentUser: User?
        get() = currentUserFlow.value

    suspend fun login(email: String, password: String): ApiResult<LoginResult>
    suspend fun register(email: String, username: String, password: String): ApiResult<User>
    suspend fun logout(): ApiResult<Unit>
    suspend fun getCurrentUser(): ApiResult<User>
    suspend fun refreshCurrentUser()
    suspend fun getQuota(): ApiResult<StorageQuota>
    suspend fun updateProfile(username: String?, avatarUrl: String?): ApiResult<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>
    fun isLoggedIn(): Boolean
}

@Single(binds = [AuthRepository::class])
class AuthRepositoryImpl(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): ApiResult<LoginResult> {
        val result = authService.login(email, password)
        result.onSuccess { loginResult ->
            tokenStorage.setAccessToken(loginResult.token)
            loginResult.refreshToken?.let { tokenStorage.setRefreshToken(it) }
            _currentUserFlow.value = loginResult.user
        }
        return result
    }

    override suspend fun register(email: String, username: String, password: String): ApiResult<User> =
        authService.register(email, username, password)

    override suspend fun logout(): ApiResult<Unit> {
        val result = authService.logout()
        tokenStorage.clear()
        _currentUserFlow.value = null
        return result
    }

    override suspend fun getCurrentUser(): ApiResult<User> =
        authService.getCurrentUser()

    override suspend fun refreshCurrentUser() {
        authService.getCurrentUser().onSuccess { user ->
            _currentUserFlow.value = user
        }
    }

    override suspend fun getQuota(): ApiResult<StorageQuota> =
        authService.getQuota()

    override suspend fun updateProfile(username: String?, avatarUrl: String?): ApiResult<User> {
        val result = authService.updateProfile(username, avatarUrl)
        result.onSuccess { user ->
            _currentUserFlow.value = user
        }
        return result
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        authService.changePassword(currentPassword, newPassword)

    override fun isLoggedIn(): Boolean = tokenStorage.getAccessToken() != null
}
