/**
 * Decline Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for declining a federated share.
 */
interface DeclineFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): ApiResult<Unit>
}

@Factory(binds = [DeclineFederatedShareUseCase::class])
class DeclineFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : DeclineFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): ApiResult<Unit> =
        federationRepository.declineShare(shareId)
}
