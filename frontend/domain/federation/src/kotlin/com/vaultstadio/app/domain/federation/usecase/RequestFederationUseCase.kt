/**
 * Use case for requesting federation with another instance.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedInstance
import com.vaultstadio.app.domain.result.Result

interface RequestFederationUseCase {
    suspend operator fun invoke(targetDomain: String, message: String? = null): Result<FederatedInstance>
}
