/**
 * Sync change DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncChangeDTO(
    val id: String,
    val itemId: String,
    val changeType: String,
    @kotlinx.serialization.Contextual
    val timestamp: Instant,
    val cursor: Long,
    val oldPath: String? = null,
    val newPath: String? = null,
    val checksum: String? = null,
)
