/**
 * List Users Use Case (admin)
 */

package com.vaultstadio.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.repository.UserQuery
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult

interface ListUsersUseCase {
    suspend operator fun invoke(adminId: String, query: UserQuery): Either<StorageException, PagedResult<User>>
}

class ListUsersUseCaseImpl(private val userService: UserService) : ListUsersUseCase {
    override suspend fun invoke(adminId: String, query: UserQuery): Either<StorageException, PagedResult<User>> =
        userService.listUsers(adminId, query)
}
