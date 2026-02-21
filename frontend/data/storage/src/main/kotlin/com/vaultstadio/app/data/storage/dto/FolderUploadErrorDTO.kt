/**
 * Folder upload error DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class FolderUploadErrorDTO(
    val path: String,
    val error: String,
)
