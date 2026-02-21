package com.vaultstadio.app.data.auth.usecase

import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.usecase.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.result.Result

class GetActiveSessionsUseCaseImpl : GetActiveSessionsUseCase {

    override suspend operator fun invoke(): Result<List<ActiveSession>> =
        Result.success(emptyList())
}
