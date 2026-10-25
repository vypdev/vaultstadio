/**
 * Accept federated share use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.AcceptFederatedShareUseCase
import com.vaultstadio.app.domain.result.Result

class AcceptFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : AcceptFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.acceptShare(shareId)
}
