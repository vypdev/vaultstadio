/**
 * Get Admin Users Use Case
 */

package com.vaultstadio.app.domain.usecase.admin

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.AdminRepository
import com.vaultstadio.app.domain.model.AdminUser
import com.vaultstadio.app.domain.model.PaginatedResponse
import org.koin.core.annotation.Factory

/**
 * Use case for getting all users (admin only).
 */
interface GetAdminUsersUseCase {
    suspend operator fun invoke(limit: Int = 50, offset: Int = 0): Result<PaginatedResponse<AdminUser>>
}

@Factory(binds = [GetAdminUsersUseCase::class])
class GetAdminUsersUseCaseImpl(
    private val adminRepository: AdminRepository,
) : GetAdminUsersUseCase {

    override suspend operator fun invoke(limit: Int, offset: Int): Result<PaginatedResponse<AdminUser>> =
        adminRepository.getUsers(limit, offset)
}
