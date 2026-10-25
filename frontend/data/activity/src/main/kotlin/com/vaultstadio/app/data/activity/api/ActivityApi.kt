/**
 * Activity API
 */

package com.vaultstadio.app.data.activity.api

import com.vaultstadio.app.data.activity.dto.ActivityDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient

class ActivityApi(client: HttpClient) : BaseApi(client) {

    suspend fun getRecentActivity(limit: Int = 20): ApiResult<List<ActivityDTO>> =
        get("/api/v1/activity", mapOf("limit" to limit.toString()))

    suspend fun getItemActivity(itemId: String, limit: Int = 20): ApiResult<List<ActivityDTO>> =
        get("/api/v1/activity/item/$itemId", mapOf("limit" to limit.toString()))
}
