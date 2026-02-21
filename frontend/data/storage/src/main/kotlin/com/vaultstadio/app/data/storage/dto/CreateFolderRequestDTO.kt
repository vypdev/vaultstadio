/**
 * Create folder request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFolderRequestDTO(
    val name: String,
    val parentId: String? = null,
)
