/**
 * Update User Use Case
 */

package com.vaultstadio.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UpdateUserInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.common.exception.StorageException

interface UpdateUserUseCase {
    suspend operator fun invoke(input: UpdateUserInput): Either<StorageException, User>
}

class UpdateUserUseCaseImpl(private val userService: UserService) : UpdateUserUseCase {
    override suspend fun invoke(input: UpdateUserInput): Either<StorageException, User> =
        userService.updateUser(input)
}
