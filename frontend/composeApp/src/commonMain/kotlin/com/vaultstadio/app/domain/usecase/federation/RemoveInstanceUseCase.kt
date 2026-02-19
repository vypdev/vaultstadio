/**
 * Remove Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import org.koin.core.annotation.Factory

/**
 * Use case for removing a federated instance.
 */
interface RemoveInstanceUseCase {
    suspend operator fun invoke(instanceId: String): ApiResult<Unit>
}

@Factory(binds = [RemoveInstanceUseCase::class])
class RemoveInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RemoveInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): ApiResult<Unit> =
        federationRepository.removeInstance(instanceId)
}
