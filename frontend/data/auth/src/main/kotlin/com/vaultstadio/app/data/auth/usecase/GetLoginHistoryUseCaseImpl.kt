package com.vaultstadio.app.data.auth.usecase

import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.usecase.GetLoginHistoryUseCase
import com.vaultstadio.app.domain.result.Result

class GetLoginHistoryUseCaseImpl : GetLoginHistoryUseCase {

    override suspend operator fun invoke(): Result<List<LoginEvent>> =
        Result.success(emptyList())
}
