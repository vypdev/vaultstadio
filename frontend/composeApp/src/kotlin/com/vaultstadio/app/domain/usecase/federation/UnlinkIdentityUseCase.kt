/**
 * Unlink Identity Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
/**
 * Use case for unlinking a federated identity.
 */
interface UnlinkIdentityUseCase {
    suspend operator fun invoke(identityId: String): Result<Unit>
}

class UnlinkIdentityUseCaseImpl(
    private val federationRepository: FederationRepository,
) : UnlinkIdentityUseCase {

    override suspend operator fun invoke(identityId: String): Result<Unit> =
        federationRepository.unlinkIdentity(identityId)
}
