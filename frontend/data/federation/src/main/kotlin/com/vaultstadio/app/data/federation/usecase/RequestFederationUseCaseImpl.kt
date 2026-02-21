/**
 * Request federation use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.federation.usecase.RequestFederationUseCase
import com.vaultstadio.app.domain.result.Result

class RequestFederationUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RequestFederationUseCase {

    override suspend operator fun invoke(targetDomain: String, message: String?): Result<FederatedInstance> =
        federationRepository.requestFederation(targetDomain, message)
}
