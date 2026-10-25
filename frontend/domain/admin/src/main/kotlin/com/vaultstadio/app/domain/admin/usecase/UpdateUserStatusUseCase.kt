/**
 * Use case for updating a user's status (admin only).
 */

package com.vaultstadio.app.domain.admin.usecase

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.result.Result

interface UpdateUserStatusUseCase {
    suspend operator fun invoke(userId: String, status: UserStatus): Result<AdminUser>
}
