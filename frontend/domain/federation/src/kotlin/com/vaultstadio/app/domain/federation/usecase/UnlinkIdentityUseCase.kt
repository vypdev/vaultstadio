/**
 * Use case for unlinking a federated identity.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.result.Result

interface UnlinkIdentityUseCase {
    suspend operator fun invoke(identityId: String): Result<Unit>
}
