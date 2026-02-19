/**
 * Create Federated Share Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedShare
import com.vaultstadio.app.domain.model.SharePermission
import org.koin.core.annotation.Factory

/**
 * Use case for creating a federated share.
 */
interface CreateFederatedShareUseCase {
    suspend operator fun invoke(
        itemId: String,
        targetInstance: String,
        targetUserId: String? = null,
        permissions: List<SharePermission> = listOf(SharePermission.READ),
        expiresInDays: Int? = null,
    ): ApiResult<FederatedShare>
}

@Factory(binds = [CreateFederatedShareUseCase::class])
class CreateFederatedShareUseCaseImpl(
    private val federationRepository: FederationRepository,
) : CreateFederatedShareUseCase {

    override suspend operator fun invoke(
        itemId: String,
        targetInstance: String,
        targetUserId: String?,
        permissions: List<SharePermission>,
        expiresInDays: Int?,
    ): ApiResult<FederatedShare> = federationRepository.createShare(
        itemId,
        targetInstance,
        targetUserId,
        permissions,
        expiresInDays,
    )
}
