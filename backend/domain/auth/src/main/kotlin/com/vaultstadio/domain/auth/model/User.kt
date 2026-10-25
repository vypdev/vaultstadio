/**
 * VaultStadio Auth Domain Models
 */

package com.vaultstadio.domain.auth.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class UserRole {
    ADMIN,
    USER,
    GUEST,
}

@Serializable
enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION,
}

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val username: String,
    val passwordHash: String,
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.ACTIVE,
    val quotaBytes: Long? = null,
    val avatarUrl: String? = null,
    val preferences: String? = null,
    val lastLoginAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun sanitized(): UserInfo = UserInfo(
        id = id,
        email = email,
        username = username,
        role = role,
        status = status,
        quotaBytes = quotaBytes,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
    )
}

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus,
    val quotaBytes: Long?,
    val avatarUrl: String?,
    val createdAt: Instant,
)

@Serializable
data class ApiKey(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val keyHash: String,
    val permissions: List<String> = emptyList(),
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val createdAt: Instant,
    val isActive: Boolean = true,
)

@Serializable
data class UserSession(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val tokenHash: String,
    val refreshTokenHash: String? = null,
    val ipAddress: String?,
    val userAgent: String?,
    val expiresAt: Instant,
    val createdAt: Instant,
    val lastActivityAt: Instant,
)

@Serializable
data class UserPreferences(
    val theme: String = "system",
    val language: String = "en",
    val defaultView: String = "grid",
    val sortBy: String = "name",
    val sortOrder: String = "asc",
    val showHiddenFiles: Boolean = false,
    val enableNotifications: Boolean = true,
)
