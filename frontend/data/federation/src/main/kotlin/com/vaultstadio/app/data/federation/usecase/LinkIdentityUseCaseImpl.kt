/**
 * Link identity use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedIdentity
import com.vaultstadio.app.domain.federation.usecase.LinkIdentityUseCase
import com.vaultstadio.app.domain.result.Result

class LinkIdentityUseCaseImpl(
    private val federationRepository: FederationRepository,
) : LinkIdentityUseCase {

    override suspend operator fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity> =
        federationRepository.linkIdentity(remoteUserId, remoteInstance, displayName)
}
