/**
 * Single sync change item.
 */

package com.vaultstadio.app.domain.sync.model

import kotlinx.datetime.Instant

data class SyncChange(
    val id: String,
    val itemId: String,
    val changeType: ChangeType,
    val timestamp: Instant,
    val cursor: Long,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
)
