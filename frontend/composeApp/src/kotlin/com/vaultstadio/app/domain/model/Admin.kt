/**
 * Admin Domain Models
 */

package com.vaultstadio.app.domain.model

import com.vaultstadio.app.domain.auth.model.UserRole
import kotlinx.datetime.Instant

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
}

data class AdminUser(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus,
    val avatarUrl: String?,
    val quotaBytes: Long?,
    val usedBytes: Long,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
) {
    val usagePercentage: Double
        get() = if (quotaBytes != null && quotaBytes > 0) {
            (usedBytes.toDouble() / quotaBytes.toDouble()) * 100
        } else {
            0.0
        }
}
