/**
 * Get Outgoing Federated Shares Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedShare
import org.koin.core.annotation.Factory

/**
 * Use case for getting outgoing federated shares.
 */
interface GetOutgoingFederatedSharesUseCase {
    suspend operator fun invoke(): ApiResult<List<FederatedShare>>
}

@Factory(binds = [GetOutgoingFederatedSharesUseCase::class])
class GetOutgoingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetOutgoingFederatedSharesUseCase {

    override suspend operator fun invoke(): ApiResult<List<FederatedShare>> =
        federationRepository.getOutgoingShares()
}
