/**
 * Admin Service
 */

package com.vaultstadio.app.data.admin.service

import com.vaultstadio.app.data.admin.api.AdminApi
import com.vaultstadio.app.data.admin.mapper.toAdminUserPaginatedResponse
import com.vaultstadio.app.data.admin.mapper.toDomain
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.PaginatedResponse
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole

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
