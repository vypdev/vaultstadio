/**
 * Decline federated share use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.DeclineFederatedShareUseCase
import com.vaultstadio.app.domain.result.Result

class DeclineFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : DeclineFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.declineShare(shareId)
}
