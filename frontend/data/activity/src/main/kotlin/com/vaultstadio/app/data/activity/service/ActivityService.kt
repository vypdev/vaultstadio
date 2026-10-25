/**
 * Activity Service
 */

package com.vaultstadio.app.data.activity.service

import com.vaultstadio.app.data.activity.api.ActivityApi
import com.vaultstadio.app.data.activity.mapper.toActivityList
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.activity.model.Activity

class ActivityService(private val activityApi: ActivityApi) {

    suspend fun getRecentActivity(limit: Int = 20): ApiResult<List<Activity>> =
        activityApi.getRecentActivity(limit).map { it.toActivityList() }

    suspend fun getItemActivity(itemId: String, limit: Int = 20): ApiResult<List<Activity>> =
        activityApi.getItemActivity(itemId, limit).map { it.toActivityList() }
}
