/**
 * Version Service
 */

package com.vaultstadio.app.data.version.service

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.version.api.VersionApi
import com.vaultstadio.app.data.version.dto.RestoreVersionRequestDTO
import com.vaultstadio.app.data.version.mapper.toDomain
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff

class VersionService(private val versionApi: VersionApi) {

    suspend fun getVersionHistory(itemId: String): ApiResult<FileVersionHistory> =
        versionApi.getVersionHistory(itemId).map { it.toDomain() }

    suspend fun getVersion(itemId: String, versionNumber: Int): ApiResult<FileVersion> =
        versionApi.getVersion(itemId, versionNumber).map { it.toDomain() }

    suspend fun restoreVersion(
        itemId: String,
        versionNumber: Int,
        comment: String? = null,
    ): ApiResult<Map<String, String>> =
        versionApi.restoreVersion(itemId, RestoreVersionRequestDTO(versionNumber, comment))

    suspend fun compareVersions(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ): ApiResult<VersionDiff> =
        versionApi.compareVersions(itemId, fromVersion, toVersion).map { it.toDomain() }

    suspend fun deleteVersion(versionId: String): ApiResult<Unit> =
        versionApi.deleteVersion(versionId)

    suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): ApiResult<Map<String, String>> =
        versionApi.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
}
