/**
 * Admin Repository implementation
 */

package com.vaultstadio.app.data.admin.repository

import com.vaultstadio.app.data.admin.service.AdminService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole

class AdminRepositoryImpl(
    private val adminService: AdminService,
) : AdminRepository {

    override suspend fun getUsers(limit: Int, offset: Int) =
        adminService.getUsers(limit, offset).toResult()

    override suspend fun updateUserQuota(userId: String, quotaBytes: Long?) =
        adminService.updateUserQuota(userId, quotaBytes).toResult()

    override suspend fun updateUserRole(userId: String, role: UserRole) =
        adminService.updateUserRole(userId, role).toResult()

    override suspend fun updateUserStatus(userId: String, status: UserStatus) =
        adminService.updateUserStatus(userId, status).toResult()
}
