/**
 * Link Identity Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedIdentity
/**
 * Use case for linking a federated identity from another instance.
 */
interface LinkIdentityUseCase {
    suspend operator fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): Result<FederatedIdentity>
}

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
