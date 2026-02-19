/**
 * Get Outgoing Federated Shares Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedShare
/**
 * Use case for getting outgoing federated shares.
 */
interface GetOutgoingFederatedSharesUseCase {
    suspend operator fun invoke(): Result<List<FederatedShare>>
}

class GetOutgoingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetOutgoingFederatedSharesUseCase {

    override suspend operator fun invoke(): Result<List<FederatedShare>> =
        federationRepository.getOutgoingShares()
}
