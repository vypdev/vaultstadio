/**
 * Accept Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for accepting a federated share.
 */
interface AcceptFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}

@Factory(binds = [AcceptFederatedShareUseCase::class])
class AcceptFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : AcceptFederatedShareUseCase {

    override suspend operator fun invoke(shareId: String): Result<Unit> =
        federationRepository.acceptShare(shareId)
}
