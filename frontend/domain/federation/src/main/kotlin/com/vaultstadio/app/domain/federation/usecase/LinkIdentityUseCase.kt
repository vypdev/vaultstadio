/**
 * Use case for linking a federated identity from another instance.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.result.Result

interface LinkIdentityUseCase {
    suspend operator fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity>
}
