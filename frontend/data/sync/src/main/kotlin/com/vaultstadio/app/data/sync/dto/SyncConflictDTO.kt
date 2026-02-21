/**
 * Sync conflict DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SyncConflictDTO(
    val id: String,
    val itemId: String,
    val conflictType: String,
    val localChange: SyncChangeDTO,
    val remoteChange: SyncChangeDTO,
    @kotlinx.serialization.Contextual
    val createdAt: Instant,
    val isPending: Boolean,
)
