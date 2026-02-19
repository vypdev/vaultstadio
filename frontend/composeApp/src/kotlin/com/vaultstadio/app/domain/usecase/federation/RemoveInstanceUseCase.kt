/**
 * Remove Instance Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.FederationRepository
/**
 * Use case for removing a federated instance.
 */
interface RemoveInstanceUseCase {
    suspend operator fun invoke(instanceId: String): Result<Unit>
}

class RemoveInstanceUseCaseImpl(
    private val federationRepository: FederationRepository,
) : RemoveInstanceUseCase {

    override suspend operator fun invoke(instanceId: String): Result<Unit> =
        federationRepository.removeInstance(instanceId)
}
