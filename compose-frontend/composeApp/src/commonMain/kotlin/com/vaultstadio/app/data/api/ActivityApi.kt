/**
 * Activity API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.activity.ActivityDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
class ActivityApi(client: HttpClient) : BaseApi(client) {

    suspend fun getRecentActivity(limit: Int = 20): ApiResult<List<ActivityDTO>> =
        get("/api/v1/activity", mapOf("limit" to limit.toString()))

    suspend fun getItemActivity(itemId: String, limit: Int = 20): ApiResult<List<ActivityDTO>> =
        get("/api/v1/activity/item/$itemId", mapOf("limit" to limit.toString()))
}
