/**
 * Get User Info Use Case
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.model.UserInfo
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.core.exception.StorageException

interface GetUserInfoUseCase {
    suspend operator fun invoke(userId: String): Either<StorageException, UserInfo>
}

class GetUserInfoUseCaseImpl(private val userService: UserService) : GetUserInfoUseCase {
    override suspend fun invoke(userId: String): Either<StorageException, UserInfo> =
        userService.getUserInfo(userId)
}
