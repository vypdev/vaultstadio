/**
 * Update User Quota Use Case
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import org.koin.core.annotation.Factory

/**
 * Use case for updating a user's storage quota (admin only).
 */
interface UpdateUserQuotaUseCase {
    suspend operator fun invoke(userId: String, quotaBytes: Long?): ApiResult<AdminUser>
}

@Factory(binds = [UpdateUserQuotaUseCase::class])
class UpdateUserQuotaUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserQuotaUseCase {

    override suspend operator fun invoke(userId: String, quotaBytes: Long?): ApiResult<AdminUser> =
        adminRepository.updateUserQuota(userId, quotaBytes)
}
