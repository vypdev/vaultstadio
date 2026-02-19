/**
 * Use case for getting a single federated instance by domain.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.result.Result

interface GetFederatedInstanceUseCase {
    suspend operator fun invoke(domain: String): Result<FederatedInstance>
}
