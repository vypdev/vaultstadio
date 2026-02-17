/**
 * Link Identity Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedIdentity
import org.koin.core.annotation.Factory

/**
 * Use case for linking a federated identity from another instance.
 */
interface LinkIdentityUseCase {
    suspend operator fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): ApiResult<FederatedIdentity>
}

@Factory(binds = [LinkIdentityUseCase::class])
class LinkIdentityUseCaseImpl(
    private val federationRepository: FederationRepository,
) : LinkIdentityUseCase {

    override suspend operator fun invoke(
        remoteUserId: String,
        remoteInstance: String,
        displayName: String,
    ): ApiResult<FederatedIdentity> =
        federationRepository.linkIdentity(remoteUserId, remoteInstance, displayName)
}
