/**
 * Document State DTO
 */

package com.vaultstadio.app.data.collaboration.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DocumentStateDTO(
    val itemId: String,
    val version: Long,
    val content: String,
    @kotlinx.serialization.Contextual val lastModified: Instant,
)
