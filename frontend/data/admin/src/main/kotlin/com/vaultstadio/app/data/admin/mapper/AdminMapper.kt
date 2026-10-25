/**
 * Admin Mappers
 */

package com.vaultstadio.app.data.admin.mapper

import com.vaultstadio.app.data.admin.dto.AdminUserDTO
import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.PaginatedResponse
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole

fun AdminUserDTO.toDomain(): AdminUser = AdminUser(
    id = id,
    email = email,
    username = username,
    role = try {
        UserRole.valueOf(role.uppercase())
    } catch (e: Exception) {
        UserRole.USER
    },
    status = try {
        UserStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        UserStatus.ACTIVE
    },
    avatarUrl = avatarUrl,
    quotaBytes = quotaBytes,
    usedBytes = usedBytes,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
)

fun PaginatedResponseDTO<AdminUserDTO>.toAdminUserPaginatedResponse(): PaginatedResponse<AdminUser> =
    PaginatedResponse(
        items = items.map { it.toDomain() },
        total = total,
        page = page,
        pageSize = pageSize,
        totalPages = totalPages,
        hasMore = hasMore,
    )
