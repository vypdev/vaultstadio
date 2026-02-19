/**
 * Version diff domain model.
 */

package com.vaultstadio.app.domain.version.model

data class VersionDiff(
    val fromVersion: Int,
    val toVersion: Int,
    val sizeChange: Long,
    val additions: Int,
    val deletions: Int,
    val isBinary: Boolean,
)
