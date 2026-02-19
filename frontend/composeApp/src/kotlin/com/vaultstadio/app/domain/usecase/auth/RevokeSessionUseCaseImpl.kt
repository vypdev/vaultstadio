package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.usecase.RevokeSessionUseCase
import com.vaultstadio.app.domain.result.Result

class RevokeSessionUseCaseImpl : RevokeSessionUseCase {

    override suspend operator fun invoke(sessionId: String): Result<Unit> =
        Result.error("NOT_IMPLEMENTED", "Session management not yet available")
}
