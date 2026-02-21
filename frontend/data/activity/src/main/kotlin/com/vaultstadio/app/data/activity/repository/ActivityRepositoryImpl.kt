/**
 * Activity Repository implementation
 */

package com.vaultstadio.app.data.activity.repository

import com.vaultstadio.app.data.activity.service.ActivityService
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.domain.activity.ActivityRepository

class ActivityRepositoryImpl(
    private val activityService: ActivityService,
) : ActivityRepository {

    override suspend fun getRecentActivity(limit: Int) =
        activityService.getRecentActivity(limit).toResult()

    override suspend fun getItemActivity(itemId: String, limit: Int) =
        activityService.getItemActivity(itemId, limit).toResult()
}
