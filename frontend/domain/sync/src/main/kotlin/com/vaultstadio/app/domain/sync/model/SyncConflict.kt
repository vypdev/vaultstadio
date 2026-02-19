/**
 * Sync conflict between local and remote changes.
 */

package com.vaultstadio.app.domain.sync.model

import kotlinx.datetime.Instant

data class SyncConflict(
    val id: String,
    val itemId: String,
    val conflictType: ConflictType,
    val localChange: SyncChange,
    val remoteChange: SyncChange,
    val createdAt: Instant,
    val isPending: Boolean,
)
