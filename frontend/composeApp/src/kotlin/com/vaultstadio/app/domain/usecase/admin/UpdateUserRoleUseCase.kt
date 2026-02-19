/**
 * Update User Role Use Case
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.auth.model.UserRole
/**
 * Use case for updating a user's role (admin only).
 */
interface UpdateUserRoleUseCase {
    suspend operator fun invoke(userId: String, role: UserRole): Result<AdminUser>
}

class UpdateUserRoleUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserRoleUseCase {

    override suspend operator fun invoke(userId: String, role: UserRole): Result<AdminUser> =
        adminRepository.updateUserRole(userId, role)
}
