/**
 * Document State
 */

package com.vaultstadio.app.domain.collaboration.model

import kotlinx.datetime.Instant

data class DocumentState(
    val itemId: String,
    val version: Long,
    val content: String,
    val lastModified: Instant,
)
