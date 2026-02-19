/**
 * Auth API
 *
 * Handles authentication and user-related API calls.
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.auth.ChangePasswordRequestDTO
import com.vaultstadio.app.data.dto.auth.LoginRequestDTO
import com.vaultstadio.app.data.dto.auth.LoginResponseDTO
import com.vaultstadio.app.data.dto.auth.RegisterRequestDTO
import com.vaultstadio.app.data.dto.auth.StorageQuotaDTO
import com.vaultstadio.app.data.dto.auth.UpdateProfileRequestDTO
import com.vaultstadio.app.data.dto.auth.UserDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

/**
 * API for authentication and user operations.
 */
@Single
class AuthApi(client: HttpClient) : BaseApi(client) {

    suspend fun login(request: LoginRequestDTO): ApiResult<LoginResponseDTO> =
        post("/api/v1/auth/login", request)

    suspend fun register(request: RegisterRequestDTO): ApiResult<UserDTO> =
        post("/api/v1/auth/register", request)

    suspend fun logout(): ApiResult<Unit> =
        postNoBody("/api/v1/auth/logout")

    suspend fun getCurrentUser(): ApiResult<UserDTO> =
        get("/api/v1/user/me")

    suspend fun getQuota(): ApiResult<StorageQuotaDTO> =
        get("/api/v1/user/me/quota")

    suspend fun updateProfile(request: UpdateProfileRequestDTO): ApiResult<UserDTO> =
        patch("/api/v1/user/me", request)

    suspend fun changePassword(request: ChangePasswordRequestDTO): ApiResult<Unit> =
        post("/api/v1/user/me/password", request)
}
