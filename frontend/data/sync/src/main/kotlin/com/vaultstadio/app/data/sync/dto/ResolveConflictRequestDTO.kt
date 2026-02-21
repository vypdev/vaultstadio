/**
 * Resolve conflict request DTO.
 */

package com.vaultstadio.app.data.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResolveConflictRequestDTO(
    val resolution: String,
)
