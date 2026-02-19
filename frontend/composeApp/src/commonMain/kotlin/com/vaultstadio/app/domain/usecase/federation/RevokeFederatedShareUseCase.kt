/**
 * Revoke Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for revoking a federated share.
 */
interface RevokeFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): ApiResult<Unit>
}

@Factory(binds = [RevokeFederatedShareUseCase::class])
class RevokeFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RevokeFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): ApiResult<Unit> =
        federationRepository.revokeShare(shareId)
}
