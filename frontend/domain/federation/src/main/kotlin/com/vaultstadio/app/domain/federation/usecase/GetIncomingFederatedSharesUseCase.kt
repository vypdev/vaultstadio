/**
 * Use case for getting incoming federated shares.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.FederatedShareStatus
import com.vaultstadio.app.domain.result.Result

interface GetIncomingFederatedSharesUseCase {
    suspend operator fun invoke(status: FederatedShareStatus? = null): Result<List<FederatedShare>>
}
