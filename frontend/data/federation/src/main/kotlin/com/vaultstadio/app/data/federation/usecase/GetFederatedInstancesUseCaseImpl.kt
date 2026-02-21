/**
 * Get federated instances use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.federation.usecase.GetFederatedInstancesUseCase
import com.vaultstadio.app.domain.result.Result

class GetFederatedInstancesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedInstancesUseCase {

    override suspend operator fun invoke(status: InstanceStatus?): Result<List<FederatedInstance>> =
        federationRepository.getInstances(status)
}
