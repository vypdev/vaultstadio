/**
 * Use case for creating a federated share.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedShare
import com.vaultstadio.app.domain.federation.model.SharePermission
import com.vaultstadio.app.domain.result.Result

interface CreateFederatedShareUseCase {
    suspend operator fun invoke(
        itemId: String,
        targetInstance: String,
        targetUserId: String? = null,
        permissions: List<SharePermission> = listOf(SharePermission.READ),
        expiresInDays: Int? = null,
    ): Result<FederatedShare>
}
