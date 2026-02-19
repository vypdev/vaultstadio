/**
 * Use case for updating a user's role (admin only).
 */

package com.vaultstadio.app.domain.admin.usecase

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.auth.model.UserRole
import com.vaultstadio.app.domain.result.Result

interface UpdateUserRoleUseCase {
    suspend operator fun invoke(userId: String, role: UserRole): Result<AdminUser>
}
