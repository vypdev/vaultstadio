/**
 * Implementation of UpdateUserRoleUseCase.
 */

package com.vaultstadio.app.data.admin.usecase

import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.usecase.UpdateUserRoleUseCase
import com.vaultstadio.app.domain.auth.model.UserRole

class UpdateUserRoleUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserRoleUseCase {

    override suspend fun invoke(userId: String, role: UserRole) =
        adminRepository.updateUserRole(userId, role)
}
