/**
 * Auth Repository implementation.
 */

package com.vaultstadio.app.data.auth.repository

import com.vaultstadio.app.data.auth.service.AuthService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val authService: AuthService,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUserFlow: StateFlow<User?> = _currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): Result<LoginResult> {
        val result = authService.login(email, password)
        result.onSuccess { loginResult ->
            tokenStorage.setAccessToken(loginResult.token)
            loginResult.refreshToken?.let { tokenStorage.setRefreshToken(it) }
            _currentUserFlow.value = loginResult.user
        }
        return result.toResult()
    }

    override suspend fun register(email: String, username: String, password: String): Result<User> =
        authService.register(email, username, password).toResult()

    override suspend fun logout(): Result<Unit> {
        val result = authService.logout()
        tokenStorage.clear()
        _currentUserFlow.value = null
        return result.toResult()
    }

    override suspend fun getCurrentUser(): Result<User> =
        authService.getCurrentUser().toResult()

    override suspend fun refreshCurrentUser() {
        authService.getCurrentUser().onSuccess { user ->
            _currentUserFlow.value = user
        }
    }

    override suspend fun getQuota(): Result<StorageQuota> =
        authService.getQuota().toResult()

    override suspend fun updateProfile(username: String?, avatarUrl: String?): Result<User> {
        val result = authService.updateProfile(username, avatarUrl)
        result.onSuccess { user ->
            _currentUserFlow.value = user
        }
        return result.toResult()
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        authService.changePassword(currentPassword, newPassword).toResult()

    override fun isLoggedIn(): Boolean = tokenStorage.getAccessToken() != null
}
