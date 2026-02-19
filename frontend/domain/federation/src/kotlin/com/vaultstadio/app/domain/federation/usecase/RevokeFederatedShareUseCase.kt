/**
 * Use case for revoking a federated share.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.result.Result

interface RevokeFederatedShareUseCase {
    suspend operator fun invoke(shareId: String): Result<Unit>
}
