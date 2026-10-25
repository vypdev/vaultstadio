/**
 * Chunked upload init request DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChunkedUploadInitRequestDTO(
    val fileName: String,
    val totalSize: Long,
    val mimeType: String? = null,
    val parentId: String? = null,
    val chunkSize: Long = 10 * 1024 * 1024,
)
