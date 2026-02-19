/**
 * VaultStadio User Domain Models
 *
 * User and authentication related models.
 */

package com.vaultstadio.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents user roles in the system.
 */
@Serializable
enum class UserRole {
    ADMIN,
    USER,
    GUEST,
}

/**
 * Represents the status of a user account.
 */
@Serializable
enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION,
}

/**
 * Core user model.
 *
 * @property id Unique identifier
 * @property email User's email address
 * @property username Display username
 * @property passwordHash Hashed password
 * @property role User's role
 * @property status Account status
 * @property quotaBytes Storage quota in bytes (null for unlimited)
 * @property avatarUrl URL to user's avatar
 * @property preferences User preferences as JSON
 * @property lastLoginAt When the user last logged in
 * @property createdAt When the account was created
 * @property updatedAt When the account was last updated
 */
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
    /**
     * Returns a sanitized version of the user without sensitive data.
     */
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

/**
 * Public user information (without sensitive data).
 */
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

/**
 * Represents an API key for programmatic access.
 *
 * @property id Unique identifier
 * @property userId Owner of this API key
 * @property name Display name for the key
 * @property keyHash Hashed API key
 * @property permissions List of permitted operations
 * @property expiresAt When this key expires (null for no expiration)
 * @property lastUsedAt When this key was last used
 * @property createdAt When this key was created
 * @property isActive Whether the key is active
 */
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

/**
 * Represents a user session.
 *
 * @property id Unique identifier
 * @property userId User this session belongs to
 * @property tokenHash Hashed session token
 * @property refreshTokenHash Hashed refresh token for token rotation
 * @property ipAddress IP address of the client
 * @property userAgent User agent of the client
 * @property expiresAt When this session expires
 * @property createdAt When this session was created
 * @property lastActivityAt When this session was last active
 */
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

/**
 * Represents user preferences.
 */
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
