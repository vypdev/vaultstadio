/**
 * Version API
 */

package com.vaultstadio.app.data.api

import com.vaultstadio.app.data.dto.version.FileVersionDTO
import com.vaultstadio.app.data.dto.version.FileVersionHistoryDTO
import com.vaultstadio.app.data.dto.version.RestoreVersionRequestDTO
import com.vaultstadio.app.data.dto.version.VersionDiffDTO
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.network.BaseApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single

@Single
class VersionApi(client: HttpClient) : BaseApi(client) {

    suspend fun getVersionHistory(itemId: String): ApiResult<FileVersionHistoryDTO> =
        get("/api/v1/versions/item/$itemId")

    suspend fun getVersion(itemId: String, versionNumber: Int): ApiResult<FileVersionDTO> =
        get("/api/v1/versions/item/$itemId/version/$versionNumber")

    suspend fun restoreVersion(
        itemId: String,
        request: RestoreVersionRequestDTO,
    ): ApiResult<Map<String, String>> =
        post("/api/v1/versions/item/$itemId/restore", request)

    suspend fun compareVersions(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ): ApiResult<VersionDiffDTO> =
        get(
            "/api/v1/versions/item/$itemId/diff",
            mapOf("from" to fromVersion.toString(), "to" to toVersion.toString()),
        )

    suspend fun deleteVersion(versionId: String): ApiResult<Unit> =
        delete("/api/v1/versions/$versionId")

    suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int = 1,
    ): ApiResult<Map<String, String>> {
        val params = mutableMapOf<String, String>("minVersionsToKeep" to minVersionsToKeep.toString())
        maxVersions?.let { params["maxVersions"] = it.toString() }
        maxAgeDays?.let { params["maxAgeDays"] = it.toString() }
        return post("/api/v1/versions/item/$itemId/cleanup", params)
    }
}
