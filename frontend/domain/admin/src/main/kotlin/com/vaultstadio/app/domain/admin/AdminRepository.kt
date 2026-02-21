/**
 * Repository interface for admin operations.
 */

package com.vaultstadio.app.domain.admin

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.PaginatedResponse
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.result.Result

interface AdminRepository {
    suspend fun getUsers(limit: Int = 50, offset: Int = 0): Result<PaginatedResponse<AdminUser>>
    suspend fun updateUserQuota(userId: String, quotaBytes: Long?): Result<AdminUser>
    suspend fun updateUserRole(userId: String, role: UserRole): Result<AdminUser>
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<AdminUser>
}
