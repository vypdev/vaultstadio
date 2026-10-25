/**
 * Auth Service – API calls and DTO to domain mapping.
 */

package com.vaultstadio.app.data.auth.service

import com.vaultstadio.app.data.auth.api.AuthApi
import com.vaultstadio.app.data.auth.mapper.createChangePasswordRequestDTO
import com.vaultstadio.app.data.auth.mapper.createLoginRequestDTO
import com.vaultstadio.app.data.auth.mapper.createRegisterRequestDTO
import com.vaultstadio.app.data.auth.mapper.createUpdateProfileRequestDTO
import com.vaultstadio.app.data.auth.mapper.toDomain
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User

class AuthService(private val authApi: AuthApi) {

    suspend fun login(email: String, password: String): ApiResult<LoginResult> =
        authApi.login(createLoginRequestDTO(email, password)).map { it.toDomain() }

    suspend fun register(email: String, username: String, password: String): ApiResult<User> =
        authApi.register(createRegisterRequestDTO(email, username, password)).map { it.toDomain() }

    suspend fun logout(): ApiResult<Unit> =
        authApi.logout()

    suspend fun getCurrentUser(): ApiResult<User> =
        authApi.getCurrentUser().map { it.toDomain() }

    suspend fun getQuota(): ApiResult<StorageQuota> =
        authApi.getQuota().map { it.toDomain() }

    suspend fun updateProfile(username: String?, avatarUrl: String?): ApiResult<User> =
        authApi.updateProfile(createUpdateProfileRequestDTO(username, avatarUrl)).map { it.toDomain() }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        authApi.changePassword(createChangePasswordRequestDTO(currentPassword, newPassword))
}
