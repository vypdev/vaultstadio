/**
 * Admin Data Transfer Objects
 */

package com.vaultstadio.app.data.admin.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDTO(
    val id: String,
    val email: String,
    val username: String,
    val role: String,
    val status: String,
    val avatarUrl: String?,
    val quotaBytes: Long?,
    val usedBytes: Long,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
    @kotlinx.serialization.Contextual
    val lastLoginAt: Instant?,
)

@Serializable
data class UpdateQuotaRequestDTO(val quotaBytes: Long?)

@Serializable
data class UpdateRoleRequestDTO(val role: String)

@Serializable
data class UpdateStatusRequestDTO(val status: String)
