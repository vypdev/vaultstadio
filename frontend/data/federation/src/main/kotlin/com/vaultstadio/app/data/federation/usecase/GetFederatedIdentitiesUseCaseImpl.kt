/**
 * Get federated identities use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.usecase.GetFederatedIdentitiesUseCase
import com.vaultstadio.app.domain.result.Result

class GetFederatedIdentitiesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedIdentitiesUseCase {

    override suspend operator fun invoke(): Result<List<FederatedIdentity>> =
        federationRepository.getIdentities()
}
