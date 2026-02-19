package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.auth.usecase.GetQuotaUseCase
import com.vaultstadio.app.domain.result.Result

class GetQuotaUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetQuotaUseCase {

    override suspend operator fun invoke(): Result<StorageQuota> =
        authRepository.getQuota()
}
