/**
 * Use case for getting linked federated identities.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.result.Result

interface GetFederatedIdentitiesUseCase {
    suspend operator fun invoke(): Result<List<FederatedIdentity>>
}
