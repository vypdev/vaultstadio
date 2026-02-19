/**
 * Get Recent Activity Use Case
 */

package com.vaultstadio.app.domain.usecase.activity

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.ActivityRepository
import com.vaultstadio.app.domain.model.Activity
import org.koin.core.annotation.Factory

/**
 * Use case for getting recent user activity.
 */
interface GetRecentActivityUseCase {
    suspend operator fun invoke(limit: Int = 20): ApiResult<List<Activity>>
}

@Factory(binds = [GetRecentActivityUseCase::class])
class GetRecentActivityUseCaseImpl(
    private val activityRepository: ActivityRepository,
) : GetRecentActivityUseCase {

    override suspend operator fun invoke(limit: Int): ApiResult<List<Activity>> =
        activityRepository.getRecentActivity(limit)
}
