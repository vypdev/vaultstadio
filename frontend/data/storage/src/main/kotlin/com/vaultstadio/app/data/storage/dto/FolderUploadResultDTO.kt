/**
 * Folder upload result DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class FolderUploadResultDTO(
    val uploadedFiles: Int,
    val createdFolders: Int,
    val errors: List<FolderUploadErrorDTO> = emptyList(),
)
