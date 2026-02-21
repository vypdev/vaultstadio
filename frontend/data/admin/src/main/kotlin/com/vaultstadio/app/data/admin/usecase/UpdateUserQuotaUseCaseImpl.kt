/**
 * Implementation of UpdateUserQuotaUseCase.
 */

package com.vaultstadio.app.data.admin.usecase

import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.usecase.UpdateUserQuotaUseCase

class UpdateUserQuotaUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserQuotaUseCase {

    override suspend fun invoke(userId: String, quotaBytes: Long?) =
        adminRepository.updateUserQuota(userId, quotaBytes)
}
