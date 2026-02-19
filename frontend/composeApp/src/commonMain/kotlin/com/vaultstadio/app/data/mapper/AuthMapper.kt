/**
 * Auth Mappers
 *
 * Converts between Auth DTOs and domain models.
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.auth.ChangePasswordRequestDTO
import com.vaultstadio.app.data.dto.auth.LoginRequestDTO
import com.vaultstadio.app.data.dto.auth.LoginResponseDTO
import com.vaultstadio.app.data.dto.auth.RegisterRequestDTO
import com.vaultstadio.app.data.dto.auth.StorageQuotaDTO
import com.vaultstadio.app.data.dto.auth.UpdateProfileRequestDTO
import com.vaultstadio.app.data.dto.auth.UserDTO
import com.vaultstadio.app.domain.model.LoginResult
import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.domain.model.User
import com.vaultstadio.app.domain.model.UserRole

// DTO -> Domain

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

// Domain -> DTO (for requests)

fun createLoginRequestDTO(email: String, password: String): LoginRequestDTO =
    LoginRequestDTO(email, password)

fun createRegisterRequestDTO(email: String, username: String, password: String): RegisterRequestDTO =
    RegisterRequestDTO(email, username, password)

fun createUpdateProfileRequestDTO(username: String?, avatarUrl: String?): UpdateProfileRequestDTO =
    UpdateProfileRequestDTO(username, avatarUrl)

fun createChangePasswordRequestDTO(currentPassword: String, newPassword: String): ChangePasswordRequestDTO =
    ChangePasswordRequestDTO(currentPassword, newPassword)
