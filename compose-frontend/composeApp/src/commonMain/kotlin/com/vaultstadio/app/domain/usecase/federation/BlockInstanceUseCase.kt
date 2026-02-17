/**
 * Block Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for blocking a federated instance.
 */
interface BlockInstanceUseCase {
    suspend operator fun invoke(instanceId: String): ApiResult<Unit>
}

@Factory(binds = [BlockInstanceUseCase::class])
class BlockInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : BlockInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): ApiResult<Unit> =
        federationRepository.blockInstance(instanceId)
}
