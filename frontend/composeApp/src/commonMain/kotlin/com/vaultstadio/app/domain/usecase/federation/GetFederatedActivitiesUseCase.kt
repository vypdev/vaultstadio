/**
 * Get Federated Activities Use Case
 */

package com.vaultstadio.app.domain.usecase.federation

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.FederationRepository
import com.vaultstadio.app.domain.model.FederatedActivity
import kotlinx.datetime.Instant
import org.koin.core.annotation.Factory

/**
 * Use case for getting federated activities.
 */
interface GetFederatedActivitiesUseCase {
    suspend operator fun invoke(
        instance: String? = null,
        since: Instant? = null,
        limit: Int = 100,
    ): ApiResult<List<FederatedActivity>>
}

@Factory(binds = [GetFederatedActivitiesUseCase::class])
class GetFederatedActivitiesUseCaseImpl(
    private val federationRepository: FederationRepository,
) : GetFederatedActivitiesUseCase {

    override suspend operator fun invoke(
        instance: String?,
        since: Instant?,
        limit: Int,
    ): ApiResult<List<FederatedActivity>> = federationRepository.getActivities(instance, since, limit)
}
