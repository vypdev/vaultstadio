/**
 * Admin API
 */

package com.vaultstadio.app.data.admin.api

import com.vaultstadio.app.data.admin.dto.AdminUserDTO
import com.vaultstadio.app.data.admin.dto.UpdateQuotaRequestDTO
import com.vaultstadio.app.data.admin.dto.UpdateRoleRequestDTO
import com.vaultstadio.app.data.admin.dto.UpdateStatusRequestDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import com.vaultstadio.app.data.network.dto.common.PaginatedResponseDTO
import io.ktor.client.HttpClient

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
