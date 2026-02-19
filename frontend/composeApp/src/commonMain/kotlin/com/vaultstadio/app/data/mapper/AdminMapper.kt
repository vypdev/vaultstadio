/**
 * Admin Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.admin.AdminUserDTO
import com.vaultstadio.app.data.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus

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
