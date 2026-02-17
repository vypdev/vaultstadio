/**
 * Sync API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.common.ApiResponseDTO
import com.vaultstadio.app.data.dto.sync.RegisterDeviceRequestDTO
import com.vaultstadio.app.data.dto.sync.SyncConflictDTO
import com.vaultstadio.app.data.dto.sync.SyncDeviceDTO
import com.vaultstadio.app.data.dto.sync.SyncRequestDTO
import com.vaultstadio.app.data.dto.sync.SyncResponseDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import org.koin.core.annotation.Single

@Single
class SyncApi(client: HttpClient) : BaseApi(client) {

    suspend fun registerDevice(request: RegisterDeviceRequestDTO): ApiResult<SyncDeviceDTO> =
        post("/api/v1/sync/devices", request)

    suspend fun getDevices(activeOnly: Boolean = true): ApiResult<List<SyncDeviceDTO>> =
        get("/api/v1/sync/devices", mapOf("activeOnly" to activeOnly.toString()))

    suspend fun deactivateDevice(deviceId: String): ApiResult<Map<String, String>> =
        postNoBody("/api/v1/sync/devices/$deviceId/deactivate")

    suspend fun removeDevice(deviceId: String): ApiResult<Unit> =
        delete("/api/v1/sync/devices/$deviceId")

    suspend fun pullChanges(
        deviceId: String,
        request: SyncRequestDTO,
    ): ApiResult<SyncResponseDTO> {
        return try {
            val response = client.request("/api/v1/sync/pull") {
                method = HttpMethod.Post
                header("X-Device-ID", deviceId)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponseDTO<SyncResponseDTO>>()
                if (apiResponse.success && apiResponse.data != null) {
                    ApiResult.Success(apiResponse.data)
                } else {
                    ApiResult.Error(
                        apiResponse.error?.code ?: "SYNC_ERROR",
                        apiResponse.error?.message ?: "Sync failed",
                    )
                }
            } else {
                ApiResult.Error("HTTP_${response.status.value}", response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Sync error")
        }
    }

    suspend fun getConflicts(): ApiResult<List<SyncConflictDTO>> =
        get("/api/v1/sync/conflicts")

    suspend fun resolveConflict(conflictId: String, resolution: String): ApiResult<Map<String, String>> =
        post("/api/v1/sync/conflicts/$conflictId/resolve", mapOf("resolution" to resolution))
}
