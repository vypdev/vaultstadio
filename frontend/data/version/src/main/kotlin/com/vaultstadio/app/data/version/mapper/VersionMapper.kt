/**
 * Version Mappers
 */

package com.vaultstadio.app.data.version.mapper

import com.vaultstadio.app.data.version.dto.FileVersionDTO
import com.vaultstadio.app.data.version.dto.FileVersionHistoryDTO
import com.vaultstadio.app.data.version.dto.VersionDiffDTO
import com.vaultstadio.app.domain.version.model.FileVersion
import com.vaultstadio.app.domain.version.model.FileVersionHistory
import com.vaultstadio.app.domain.version.model.VersionDiff

fun FileVersionDTO.toDomain(): FileVersion = FileVersion(
    id = id,
    itemId = itemId,
    versionNumber = versionNumber,
    size = size,
    checksum = checksum,
    createdBy = createdBy,
    createdAt = createdAt,
    comment = comment,
    isLatest = isLatest,
    restoredFrom = restoredFrom,
)

fun FileVersionHistoryDTO.toDomain(): FileVersionHistory = FileVersionHistory(
    itemId = itemId,
    itemName = itemName,
    versions = versions.map { it.toDomain() },
    totalVersions = totalVersions,
    totalSize = totalSize,
)

fun VersionDiffDTO.toDomain(): VersionDiff = VersionDiff(
    fromVersion = fromVersion,
    toVersion = toVersion,
    sizeChange = sizeChange,
    additions = additions,
    deletions = deletions,
    isBinary = isBinary,
)
