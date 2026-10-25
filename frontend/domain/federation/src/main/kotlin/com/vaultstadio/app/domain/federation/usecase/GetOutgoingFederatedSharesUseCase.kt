/**
 * Use case for getting outgoing federated shares.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.result.Result

interface GetOutgoingFederatedSharesUseCase {
    suspend operator fun invoke(): Result<List<FederatedShare>>
}
