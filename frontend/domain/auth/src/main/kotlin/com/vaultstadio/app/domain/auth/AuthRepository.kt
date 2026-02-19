/**
 * Auth Repository interface (domain contract).
 */

package com.vaultstadio.app.domain.auth

import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    val currentUserFlow: StateFlow<User?>
    val currentUser: User?
        get() = currentUserFlow.value

    suspend fun login(email: String, password: String): Result<LoginResult>
    suspend fun register(email: String, username: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User>
    suspend fun refreshCurrentUser()
    suspend fun getQuota(): Result<StorageQuota>
    suspend fun updateProfile(username: String?, avatarUrl: String?): Result<User>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    fun isLoggedIn(): Boolean
}
