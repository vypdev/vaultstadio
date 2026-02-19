/**
 * Request to restore a file version.
 */

package com.vaultstadio.app.domain.version.model

data class RestoreVersionRequest(
    val versionNumber: Int,
    val comment: String? = null,
)
