/**
 * Use case for getting federated activities.
 */

package com.vaultstadio.app.domain.federation.usecase

import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.result.Result
import kotlinx.datetime.Instant

interface GetFederatedActivitiesUseCase {
    suspend operator fun invoke(
        instance: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): Result<List<FederatedActivity>>
}
