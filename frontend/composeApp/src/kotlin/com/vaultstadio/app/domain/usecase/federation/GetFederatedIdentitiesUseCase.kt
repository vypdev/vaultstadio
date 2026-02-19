/**
 * Get Federated Identities Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedIdentity
/**
 * Use case for getting federated identities.
 */
interface GetFederatedIdentitiesUseCase {
    suspend operator fun invoke(): Result<List<FederatedIdentity>>
}

class GetFederatedIdentitiesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedIdentitiesUseCase {

    override suspend operator fun invoke(): Result<List<FederatedIdentity>> =
        federationRepository.getIdentities()
}
