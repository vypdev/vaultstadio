/**
 * Sync pull response DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncResponseDTO(
    val changes: List<SyncChangeDTO>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflictDTO>,
    @kotlinx.serialization.Contextual
    val serverTime: Instant,
)
