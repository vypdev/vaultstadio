/**
 * Delete User Use Case (admin)
 */

package com.vaultstadio.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.StorageException

interface DeleteUserUseCase {
    suspend operator fun invoke(userId: String, adminId: String): Either<StorageException, Unit>
}

class DeleteUserUseCaseImpl(private val userService: UserService) : DeleteUserUseCase {
    override suspend fun invoke(userId: String, adminId: String): Either<StorageException, Unit> =
        userService.deleteUser(userId, adminId)
}
