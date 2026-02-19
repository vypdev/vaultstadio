/**
 * Get Incoming Federated Shares Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedShare
import org.koin.core.annotation.Factory

/**
 * Use case for getting incoming federated shares.
 */
interface GetIncomingFederatedSharesUseCase {
    suspend operator fun invoke(): ApiResult<List<FederatedShare>>
}

@Factory(binds = [GetIncomingFederatedSharesUseCase::class])
class GetIncomingFederatedSharesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetIncomingFederatedSharesUseCase {

    override suspend operator fun invoke(): ApiResult<List<FederatedShare>> =
        federationRepository.getIncomingShares()
}
