/**
 * Response from pull changes.
 */

package com.vaultstadio.app.domain.sync.model

import kotlinx.datetime.Instant

data class SyncResponse(
    val changes: List<SyncChange>,
    val cursor: String,
    val hasMore: Boolean,
    val conflicts: List<SyncConflict>,
    val serverTime: Instant,
)
