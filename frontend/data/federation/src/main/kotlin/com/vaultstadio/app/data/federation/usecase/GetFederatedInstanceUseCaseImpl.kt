/**
 * Get federated instance use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstanceUseCase
import com.vaultstadio.app.domain.result.Result

class GetFederatedInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstanceUseCase {

    override suspend operator fun invoke(domain: String): Result<FederatedInstance> =
        federationRepository.getInstance(domain)
}
