/**
 * Version Repository
 */

package com.vaultstadio.app.data.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.service.VersionService
import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.FileVersionHistory
import com.vaultstadio.app.domain.model.VersionDiff
import org.koin.core.annotation.Single

/**
 * Repository interface for version operations.
 */
interface VersionRepository {
    suspend fun getVersionHistory(itemId: String): ApiResult<FileVersionHistory>
    suspend fun getVersion(itemId: String, versionNumber: Int): ApiResult<FileVersion>
    suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String? = null): ApiResult<Unit>
    suspend fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int): ApiResult<VersionDiff>
    suspend fun deleteVersion(versionId: String): ApiResult<Unit>
    suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): ApiResult<Unit>
    fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String
}

@Single(binds = [VersionRepository::class])
class VersionRepositoryImpl(
    private val versionService: VersionService,
    private val config: ApiClientConfig,
    private val tokenStorage: TokenStorage,
) : VersionRepository {

    override suspend fun getVersionHistory(itemId: String): ApiResult<FileVersionHistory> =
        versionService.getVersionHistory(itemId)

    override suspend fun getVersion(itemId: String, versionNumber: Int): ApiResult<FileVersion> =
        versionService.getVersion(itemId, versionNumber)

    override suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String?): ApiResult<Unit> =
        versionService.restoreVersion(itemId, versionNumber, comment).map { }

    override suspend fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int): ApiResult<VersionDiff> =
        versionService.compareVersions(itemId, fromVersion, toVersion)

    override suspend fun deleteVersion(versionId: String): ApiResult<Unit> =
        versionService.deleteVersion(versionId)

    override suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ): ApiResult<Unit> = versionService.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep).map { }

    override fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String {
        val token = tokenStorage.getAccessToken()
        val auth = if (token != null) "?token=$token" else ""
        return "${config.baseUrl}/api/v1/versions/item/$itemId/version/$versionNumber/download$auth"
    }
}
