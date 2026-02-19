/**
 * Version Mappers
 */

package com.vaultstadio.app.data.mapper

import com.vaultstadio.app.data.dto.version.FileVersionDTO
import com.vaultstadio.app.data.dto.version.FileVersionHistoryDTO
import com.vaultstadio.app.data.dto.version.VersionDiffDTO
import com.vaultstadio.app.domain.model.FileVersion
import com.vaultstadio.app.domain.model.FileVersionHistory
import com.vaultstadio.app.domain.model.VersionDiff

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
