package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.usecase.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.result.Result

class GetActiveSessionsUseCaseImpl : GetActiveSessionsUseCase {

    override suspend operator fun invoke(): Result<List<ActiveSession>> =
        Result.success(emptyList())
}
