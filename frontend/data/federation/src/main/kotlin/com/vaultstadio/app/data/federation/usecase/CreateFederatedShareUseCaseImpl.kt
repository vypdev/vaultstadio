/**
 * Create federated share use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.federation.usecase.CreateFederatedShareUseCase
import com.vaultstadio.app.domain.result.Result

class CreateFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : CreateFederatedShareUseCase {

    override suspend operator fun invoke(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): Result<FederatedShare> =
        federationRepository.createShare(
            itemId,
            targetInstance,
            targetUserId,
            permissions,
            expiresInDays,
        )
}
