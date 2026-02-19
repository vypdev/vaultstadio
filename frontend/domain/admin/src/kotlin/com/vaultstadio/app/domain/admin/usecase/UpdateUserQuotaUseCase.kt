/**
 * Use case for updating a user's storage quota (admin only).
 */

package com.vaultstadio.app.domain.admin.usecase

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.result.Result

interface UpdateUserQuotaUseCase {
    suspend operator fun invoke(userId: String, quotaBytes: Long?): Result<AdminUser>
}
