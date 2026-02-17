/**
 * Update User Role Use Case
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.UserRole
import org.koin.core.annotation.Factory

/**
 * Use case for updating a user's role (admin only).
 */
interface UpdateUserRoleUseCase {
    suspend operator fun invoke(userId: String, role: UserRole): ApiResult<AdminUser>
}

@Factory(binds = [UpdateUserRoleUseCase::class])
class UpdateUserRoleUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserRoleUseCase {

    override suspend operator fun invoke(userId: String, role: UserRole): ApiResult<AdminUser> =
        adminRepository.updateUserRole(userId, role)
}
