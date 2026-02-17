/**
 * Admin Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.AdminService
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.model.UserStatus
import org.koin.core.annotation.Single

/**
 * Repository interface for admin operations.
 */
interface AdminRepository {
    suspend fun getUsers(limit: Int = 50, offset: Int = 0): ApiResult<PaginatedResponse<AdminUser>>
    suspend fun updateUserQuota(userId: String, quotaBytes: Long?): ApiResult<AdminUser>
    suspend fun updateUserRole(userId: String, role: UserRole): ApiResult<AdminUser>
    suspend fun updateUserStatus(userId: String, status: UserStatus): ApiResult<AdminUser>
}

@Single(binds = [AdminRepository::class])
class AdminRepositoryImpl(
    private val adminService: AdminService,
) : AdminRepository {

    override suspend fun getUsers(limit: Int, offset: Int): ApiResult<PaginatedResponse<AdminUser>> =
        adminService.getUsers(limit, offset)

    override suspend fun updateUserQuota(userId: String, quotaBytes: Long?): ApiResult<AdminUser> =
        adminService.updateUserQuota(userId, quotaBytes)

    override suspend fun updateUserRole(userId: String, role: UserRole): ApiResult<AdminUser> =
        adminService.updateUserRole(userId, role)

    override suspend fun updateUserStatus(userId: String, status: UserStatus): ApiResult<AdminUser> =
        adminService.updateUserStatus(userId, status)
}
