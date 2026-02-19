/**
 * List Users Use Case (admin)
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.repository.PagedResult
import com.vaultstadio.core.domain.repository.UserQuery
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface ListUsersUseCase {
    suspend operator fun invoke(adminId: String, query: UserQuery): Either<StorageException, PagedResult<User>>
}

class ListUsersUseCaseImpl(private val userService: UserService) : ListUsersUseCase {
    override suspend fun invoke(adminId: String, query: UserQuery): Either<StorageException, PagedResult<User>> =
        userService.listUsers(adminId, query)
}
