/**
 * Use case for removing a federated instance.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.result.Result

interface RemoveInstanceUseCase {
    suspend operator fun invoke(instanceId: String): Result<Unit>
}
