/**
 * Get Federated Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedInstance
import org.koin.core.annotation.Factory

/**
 * Use case for getting details of a specific federated instance by domain.
 */
interface GetFederatedInstanceUseCase {
    suspend operator fun invoke(domain: String): Result<FederatedInstance>
}

@Factory(binds = [GetFederatedInstanceUseCase::class])
class GetFederatedInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstanceUseCase {

    override suspend operator fun invoke(domain: String): Result<FederatedInstance> =
        federationRepository.getInstance(domain)
}
