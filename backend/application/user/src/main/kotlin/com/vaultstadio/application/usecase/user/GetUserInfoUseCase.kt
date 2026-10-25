/**
 * Get User Info Use Case
 */

package com.vaultstadio.application.usecase.user

import arrow.core.Either
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.common.exception.StorageException

interface GetUserInfoUseCase {
    suspend operator fun invoke(userId: String): Either<StorageException, UserInfo>
}

class GetUserInfoUseCaseImpl(private val userService: UserService) : GetUserInfoUseCase {
    override suspend fun invoke(userId: String): Either<StorageException, UserInfo> =
        userService.getUserInfo(userId)
}
