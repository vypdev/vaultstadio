/**
 * Remove instance use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.RemoveInstanceUseCase
import com.vaultstadio.app.domain.result.Result

class RemoveInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RemoveInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): Result<Unit> =
        federationRepository.removeInstance(instanceId)
}
