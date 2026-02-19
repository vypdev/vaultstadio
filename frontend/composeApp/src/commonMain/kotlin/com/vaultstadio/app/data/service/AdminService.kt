/**
 * Admin Service
 */

package com.vaultstadio.app.data.service

import com.vaultstadio.app.data.api.AdminApi
import com.vaultstadio.app.data.mapper.toAdminUserPaginatedResponse
import com.vaultstadio.app.data.mapper.toDomain
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import org.koin.core.annotation.Single

@Single
class AdminService(private val adminApi: AdminApi) {

    suspend fun getUsers(limit: Int = 50, offset: Int = 0): ApiResult<PaginatedResponse<AdminUser>> =
        adminApi.getUsers(limit, offset).map { it.toAdminUserPaginatedResponse() }

    suspend fun updateUserQuota(userId: String, quotaBytes: Long?): ApiResult<AdminUser> =
        adminApi.updateUserQuota(userId, quotaBytes).map { it.toDomain() }

    suspend fun updateUserRole(userId: String, role: UserRole): ApiResult<AdminUser> =
        adminApi.updateUserRole(userId, role.name).map { it.toDomain() }

    suspend fun updateUserStatus(userId: String, status: UserStatus): ApiResult<AdminUser> =
        adminApi.updateUserStatus(userId, status.name).map { it.toDomain() }
}
