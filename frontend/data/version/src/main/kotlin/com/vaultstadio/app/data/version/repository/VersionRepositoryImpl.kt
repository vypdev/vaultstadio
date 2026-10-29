/**
 * Version Repository implementation
 */

package com.vaultstadio.app.data.version.repository

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.apiV1Location
import com.vaultstadio.app.data.network.mapper.toResult
import com.vaultstadio.app.data.version.service.VersionService
import com.vaultstadio.app.domain.version.VersionRepository

class VersionRepositoryImpl(
    private val versionService: VersionService,
    private val config: ApiClientConfig,
) : VersionRepository {

    override suspend fun getVersionHistory(itemId: String) =
        versionService.getVersionHistory(itemId).toResult()

    override suspend fun getVersion(itemId: String, versionNumber: Int) =
        versionService.getVersion(itemId, versionNumber).toResult()

    override suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String?) =
        versionService.restoreVersion(itemId, versionNumber, comment).map { }.toResult()

    override suspend fun compareVersions(
        itemId: String,
        fromVersion: Int,
        toVersion: Int,
    ) = versionService.compareVersions(itemId, fromVersion, toVersion).toResult()

    override suspend fun deleteVersion(versionId: String) =
        versionService.deleteVersion(versionId).toResult()

    override suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int?,
        maxAgeDays: Int?,
        minVersionsToKeep: Int,
    ) = versionService.cleanupVersions(itemId, maxVersions, maxAgeDays, minVersionsToKeep)
        .map { }.toResult()

    override fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String =
        apiV1Location(
            config.baseUrl,
            listOf("versions", "item", itemId, "version", versionNumber.toString(), "download"),
        )
}
