/**
 * Activity Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.ActivityService
import com.vaultstadio.app.domain.model.Activity
import org.koin.core.annotation.Single

/**
 * Repository interface for activity operations.
 */
interface ActivityRepository {
    suspend fun getRecentActivity(limit: Int = 20): ApiResult<List<Activity>>
    suspend fun getItemActivity(itemId: String, limit: Int = 20): ApiResult<List<Activity>>
}

@Single(binds = [ActivityRepository::class])
class ActivityRepositoryImpl(
    private val activityService: ActivityService,
) : ActivityRepository {

    override suspend fun getRecentActivity(limit: Int): ApiResult<List<Activity>> =
        activityService.getRecentActivity(limit)

    override suspend fun getItemActivity(itemId: String, limit: Int): ApiResult<List<Activity>> =
        activityService.getItemActivity(itemId, limit)
}
