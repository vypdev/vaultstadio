/**
 * Request for syncing changes (pagination and options).
 */

package com.vaultstadio.app.domain.sync.model

data class SyncRequest(
    val cursor: String? = null,
    val limit: Int = 1000,
    val includeDeleted: Boolean = true,
)
