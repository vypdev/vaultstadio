/**
 * Request Federation Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedInstance
import org.koin.core.annotation.Factory

/**
 * Use case for requesting federation with another instance.
 */
interface RequestFederationUseCase {
    suspend operator fun invoke(targetDomain: String, message: String? = null): ApiResult<FederatedInstance>
}

@Factory(binds = [RequestFederationUseCase::class])
class RequestFederationUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RequestFederationUseCase {

    override suspend operator fun invoke(targetDomain: String, message: String?): ApiResult<FederatedInstance> =
        federationRepository.requestFederation(targetDomain, message)
}
