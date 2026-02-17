/**
 * User Domain Model
 */

package com.vaultstadio.app.domain.model

import kotlinx.datetime.Instant

/**
 * User role.
 */
enum class UserRole {
    ADMIN,
    USER,
    GUEST,
}

/**
 * User domain model.
 */
data class User(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val avatarUrl: String?,
    val createdAt: Instant,
)

/**
 * Login response domain model.
 */
data class LoginResult(
    val user: User,
    val token: String,
    val expiresAt: Instant,
    val refreshToken: String?,
)

/**
 * Storage quota domain model.
 */
data class StorageQuota(
    val usedBytes: Long,
    val quotaBytes: Long?,
    val usagePercentage: Double,
    val fileCount: Long,
    val folderCount: Long,
    val remainingBytes: Long?,
) {
    val isNearLimit: Boolean get() = usagePercentage >= 90
    val isOverLimit: Boolean get() = usagePercentage >= 100
}
