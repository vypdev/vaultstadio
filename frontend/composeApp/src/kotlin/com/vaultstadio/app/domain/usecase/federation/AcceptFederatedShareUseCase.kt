/**
 * Accept Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
/**
 * Use case for accepting a federated share.
 */
interface AcceptFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}

class AcceptFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : AcceptFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.acceptShare(shareId)
}
