/**
 * Use case for blocking a federated instance.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.result.Result

interface BlockInstanceUseCase {
    suspend operator fun invoke(instanceId: String): Result<Unit>
}
