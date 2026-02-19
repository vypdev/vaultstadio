/**
 * Block Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for blocking a federated instance.
 */
interface BlockInstanceUseCase {
    suspend operator fun invoke(instanceId: String): Result<Unit>
}

@Factory(binds = [BlockInstanceUseCase::class])
class BlockInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : BlockInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): Result<Unit> =
        federationRepository.blockInstance(instanceId)
}
