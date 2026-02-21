/**
 * Refresh Session Use Case
 *
 * Application use case for refreshing an auth session.
 */

package com.vaultstadio.application.usecase.auth

import arrow.core.Either
import com.vaultstadio.core.domain.service.RefreshResult
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.StorageException

interface RefreshSessionUseCase {
    suspend operator fun invoke(refreshToken: String): Either<StorageException, RefreshResult>
}

class RefreshSessionUseCaseImpl(private val userService: UserService) : RefreshSessionUseCase {
    override suspend fun invoke(refreshToken: String): Either<StorageException, RefreshResult> =
        userService.refreshSession(refreshToken)
}
