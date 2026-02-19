/**
 * Auth Data Transfer Objects
 */

package com.vaultstadio.app.data.dto.auth

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDTO(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponseDTO(
    val user: UserDTO,
    val token: String,
    val expiresAt: Instant,
    val refreshToken: String? = null,
)

@Serializable
data class RegisterRequestDTO(
    val email: String,
    val username: String,
    val password: String,
)

@Serializable
data class UserDTO(
    val id: String,
    val email: String,
    val username: String,
    val role: String,
    val avatarUrl: String?,
    val createdAt: Instant,
)

@Serializable
data class StorageQuotaDTO(
    val usedBytes: Long,
    val quotaBytes: Long?,
    val usagePercentage: Double,
    val fileCount: Long,
    val folderCount: Long,
    val remainingBytes: Long?,
)

@Serializable
data class UpdateProfileRequestDTO(
    val username: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class ChangePasswordRequestDTO(
    val currentPassword: String,
    val newPassword: String,
)
