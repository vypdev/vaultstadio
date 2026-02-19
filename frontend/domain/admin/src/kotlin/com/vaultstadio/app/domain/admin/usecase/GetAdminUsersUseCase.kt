/**
 * Use case for getting all users (admin only).
 */

package com.vaultstadio.app.domain.admin.usecase

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.PaginatedResponse
import com.vaultstadio.app.domain.result.Result

interface GetAdminUsersUseCase {
    suspend operator fun invoke(limit: Int = 50, offset: Int = 0): Result<PaginatedResponse<AdminUser>>
}
