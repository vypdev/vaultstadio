/**
 * Rename request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class RenameRequestDTO(
    val name: String,
)
