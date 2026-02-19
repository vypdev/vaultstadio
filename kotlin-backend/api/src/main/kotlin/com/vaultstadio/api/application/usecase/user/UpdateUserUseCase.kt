/**
 * Update User Use Case
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.service.UpdateUserInput
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface UpdateUserUseCase {
    suspend operator fun invoke(input: UpdateUserInput): Either<StorageException, User>
}

class UpdateUserUseCaseImpl(private val userService: UserService) : UpdateUserUseCase {
    override suspend fun invoke(input: UpdateUserInput): Either<StorageException, User> =
        userService.updateUser(input)
}
