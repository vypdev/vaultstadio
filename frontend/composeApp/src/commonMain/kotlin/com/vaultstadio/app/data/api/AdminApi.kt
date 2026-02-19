/**
 * Admin API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.admin.AdminUserDTO
import com.vaultstadio.app.data.dto.admin.UpdateQuotaRequestDTO
import com.vaultstadio.app.data.dto.admin.UpdateRoleRequestDTO
import com.vaultstadio.app.data.dto.admin.UpdateStatusRequestDTO
import com.vaultstadio.app.data.dto.common.PaginatedResponseDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
class AdminApi(client: HttpClient) : BaseApi(client) {

    suspend fun getUsers(limit: Int = 50, offset: Int = 0): ApiResult<PaginatedResponseDTO<AdminUserDTO>> =
        get("/api/v1/admin/users", mapOf("limit" to limit.toString(), "offset" to offset.toString()))

    suspend fun updateUserQuota(userId: String, quotaBytes: Long?): ApiResult<AdminUserDTO> =
        patch("/api/v1/admin/users/$userId/quota", UpdateQuotaRequestDTO(quotaBytes))

    suspend fun updateUserRole(userId: String, role: String): ApiResult<AdminUserDTO> =
        patch("/api/v1/admin/users/$userId/role", UpdateRoleRequestDTO(role))

    suspend fun updateUserStatus(userId: String, status: String): ApiResult<AdminUserDTO> =
        patch("/api/v1/admin/users/$userId/status", UpdateStatusRequestDTO(status))
}
