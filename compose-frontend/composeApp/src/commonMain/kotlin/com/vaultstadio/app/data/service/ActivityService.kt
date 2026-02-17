/**
 * Activity Service
 */

package com.vaultstadio.app.data.service

import com.vaultstadio.app.data.api.ActivityApi
import com.vaultstadio.app.data.mapper.toActivityList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.Activity
import org.koin.core.annotation.Single

@Single
class ActivityService(private val activityApi: ActivityApi) {

    suspend fun getRecentActivity(limit: Int = 20): ApiResult<List<Activity>> =
        activityApi.getRecentActivity(limit).map { it.toActivityList() }

    suspend fun getItemActivity(itemId: String, limit: Int = 20): ApiResult<List<Activity>> =
        activityApi.getItemActivity(itemId, limit).map { it.toActivityList() }
}
