/**
 * Get federated activities use case implementation.
 */

package com.vaultstadio.app.data.federation.usecase

import com.vaultstadio.app.domain.federation.FederationRepository
import com.vaultstadio.app.domain.federation.model.FederatedActivity
import com.vaultstadio.app.domain.federation.usecase.GetFederatedActivitiesUseCase
import com.vaultstadio.app.domain.result.Result
import kotlinx.datetime.Instant

class GetFederatedActivitiesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedActivitiesUseCase {

    override suspend operator fun invoke(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): Result<List<FederatedActivity>> =
        federationRepository.getActivities(instance, since, limit)
}
