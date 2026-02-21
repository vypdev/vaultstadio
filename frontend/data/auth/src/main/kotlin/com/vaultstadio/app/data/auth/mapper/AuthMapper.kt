/**
 * Auth Mappers – DTO to domain and domain to DTO (requests).
 */

package com.vaultstadio.app.data.auth.mapper

import com.vaultstadio.app.data.auth.dto.ChangePasswordRequestDTO
import com.vaultstadio.app.data.auth.dto.LoginRequestDTO
import com.vaultstadio.app.data.auth.dto.LoginResponseDTO
import com.vaultstadio.app.data.auth.dto.RegisterRequestDTO
import com.vaultstadio.app.data.auth.dto.StorageQuotaDTO
import com.vaultstadio.app.data.auth.dto.UpdateProfileRequestDTO
import com.vaultstadio.app.data.auth.dto.UserDTO
import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.model.UserRole

fun UserDTO.toDomain(): User = User(
    id = id,
    email = email,
    username = username,
    role = try {
        UserRole.valueOf(role.uppercase())
    } catch (e: IllegalArgumentException) {
        UserRole.USER
    },
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)

fun LoginResponseDTO.toDomain(): LoginResult = LoginResult(
    user = user.toDomain(),
    token = token,
    expiresAt = expiresAt,
    refreshToken = refreshToken,
)

fun StorageQuotaDTO.toDomain(): StorageQuota = StorageQuota(
    usedBytes = usedBytes,
    quotaBytes = quotaBytes,
    usagePercentage = usagePercentage,
    fileCount = fileCount,
    folderCount = folderCount,
    remainingBytes = remainingBytes,
)

fun createLoginRequestDTO(email: String, password: String): LoginRequestDTO =
    LoginRequestDTO(email, password)

fun createRegisterRequestDTO(email: String, username: String, password: String): RegisterRequestDTO =
    RegisterRequestDTO(email, username, password)

fun createUpdateProfileRequestDTO(username: String?, avatarUrl: String?): UpdateProfileRequestDTO =
    UpdateProfileRequestDTO(username, avatarUrl)

fun createChangePasswordRequestDTO(currentPassword: String, newPassword: String): ChangePasswordRequestDTO =
    ChangePasswordRequestDTO(currentPassword, newPassword)
