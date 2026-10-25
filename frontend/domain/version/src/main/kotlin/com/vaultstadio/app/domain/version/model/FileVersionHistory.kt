/**
 * File version history domain model.
 */

package com.vaultstadio.app.domain.version.model

data class FileVersionHistory(
    val itemId: String,
    val itemName: String,
    val versions: List<FileVersion>,
    val totalVersions: Int,
    val totalSize: Long,
)
