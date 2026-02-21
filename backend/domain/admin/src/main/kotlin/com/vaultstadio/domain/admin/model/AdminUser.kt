package com.vaultstadio.domain.admin.model

import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Admin view of a user (for list/management).
 */
@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus,
    val quotaBytes: Long?,
    val usedBytes: Long = 0,
    val createdAt: Instant,
)
