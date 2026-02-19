/**
 * Get Federated Instances Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedInstance
import com.vaultstadio.app.domain.model.InstanceStatus
/**
 * Use case for getting federated instances.
 */
interface GetFederatedInstancesUseCase {
    suspend operator fun invoke(status: InstanceStatus? = null): Result<List<FederatedInstance>>
}

class GetFederatedInstancesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstancesUseCase {

    override suspend operator fun invoke(status: InstanceStatus?): Result<List<FederatedInstance>> =
        federationRepository.getInstances(status)
}
