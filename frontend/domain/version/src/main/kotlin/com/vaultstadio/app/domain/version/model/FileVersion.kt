/**
 * File version domain model.
 */

package com.vaultstadio.app.domain.version.model

import kotlinx.datetime.Instant

data class FileVersion(
    val id: String,
    val itemId: String,
    val versionNumber: Int,
    val size: Long,
    val checksum: String,
    val createdBy: String,
    val createdAt: Instant,
    val comment: String? = null,
    val isLatest: Boolean,
    val restoredFrom: Int? = null,
) {
    val isRestore: Boolean get() = restoredFrom != null
}
