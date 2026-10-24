/**
 * Version Data Transfer Objects
 */

package com.vaultstadio.app.data.version.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FileVersionDTO(
    val id: String,
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val createdBy: String,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
    val comment: String? = null,
    val isLatest: Boolean,
    val restoredFrom: Int? = null,
)

@Serializable
data class FileVersionHistoryDTO(
    val itemId: String,
    val itemName: String,
    val versions: List<FileVersionDTO>,
    val totalVersions: Int,
    val totalSize: Long,
)

@Serializable
data class RestoreVersionRequestDTO(
    val versionNumber: Int,
    val comment: String? = null,
)

@Serializable
data class VersionDiffDTO(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int,
    val deletions: Int,
    val isBinary: Boolean,
)

@Serializable
data class CleanupVersionsRequestDTO(
    val maxVersions: Int? = null,
    val maxAgeDays: Int? = null,
    val minVersionsToKeep: Int = 1,
)
