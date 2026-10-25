/**
 * Move request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class MoveRequestDTO(
    val destinationId: String?,
    val newName: String? = null,
)
