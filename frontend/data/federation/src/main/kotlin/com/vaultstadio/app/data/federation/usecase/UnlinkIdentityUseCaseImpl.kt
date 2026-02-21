/**
 * Unlink identity use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.UnlinkIdentityUseCase
import com.vaultstadio.app.domain.result.Result

class UnlinkIdentityUseCaseImpl(
    private val federationRepository: FederationRepository,
) : UnlinkIdentityUseCase {

    override suspend operator fun invoke(identityId: String): Result<Unit> =
        federationRepository.unlinkIdentity(identityId)
}
