/**
 * Decline Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
/**
 * Use case for declining a federated share.
 */
interface DeclineFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}

class DeclineFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : DeclineFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.declineShare(shareId)
}
