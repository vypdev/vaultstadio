/**
 * Get Quota Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.AuthRepository
import com.vaultstadio.app.domain.model.StorageQuota
import org.koin.core.annotation.Factory

/**
 * Use case for getting storage quota.
 */
interface GetQuotaUseCase {
    suspend operator fun invoke(): ApiResult<StorageQuota>
}

@Factory(binds = [GetQuotaUseCase::class])
class GetQuotaUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetQuotaUseCase {

    override suspend operator fun invoke(): ApiResult<StorageQuota> =
        authRepository.getQuota()
}
