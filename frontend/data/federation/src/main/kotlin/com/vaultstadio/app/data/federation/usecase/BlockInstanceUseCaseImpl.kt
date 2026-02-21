/**
 * Block instance use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.usecase.BlockInstanceUseCase
import com.vaultstadio.app.domain.result.Result

class BlockInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : BlockInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): Result<Unit> =
        federationRepository.blockInstance(instanceId)
}
