/**
 * Chunked upload init response DTO.
 */

package com.vaultstadio.app.data.storage.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChunkedUploadInitDTO(
    val uploadId: String,
    val chunkSize: Long,
    val totalChunks: Int,
)
