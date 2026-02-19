/**
 * Repository interface for version operations.
 */

package com.vaultstadio.app.domain.version

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff

interface VersionRepository {
    suspend fun getVersionHistory(itemId: String): Result<FileVersionHistory>
    suspend fun getVersion(itemId: String, versionNumber: Int): Result<FileVersion>
    suspend fun restoreVersion(itemId: String, versionNumber: Int, comment: String? = null): Result<Unit>
    suspend fun compareVersions(itemId: String, fromVersion: Int, toVersion: Int): Result<VersionDiff>
    suspend fun deleteVersion(versionId: String): Result<Unit>
    suspend fun cleanupVersions(
        itemId: String,
        maxVersions: Int? = null,
        maxAgeDays: Int? = null,
        minVersionsToKeep: Int = 1,
    ): Result<Unit>
    fun getVersionDownloadUrl(itemId: String, versionNumber: Int): String
}
