/**
 * Revoke federated share use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.RevokeFederatedShareUseCase
import com.vaultstadio.app.domain.result.Result

class RevokeFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RevokeFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.revokeShare(shareId)
}
