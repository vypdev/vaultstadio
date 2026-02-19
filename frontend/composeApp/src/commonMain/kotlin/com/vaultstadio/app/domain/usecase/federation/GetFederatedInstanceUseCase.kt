/**
 * Get Federated Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedInstance
import org.koin.core.annotation.Factory

/**
 * Use case for getting details of a specific federated instance by domain.
 */
interface GetFederatedInstanceUseCase {
    suspend operator fun invoke(domain: String): ApiResult<FederatedInstance>
}

@Factory(binds = [GetFederatedInstanceUseCase::class])
class GetFederatedInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstanceUseCase {

    override suspend operator fun invoke(domain: String): ApiResult<FederatedInstance> =
        federationRepository.getInstance(domain)
}
