/**
 * Implementation of GetAdminUsersUseCase.
 */

package com.vaultstadio.app.data.admin.usecase

import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.usecase.GetAdminUsersUseCase

class GetAdminUsersUseCaseImpl(
    private val adminRepository: AdminRepository,
) : GetAdminUsersUseCase {

    override suspend fun invoke(limit: Int, offset: Int) = adminRepository.getUsers(limit, offset)
}
