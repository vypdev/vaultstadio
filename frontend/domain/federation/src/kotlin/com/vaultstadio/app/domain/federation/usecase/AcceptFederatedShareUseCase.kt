/**
 * Use case for accepting an incoming federated share.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.result.Result

interface AcceptFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}
