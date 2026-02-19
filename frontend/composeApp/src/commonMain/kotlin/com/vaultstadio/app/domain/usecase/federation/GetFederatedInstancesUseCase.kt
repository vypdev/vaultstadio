/**
 * Get Federated Instances Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.InstanceStatus
import org.koin.core.annotation.Factory

/**
 * Use case for getting federated instances.
 */
interface GetFederatedInstancesUseCase {
    suspend operator fun invoke(status: InstanceStatus? = null): ApiResult<List<FederatedInstance>>
}

@Factory(binds = [GetFederatedInstancesUseCase::class])
class GetFederatedInstancesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstancesUseCase {

    override suspend operator fun invoke(status: InstanceStatus?): ApiResult<List<FederatedInstance>> =
        federationRepository.getInstances(status)
}
