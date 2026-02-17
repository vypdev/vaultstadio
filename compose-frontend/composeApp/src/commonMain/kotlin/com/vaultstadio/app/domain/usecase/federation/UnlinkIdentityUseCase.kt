/**
 * Unlink Identity Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for unlinking a federated identity.
 */
interface UnlinkIdentityUseCase {
    suspend operator fun invoke(identityId: String): ApiResult<Unit>
}

@Factory(binds = [UnlinkIdentityUseCase::class])
class UnlinkIdentityUseCaseImpl(
    private val federationRepository: FederationRepository,
) : UnlinkIdentityUseCase {

    override suspend operator fun invoke(identityId: String): ApiResult<Unit> =
        federationRepository.unlinkIdentity(identityId)
}
