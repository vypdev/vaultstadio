/**
 * Use case for getting federated instances.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.model.InstanceStatus
import com.vaultstadio.app.domain.result.Result

interface GetFederatedInstancesUseCase {
    suspend operator fun invoke(status: InstanceStatus? = null): Result<List<FederatedInstance>>
}
