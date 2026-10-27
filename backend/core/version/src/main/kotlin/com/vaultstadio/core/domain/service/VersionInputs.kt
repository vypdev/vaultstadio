package com.vaultstadio.core.domain.service

/**
 * Input for creating a new file version.
 *
 * @property itemId Storage item ID
 * @property size File size in bytes
 * @property checksum File checksum
 * @property storageKey Storage backend key
 * @property comment Optional version comment
 */
data class CreateVersionInput(
    val itemId: String,
    val size: Long,
    val checksum: String,
    val storageKey: String,
    val comment: String? = null,
)

/**
 * Input for restoring a file version.
 *
 * @property itemId Storage item ID
 * @property versionNumber Version number to restore
 * @property comment Optional comment for the restore
 */
data class RestoreVersionInput(
    val itemId: String,
    val versionNumber: Int,
    val comment: String? = null,
)
