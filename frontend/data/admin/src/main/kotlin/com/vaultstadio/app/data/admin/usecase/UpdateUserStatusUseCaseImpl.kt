/**
 * Implementation of UpdateUserStatusUseCase.
 */

package com.vaultstadio.app.data.admin.usecase

import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.admin.usecase.UpdateUserStatusUseCase

class UpdateUserStatusUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserStatusUseCase {

    override suspend fun invoke(userId: String, status: UserStatus) =
        adminRepository.updateUserStatus(userId, status)
}
