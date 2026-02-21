/**
 * Get outgoing federated shares use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.usecase.GetOutgoingFederatedSharesUseCase
import com.vaultstadio.app.domain.result.Result

class GetOutgoingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetOutgoingFederatedSharesUseCase {

    override suspend operator fun invoke(): Result<List<FederatedShare>> =
        federationRepository.getOutgoingShares()
}
