/**
 * Update User Status Use Case
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.UserStatus
/**
 * Use case for updating a user's status (admin only).
 */
interface UpdateUserStatusUseCase {
    suspend operator fun invoke(userId: String, status: UserStatus): Result<AdminUser>
}

class UpdateUserStatusUseCaseImpl(
    private val adminRepository: AdminRepository,
) : UpdateUserStatusUseCase {

    override suspend operator fun invoke(userId: String, status: UserStatus): Result<AdminUser> =
        adminRepository.updateUserStatus(userId, status)
}
