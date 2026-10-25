/**
 * Get incoming federated shares use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.federation.usecase.GetIncomingFederatedSharesUseCase
import com.vaultstadio.app.domain.result.Result

class GetIncomingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetIncomingFederatedSharesUseCase {

    override suspend operator fun invoke(status: FederatedShareStatus?): Result<List<FederatedShare>> =
        federationRepository.getIncomingShares(status)
}
