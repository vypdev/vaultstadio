/**
 * Get Incoming Federated Shares Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedShare
/**
 * Use case for getting incoming federated shares.
 */
interface GetIncomingFederatedSharesUseCase {
    suspend operator fun invoke(): Result<List<FederatedShare>>
}

class GetIncomingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetIncomingFederatedSharesUseCase {

    override suspend operator fun invoke(): Result<List<FederatedShare>> =
        federationRepository.getIncomingShares()
}
